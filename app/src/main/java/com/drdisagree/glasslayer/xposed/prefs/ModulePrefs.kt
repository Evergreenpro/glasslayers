package com.drdisagree.glasslayer.xposed.prefs

import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.drdisagree.glasslayer.data.source.PrefKeys
import com.drdisagree.glasslayer.data.source.PrefsContract
import com.drdisagree.glasslayer.xposed.hook.Guard
import com.drdisagree.glasslayer.xposed.hook.HookEnv
import com.drdisagree.glasslayer.xposed.hook.HostApplication
import io.github.libxposed.api.XposedInterface
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

object ModulePrefs {

    private var prefs: SharedPreferences? = null
    private val listeners = CopyOnWriteArrayList<(String?) -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, WORKER_NAME).apply { isDaemon = true } }
    private val overrides = ConcurrentHashMap<String, Optional<Any>>()
    private val dispatched = HashMap<String, Optional<Any>>()
    private val observer = PrefsObserver(::onProviderChanged)
    private val receiver = PrefsReceiver { intent ->
        val snapshot = intent.extras
        worker.execute { Guard.run(BROADCAST_TAG, Unit) { applySnapshot(snapshot) } }
    }
    private var attachAttempts = 0

    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        worker.execute { Guard.run(REMOTE_TAG, Unit) { onRemoteChange(key) } }
    }

    fun init(module: XposedInterface) {
        prefs = try {
            module.getRemotePreferences(PrefKeys.GROUP)
        } catch (throwable: Throwable) {
            HookEnv.log("Remote preferences unavailable", throwable)
            null
        }
        prefs?.registerOnSharedPreferenceChangeListener(changeListener)
        attachObserver()
    }

    fun addListener(listener: (String?) -> Unit) {
        listeners += listener
    }

    fun boolean(key: String, default: Boolean): Boolean = read(key, default) { it.getBoolean(key, default) }

    fun long(key: String, default: Long): Long = read(key, default) { it.getLong(key, default) }

    fun float(key: String, default: Float): Float = read(key, default) { it.getFloat(key, default) }

    private inline fun <reified T> read(key: String, default: T, remote: (SharedPreferences) -> T): T {
        overrides[key]?.let { return it.orElse(null) as? T ?: default }
        return try {
            prefs?.let(remote) ?: default
        } catch (_: Throwable) {
            default
        }
    }

    private fun attachObserver() {
        val context = HostApplication.get()
        if (context == null) {
            if (attachAttempts++ < ATTACH_ATTEMPTS) {
                mainHandler.postDelayed({ Guard.run(ATTACH_TAG, Unit) { attachObserver() } }, ATTACH_DELAY_MS)
            }
            return
        }
        try {
            context.contentResolver.registerContentObserver(PrefsContract.CHANGED_URI, true, observer)
        } catch (_: SecurityException) {
            listenForBroadcasts(context)
        } catch (throwable: Throwable) {
            HookEnv.log("Preference change channel unavailable", throwable)
        }
    }

    private fun listenForBroadcasts(context: Context) {
        try {
            context.registerReceiver(receiver, IntentFilter(PrefsContract.ACTION_CHANGED), PrefsContract.PERMISSION, null, Context.RECEIVER_EXPORTED)
            HookEnv.log("Preference changes arrive by broadcast here")
        } catch (throwable: Throwable) {
            HookEnv.log("Preference change channel unavailable", throwable)
        }
    }

    private fun onProviderChanged(uri: Uri?) {
        val key = PrefsContract.keyOf(uri)
        worker.execute { Guard.run(PROVIDER_TAG, Unit) { readProvider(key) } }
    }

    private fun readProvider(key: String?) {
        val context = HostApplication.get() ?: return
        applySnapshot(context.contentResolver.call(PrefsContract.BASE_URI, PrefsContract.METHOD_READ, key, null))
    }

    private fun applySnapshot(result: Bundle?) {
        result ?: return
        val values = result.getBundle(PrefsContract.EXTRA_VALUES) ?: Bundle.EMPTY
        val changed = ArrayList<String>()
        values.keySet().forEach { name -> if (applyOverride(name, Optional.ofNullable(values.valueOf(name)))) changed += name }
        result.getStringArrayList(PrefsContract.EXTRA_REMOVED)?.forEach { name ->
            if (applyOverride(name, Optional.empty())) changed += name
        }
        when (changed.size) {
            0 -> Unit
            1 -> dispatch(changed[0])
            else -> dispatch(null)
        }
    }

    private fun applyOverride(key: String, value: Optional<Any>): Boolean {
        val before = effective(key)
        overrides[key] = value
        if (value == before) return false
        dispatched[key] = value
        return true
    }

    private fun onRemoteChange(key: String?) {
        if (key == null) {
            overrides.clear()
            dispatched.clear()
            dispatch(null)
            return
        }
        overrides.remove(key)
        val value = Optional.ofNullable(remoteValue(key))
        if (dispatched[key] == value) return
        dispatched[key] = value
        dispatch(key)
    }

    private fun effective(key: String): Optional<Any> = overrides[key] ?: Optional.ofNullable(remoteValue(key))

    private fun remoteValue(key: String): Any? = try {
        prefs?.all?.get(key)
    } catch (_: Throwable) {
        null
    }

    private fun dispatch(key: String?) {
        mainHandler.post { listeners.forEach { listener -> Guard.run(TAG, Unit) { listener(key) } } }
    }

    @Suppress("DEPRECATION")
    private fun Bundle.valueOf(key: String): Any? = get(key)
}

private const val TAG = "ModulePrefs.listener"
private const val REMOTE_TAG = "ModulePrefs.remote"
private const val PROVIDER_TAG = "ModulePrefs.provider"
private const val ATTACH_TAG = "ModulePrefs.attach"
private const val BROADCAST_TAG = "ModulePrefs.broadcast"
private const val WORKER_NAME = "GlassLayerPrefs"
private const val ATTACH_ATTEMPTS = 40
private const val ATTACH_DELAY_MS = 250L
