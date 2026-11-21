package com.drdisagree.glasslayer.xposed.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.view.Choreographer
import android.view.SurfaceControl
import android.view.View
import com.drdisagree.glasslayer.xposed.glass.BackdropCapture
import com.drdisagree.glasslayer.xposed.glass.BackdropClient
import com.drdisagree.glasslayer.xposed.glass.BackdropFrame
import com.drdisagree.glasslayer.xposed.glass.DEFAULT_REFRESH_RATE
import com.drdisagree.glasslayer.xposed.glass.WaterRippleShader
import com.drdisagree.glasslayer.xposed.glass.WaterSurface
import com.drdisagree.glasslayer.xposed.hook.Guard
import com.drdisagree.glasslayer.xposed.hook.call
import kotlin.math.max

@SuppressLint("ViewConstructor")
internal class UnlockRippleView(
    context: Context,
    private val origin: PointF,
    private val durationScale: Float,
    private val onFinished: (UnlockRippleView) -> Unit
) : View(context), BackdropClient, Choreographer.FrameCallback {

    private val shader = RuntimeShader(WaterRippleShader.SOURCE)
    private val paint = Paint().apply { shader = this@UnlockRippleView.shader }
    private val empty = BitmapShader(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    private val surface = WaterSurface()
    private val backdropMatrix = Matrix()
    private val fieldMatrix = Matrix()
    private val screen = IntArray(2)
    private var frame: BackdropFrame? = null
    private var frameBitmap: Bitmap? = null
    private var fieldReady = false
    private var startNanos = 0L
    private var lastNanos = 0L
    private var elapsedSeconds = 0f
    private var finished = false

    override val displayId: Int
        get() = display?.displayId ?: 0

    override val wantsFullDisplay: Boolean
        get() = true

    override val wantsDisplayCapture: Boolean
        get() = true

    override val preferredScale: Float
        get() = CAPTURE_SCALE

    override val refreshRate: Float
        get() = display?.refreshRate ?: DEFAULT_REFRESH_RATE

    init {
        setWillNotDraw(false)
        shader.setInputShader(WaterRippleShader.BACKDROP, empty)
        shader.setInputShader(WaterRippleShader.FIELD, empty)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        BackdropCapture.register(this)
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(this)
        BackdropCapture.unregister(this)
        frame = null
        frameBitmap = null
        surface.release()
        super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        Guard.run(TAG_FRAME, Unit) {
            if (finished) return@run
            if (!fieldReady && width > 0 && height > 0) prepareField()
            if (startNanos == 0L) {
                startNanos = frameTimeNanos
                lastNanos = frameTimeNanos
            }
            val duration = max(GlassConfig.unlockRippleDurationMs * durationScale, 1f)
            elapsedSeconds = (frameTimeNanos - startNanos) / NANOS
            if (elapsedSeconds * MILLIS >= duration || !GlassConfig.unlockRippleActive) {
                finish()
                return@run
            }
            if (fieldReady) {
                val rate = REFERENCE_MS / duration * GlassConfig.unlockRippleSpeed
                surface.advance((frameTimeNanos - lastNanos) / NANOS, rate)
                surface.upload()
            }
            lastNanos = frameTimeNanos
            BackdropCapture.captureNow()
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun finish() {
        if (finished) return
        finished = true
        Choreographer.getInstance().removeFrameCallback(this)
        BackdropCapture.unregister(this)
        onFinished(this)
    }

    private fun prepareField() {
        getLocationOnScreen(screen)
        surface.reset(width, height)
        surface.impulse(origin.x - screen[0], origin.y - screen[1])
        surface.upload()
        val bitmap = surface.bitmap ?: return
        surface.fieldMatrix(fieldMatrix)
        val fieldShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
            setLocalMatrix(fieldMatrix)
        }
        shader.setInputShader(WaterRippleShader.FIELD, fieldShader)
        fieldReady = true
    }

    override fun onDraw(canvas: Canvas) {
        Guard.run(TAG_DRAW, Unit) {
            if (!fieldReady || width <= 0 || height <= 0) return@run
            bindBackdrop()
            if (frameBitmap == null) return@run
            val duration = max(GlassConfig.unlockRippleDurationMs * durationScale, 1f) / MILLIS
            shader.setFloatUniform(WaterRippleShader.PROGRESS, (elapsedSeconds / duration).coerceIn(0f, 1f))
            shader.setFloatUniform(WaterRippleShader.REFRACTION, REFRACTION_DP * resources.displayMetrics.density * GlassConfig.unlockRippleStrength)
            shader.setFloatUniform(WaterRippleShader.HAS_BACKDROP, if (frameBitmap != null) 1f else 0f)
            shader.setFloatUniform(WaterRippleShader.OVERLAY, 1f)
            shader.setFloatUniform(WaterRippleShader.RESOLUTION, width.toFloat(), height.toFloat())
            canvas.drawPaint(paint)
        }
    }

    private fun bindBackdrop() {
        val current = frame ?: return
        if (current.bitmap === frameBitmap) return
        val bitmap = current.bitmap
        val bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }
        backdropMatrix.setScale(1f / current.scale, 1f / current.scale)
        backdropMatrix.postTranslate((current.screenRect.left - screen[0]).toFloat(), (current.screenRect.top - screen[1]).toFloat())
        bitmapShader.setLocalMatrix(backdropMatrix)
        shader.setInputShader(WaterRippleShader.BACKDROP, bitmapShader)
        frameBitmap = bitmap
    }

    override fun captureRect(out: Rect): Boolean {
        if (finished || !isAttachedToWindow) return false
        out.set(0, 0, rootView.width, rootView.height)
        return !out.isEmpty
    }

    override fun excludeLayers(out: MutableCollection<SurfaceControl>) {
        val control = call(VIEW_ROOT)?.call(SURFACE_CONTROL) as? SurfaceControl ?: return
        if (control.isValid) out.add(control)
    }

    override fun onBackdrop(frame: BackdropFrame) {
        if (finished) return
        this.frame = frame
        invalidate()
    }
}

private const val TAG_FRAME = "UnlockRipple.frame"
private const val TAG_DRAW = "UnlockRipple.draw"
private const val VIEW_ROOT = "getViewRootImpl"
private const val SURFACE_CONTROL = "getSurfaceControl"
private const val NANOS = 1_000_000_000f
private const val MILLIS = 1000f
private const val REFERENCE_MS = 800f
private const val REFRACTION_DP = 80f
private const val CAPTURE_SCALE = 0.4f
