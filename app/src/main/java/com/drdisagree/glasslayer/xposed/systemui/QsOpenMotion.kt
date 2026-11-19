package com.drdisagree.glasslayer.xposed.systemui

import android.content.res.Resources
import android.graphics.Matrix
import android.os.SystemClock
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.drdisagree.glasslayer.xposed.compose.ComposeModifiers
import com.drdisagree.glasslayer.xposed.compose.ComposeToolkit
import com.drdisagree.glasslayer.xposed.glass.ComposeLiquidGlass
import com.drdisagree.glasslayer.xposed.hook.Guard
import com.drdisagree.glasslayer.xposed.hook.HookEnv
import com.drdisagree.glasslayer.xposed.hook.call
import com.drdisagree.glasslayer.xposed.hook.field
import com.drdisagree.glasslayer.xposed.hook.hookAll
import com.drdisagree.glasslayer.xposed.hook.hookAllAfter
import com.drdisagree.glasslayer.xposed.hook.hookAllBefore
import com.drdisagree.glasslayer.xposed.hook.proceedWithArgs
import java.lang.ref.WeakReference
import java.lang.reflect.Proxy
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

object QsOpenMotion {

    private const val EXPANSION_MANAGER = "${SystemUiModule.PACKAGE}.shade.ShadeExpansionStateManager"
    private const val EXPANSION_LISTENER = "${SystemUiModule.PACKAGE}.shade.ShadeExpansionListener"
    private const val BAR_STATE_CONTROLLER = "${SystemUiModule.PACKAGE}.statusbar.StatusBarStateControllerImpl"
    private const val STACK_LAYOUT = "${SystemUiModule.PACKAGE}.statusbar.notification.stack.NotificationStackScrollLayout"
    private val touchViews = listOf(
        "${SystemUiModule.PACKAGE}.shade.NotificationPanelView",
        "${SystemUiModule.PACKAGE}.scene.ui.view.SceneWindowRootView",
        "${SystemUiModule.PACKAGE}.statusbar.phone.PhoneStatusBarView"
    )
    private const val ADD_LISTENER = "addExpansionListener"
    private const val ON_EXPANSION = "onPanelExpansionChanged"
    private val touchSources = listOf(
        "${SystemUiModule.PACKAGE}.shade.NotificationPanelViewController",
        "${SystemUiModule.PACKAGE}.shade.ShadeSurfaceImpl"
    )
    private const val TRANSITION_LAYOUT = "${SystemUiModule.PACKAGE}.util.animation.TransitionLayout"
    private const val GRAPHICS_LAYER_KT = "androidx.compose.ui.graphics.GraphicsLayerModifierKt"
    private const val INT_STATE_CLASS = "androidx.compose.runtime.ParcelableSnapshotMutableIntState"
    private const val FUNCTION1_CLASS = "kotlin.jvm.functions.Function1"
    private const val COMPANION_FIELD = "Companion"
    private const val SHADE = 0

    private val blocks = listOf(
        "${SystemUiModule.PACKAGE}.qs.panels.ui.compose.infinitegrid.TileKt" to "TileExpandable",
        "${SystemUiModule.PACKAGE}.brightness.ui.compose.BrightnessSliderKt" to "BrightnessSliderContainer",
        "${SystemUiModule.PACKAGE}.media.remedia.ui.compose.MediaKt" to "Media",
        "${SystemUiModule.PACKAGE}.qs.footer.ui.compose.FooterActionsKt" to "FooterActions"
    )

    private val companion: Any? by lazy {
        try {
            HookEnv.findClass(ComposeToolkit.MODIFIER_CLASS)?.getField(COMPANION_FIELD)?.get(null)
        } catch (_: Throwable) {
            null
        }
    }

    private val graphicsLayer: Method? by lazy {
        HookEnv.findClass(GRAPHICS_LAYER_KT)?.methods?.firstOrNull {
            it.name == "graphicsLayer" && it.parameterTypes.size == 2 && it.parameterTypes[1].name == FUNCTION1_CLASS
        }
    }

    private val thenMethod: Method? by lazy {
        try {
            HookEnv.findClass(ComposeToolkit.MODIFIER_CLASS)?.let { it.getMethod("then", it) }
        } catch (_: Throwable) {
            null
        }
    }

    private val frameState: Any? by lazy {
        try {
            HookEnv.findClass(INT_STATE_CLASS)?.getConstructor(Int::class.javaPrimitiveType)?.newInstance(0)
        } catch (_: Throwable) {
            null
        }
    }

    private val sampleTimes = LongArray(SAMPLES)
    private val sampleValues = FloatArray(SAMPLES)
    private var sampleHead = 0
    private var sampleCount = 0

    private var value = 1f
    private var velocity = 0f
    private var target = 1f
    private var touchX = 0f
    private var running = false
    private var lastFrameNanos = 0L
    private var settledAt = 0L
    private var frameCount = 0

    private var stack: WeakReference<ViewGroup>? = null
    private var barState: WeakReference<Any>? = null
    private var listenerAdded = false
    private val stackMatrices = WeakHashMap<View, Matrix>()
    private val movedViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
    private val mediaViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
    private val location = IntArray(2)

    private val frameCallback = Choreographer.FrameCallback { nanos -> Guard.run(FRAME_TAG, Unit) { onFrame(nanos) } }

    fun install(): Int {
        val listenerClass = HookEnv.findClass(EXPANSION_LISTENER) ?: return 0
        var hooks = HookEnv.findClass(EXPANSION_MANAGER).hookAllAfter(ADD_LISTENER) { chain, _ ->
            addListener(chain.thisObject, listenerClass)
        }
        if (hooks == 0) return 0
        touchSources.forEach { className ->
            hooks += HookEnv.findClass(className).hookAllBefore("handleExternalTouch") { chain -> recordTouch(chain.args.firstOrNull()) }
        }
        touchViews.forEach { className ->
            hooks += HookEnv.findClass(className).hookAllBefore("dispatchTouchEvent") { chain -> recordTouch(chain.args.firstOrNull()) }
        }
        hooks += HookEnv.findClass(BAR_STATE_CONTROLLER).hookAllAfter("addCallback") { chain, _ ->
            if (barState?.get() == null) chain.thisObject?.let { barState = WeakReference(it) }
        }
        hooks += HookEnv.findClass(STACK_LAYOUT).hookAllBefore("onLayout") { chain ->
            val layout = chain.thisObject as? ViewGroup ?: return@hookAllBefore
            if (stack?.get() !== layout) stack = WeakReference(layout)
        }
        hooks += HookEnv.findClass(TRANSITION_LAYOUT).hookAllBefore("dispatchDraw") { chain ->
            (chain.thisObject as? View)?.let { mediaViews += it }
        }
        blocks.forEach { (className, name) ->
            hooks += HookEnv.findClass(className).hookAll(name) { chain ->
                chain.proceedWithArgs { args -> wrap(chain.executable as Method, args) }
            }
        }
        return hooks
    }

    fun refresh() {
        if (!GlassConfig.qsMotionActive) snapToRest()
        bump()
    }

    private fun addListener(manager: Any?, listenerClass: Class<*>) {
        if (listenerAdded || manager == null) return
        listenerAdded = true
        val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { proxy, method, args ->
            when (method.name) {
                ON_EXPANSION -> Guard.run(EXPANSION_TAG, Unit) { onExpansion(args?.firstOrNull()) }
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> LISTENER_NAME
                else -> null
            }
        }
        manager.call(ADD_LISTENER, listener)
    }

    private fun recordTouch(event: Any?) {
        val motion = event as? MotionEvent ?: return
        if (motion.actionMasked == MotionEvent.ACTION_DOWN) touchX = motion.rawX
    }

    private fun onExpansion(event: Any?) {
        val fraction = event.field("fraction") as? Float ?: return
        val state = barState?.get()?.call("getState") as? Int ?: SHADE
        if (!GlassConfig.qsMotionActive || state != SHADE) {
            if (value != 1f || running) snapToRest()
            return
        }
        target = fraction.coerceIn(0f, 1f)
        if (!running && abs(value - target) > EPSILON) start()
    }

    private fun start() {
        running = true
        settledAt = 0L
        lastFrameNanos = 0L
        record(SystemClock.uptimeMillis(), value)
        applyStack()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun snapToRest() {
        running = false
        value = 1f
        target = 1f
        velocity = 0f
        sampleCount = 0
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        resetStack()
        bump()
    }

    private fun onFrame(nanos: Long) {
        if (!running) return
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrameNanos == 0L) DEFAULT_FRAME_S else ((nanos - lastFrameNanos) / NANOS_PER_SECOND).coerceIn(0f, MAX_FRAME_S)
        lastFrameNanos = nanos
        step(dt)
        record(now, value)
        if (abs(value - target) < EPSILON && abs(velocity) < VELOCITY_EPSILON) {
            value = target
            velocity = 0f
            if (settledAt == 0L) settledAt = now
        } else {
            settledAt = 0L
        }
        if (settledAt != 0L && now - settledAt > MAX_DELAY_MS + SETTLE_MS) {
            running = false
            resetStack()
            bump()
            return
        }
        applyStack()
        bump()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun step(dt: Float) {
        var remaining = dt
        while (remaining > 0f) {
            val h = min(remaining, SUBSTEP_S)
            val acceleration = -STIFFNESS * (value - target) - DAMPING * velocity
            velocity += acceleration * h
            value += velocity * h
            remaining -= h
        }
    }

    private fun record(time: Long, sample: Float) {
        sampleHead = (sampleHead + 1) % SAMPLES
        sampleTimes[sampleHead] = time
        sampleValues[sampleHead] = sample
        if (sampleCount < SAMPLES) sampleCount++
    }

    private fun valueAt(time: Long): Float {
        if (sampleCount == 0) return value
        var index = sampleHead
        repeat(sampleCount) {
            if (sampleTimes[index] <= time) return sampleValues[index]
            index = (index - 1 + SAMPLES) % SAMPLES
        }
        return sampleValues[(sampleHead - sampleCount + 1 + SAMPLES) % SAMPLES]
    }

    private fun progressAt(centerX: Float, centerY: Float): Float {
        if (!running) return value
        val density = Resources.getSystem().displayMetrics.density
        val delay = (hypot(centerX - touchX, centerY - ANCHOR_Y) / density * DELAY_PER_DP).toLong().coerceAtMost(MAX_DELAY_MS)
        return valueAt(SystemClock.uptimeMillis() - delay).coerceIn(0f, MAX_PROGRESS)
    }

    private fun offsetX(centerX: Float, centerY: Float, rest: Float): Float {
        val halfWidth = Resources.getSystem().displayMetrics.widthPixels / 2f
        val side = ((touchX - halfWidth) / halfWidth).coerceIn(-1f, 1f)
        return ((touchX - centerX) * PULL_X + side * abs(centerY - ANCHOR_Y) * SWEEP) * rest
    }

    private fun offsetY(centerY: Float, rest: Float): Float = (ANCHOR_Y - centerY) * PULL_Y * rest

    private fun scaleOf(progress: Float): Float = 1f - (1f - START_SCALE) * (1f - progress)

    private fun alphaOf(progress: Float): Float = ((progress - FADE_START) / (1f - FADE_START)).coerceIn(0f, 1f)

    private fun bump() {
        frameState?.call("setIntValue", ++frameCount)
    }

    private fun applyStack() {
        stack?.get()?.let { group ->
            for (index in 0 until group.childCount) moveView(group.getChildAt(index))
        }
        mediaViews.forEach { if (it.isAttachedToWindow && it.isShown) moveView(it) }
    }

    private fun moveView(view: View) {
        if (view.width <= 0 || view.height <= 0) return
        view.getLocationOnScreen(location)
        val centerX = location[0] + view.width / 2f
        val centerY = location[1] + view.height / 2f
        val progress = progressAt(centerX, centerY)
        val rest = 1f - progress
        val scale = scaleOf(progress)
        val matrix = stackMatrices.getOrPut(view) { Matrix() }
        matrix.setScale(scale, scale, view.width / 2f, view.height / 2f)
        matrix.postTranslate(offsetX(centerX, centerY, rest), offsetY(centerY, rest))
        view.animationMatrix = matrix
        view.transitionAlpha = alphaOf(progress)
        movedViews += view
    }

    private fun resetStack() {
        movedViews.forEach { view ->
            view.animationMatrix = null
            view.transitionAlpha = 1f
        }
        movedViews.clear()
    }

    private fun wrap(method: Method, args: Array<Any?>) {
        if (!GlassConfig.qsMotionActive) return
        val index = method.parameterTypes.indexOfFirst { it.name == ComposeToolkit.MODIFIER_CLASS }
        if (index == -1) return
        val stock = args[index] ?: return
        val base = companion ?: return
        val block = QsMotionBlock()
        val probe = ComposeModifiers.contentProxy(PROBE_TAG) { drawScope -> probe(drawScope, block) } ?: return
        val layer = ComposeToolkit.function1(LAYER_TAG) { scope -> scope?.let { applyLayer(it, block) }; Unit } ?: return
        val probed = ComposeModifiers.drawWithContent(base, probe) ?: return
        val layered = graphicsLayer?.invoke(null, probed, layer) ?: return
        args[index] = thenMethod?.invoke(layered, stock) ?: return
    }

    private fun probe(drawScope: Any, block: QsMotionBlock) {
        if (running || block.centerX.isNaN()) {
            val position = ComposeLiquidGlass.screenPosition(drawScope)
            val size = ComposeToolkit.drawSize(drawScope)
            if (position != null && size != null) {
                block.centerX = unpackX(position) + unpackX(size) / 2f
                block.centerY = unpackY(position) + unpackY(size) / 2f
            }
        }
        drawScope.call("drawContent")
    }

    private fun applyLayer(scope: Any, block: QsMotionBlock) {
        frameState?.call("getIntValue")
        val known = !block.centerX.isNaN()
        val progress = when {
            !GlassConfig.qsMotionActive -> 1f
            known -> progressAt(block.centerX, block.centerY)
            else -> value
        }
        val rest = 1f - progress
        val scale = scaleOf(progress)
        scope.call("setScaleX", scale)
        scope.call("setScaleY", scale)
        scope.call("setAlpha", alphaOf(progress))
        if (!known) return
        scope.call("setTranslationX", offsetX(block.centerX, block.centerY, rest))
        scope.call("setTranslationY", offsetY(block.centerY, rest))
    }

    private fun unpackX(packed: Long): Float = Float.fromBits((packed ushr 32).toInt())

    private fun unpackY(packed: Long): Float = Float.fromBits((packed and 0xFFFFFFFFL).toInt())
}

private const val FRAME_TAG = "QsOpenMotion.frame"
private const val EXPANSION_TAG = "QsOpenMotion.expansion"
private const val LISTENER_NAME = "GlassLayerOpenMotion"
private const val PROBE_TAG = "QsOpenMotion.probe"
private const val LAYER_TAG = "QsOpenMotion.layer"
private const val SAMPLES = 128
private const val STIFFNESS = 600f
private val DAMPING = 2f * 0.7f * sqrt(STIFFNESS)
private const val SUBSTEP_S = 0.004f
private const val DEFAULT_FRAME_S = 1f / 120f
private const val MAX_FRAME_S = 0.05f
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val EPSILON = 0.001f
private const val VELOCITY_EPSILON = 0.01f
private const val DELAY_PER_DP = 0.4f
private const val MAX_DELAY_MS = 220L
private const val SETTLE_MS = 40L
private const val START_SCALE = 0.7f
private const val FADE_START = 0.15f
private const val PULL_X = 0.6f
private const val PULL_Y = 0.12f
private const val SWEEP = 0.25f
private const val ANCHOR_Y = 0f
private const val MAX_PROGRESS = 1.15f
