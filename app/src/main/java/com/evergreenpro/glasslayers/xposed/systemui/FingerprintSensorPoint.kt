package com.evergreenpro.glasslayers.xposed.systemui

import android.content.Context
import android.graphics.PointF
import android.view.Surface
import android.view.WindowManager
import com.evergreenpro.glasslayers.xposed.hook.call
import com.evergreenpro.glasslayers.xposed.hook.field

internal object FingerprintSensorPoint {

    private const val FINGERPRINT_SERVICE = "fingerprint"
    private const val PROPERTIES = "getSensorPropertiesInternal"
    private const val IS_UDFPS = "isAnyUdfpsType"
    private const val IS_SIDE = "isAnySidefpsType"
    private const val LOCATION = "getLocation"
    private const val LOCATION_X = "sensorLocationX"
    private const val LOCATION_Y = "sensorLocationY"
    private const val DIMEN = "dimen"
    private const val SENSOR_X = "physical_fingerprint_sensor_center_screen_location_x"
    private const val SENSOR_Y = "physical_fingerprint_sensor_center_screen_location_y"
    private const val POWER_BUTTON_Y = "physical_power_button_center_screen_location_y"

    fun read(context: Context): PointF? {
        val display = context.display ?: return null
        if (display.rotation != Surface.ROTATION_0) return null
        val manager = context.getSystemService(FINGERPRINT_SERVICE) ?: return null
        val sensors = manager.call(PROPERTIES) as? List<*> ?: return null
        val sensor = sensors.firstOrNull { it.call(IS_UDFPS) == true || it.call(IS_SIDE) == true } ?: return null
        val bounds = context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds ?: return null
        val natural = display.supportedModes.maxOfOrNull { minOf(it.physicalWidth, it.physicalHeight) } ?: return null
        if (natural <= 0) return null
        val scale = minOf(bounds.width(), bounds.height()).toFloat() / natural
        val reported = reported(sensor)
        val point = when {
            reported != null -> PointF(reported.x * scale, reported.y * scale)
            sensor.call(IS_SIDE) == true -> sideSensor(context, bounds.width().toFloat())
            else -> null
        } ?: return null
        return PointF(point.x.coerceIn(0f, bounds.width().toFloat()), point.y.coerceIn(0f, bounds.height().toFloat()))
    }

    private fun reported(sensor: Any): PointF? {
        val location = sensor.call(LOCATION) ?: return null
        val x = (location.field(LOCATION_X) as? Int)?.toFloat() ?: return null
        val y = (location.field(LOCATION_Y) as? Int)?.toFloat() ?: return null
        if (x <= 0f && y <= 0f) return null
        return PointF(x, y)
    }

    private fun sideSensor(context: Context, width: Float): PointF? {
        val y = dimension(context, SENSOR_Y) ?: dimension(context, POWER_BUTTON_Y) ?: return null
        val x = dimension(context, SENSOR_X) ?: width
        return PointF(x, y)
    }

    private fun dimension(context: Context, name: String): Float? {
        val resources = context.resources
        val id = resources.getIdentifier(name, DIMEN, context.packageName)
        if (id == 0) return null
        return try {
            resources.getDimension(id).takeIf { it > 0f }
        } catch (_: Exception) {
            null
        }
    }
}
