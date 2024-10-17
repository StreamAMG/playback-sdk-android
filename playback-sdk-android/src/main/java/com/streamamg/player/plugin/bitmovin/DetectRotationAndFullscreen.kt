package com.streamamg.player.plugin.bitmovin

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.bitmovin.player.PlayerView
import kotlin.math.absoluteValue

@Composable
fun DetectRotationAndFullscreen(playerView: PlayerView?, callback: (isFullscreen: Boolean) -> Unit) {
    val context = LocalContext.current
    var isLandscape by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = event.values[0]
                    val y = event.values[1]

                    // Determine if the device is in landscape or portrait orientation
                    if (x.absoluteValue > y.absoluteValue) {
                        // Landscape mode
                        if (!isLandscape) {
                            isLandscape = true
                            Log.d("Orientation", "Landscape mode detected")
//                            playerView?.enterFullscreen()
                            callback.invoke(true)
                        }
                    } else {
                        // Portrait mode
                        if (isLandscape) {
                            isLandscape = false
                            Log.d("Orientation", "Portrait mode detected")
//                            playerView?.exitFullscreen()
                            callback.invoke(false)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register the sensor listener
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            // Unregister the sensor listener to avoid memory leaks
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
}
