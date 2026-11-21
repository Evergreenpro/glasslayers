package com.evergreenpro.glasslayers.xposed.glass

object BackdropZoom {

    @Volatile
    var scale: Float = 1f

    @Volatile
    var centerX: Float = 0f

    @Volatile
    var centerY: Float = 0f
}
