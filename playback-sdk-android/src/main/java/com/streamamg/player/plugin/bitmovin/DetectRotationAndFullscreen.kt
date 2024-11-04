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
import kotlin.math.abs
import kotlin.math.absoluteValue

@Composable
fun DetectRotationAndFullscreen(playerView: PlayerView?, callback: (isFullscreen: Boolean) -> Unit) {
    val context = LocalContext.current
    var isLandscape by remember { mutableStateOf(false) }
    var lastOrientation by remember { mutableStateOf(false) }
    val threshold = 5.0f

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = event.values[0]
                    val y = event.values[1]

                    val currentOrientation = x.absoluteValue > y.absoluteValue
                    if (abs(x) > threshold || abs(y) > threshold) {
                        if (currentOrientation != lastOrientation) {
                            if (currentOrientation && !isLandscape) {
                                isLandscape = true
                                Log.d("Orientation", "Landscape mode detected")
                                callback.invoke(true)
                            } else if (!currentOrientation && isLandscape) {
                                isLandscape = false
                                Log.d("Orientation", "Portrait mode detected")
                                callback.invoke(false)
                            }
                        }
                    }
                    lastOrientation = currentOrientation
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
}
