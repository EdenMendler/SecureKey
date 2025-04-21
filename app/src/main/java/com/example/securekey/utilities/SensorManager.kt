package com.example.securekey.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.util.Log
import java.util.Calendar

class SensorManager(
    private val context: Context,
    private val onLockStatusChanged: (Int, Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager: AndroidSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private var shakeCount = 0
    private val TAG = "SensorManager"

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun hasAccelerometer(): Boolean {
        return accelerometer != null
    }

    fun startSensing() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopSensing() {
        if (accelerometer != null) {
            sensorManager.unregisterListener(this)
        }
    }

    fun getBatteryLevel(activity: Activity): Float {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = activity.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return level * 100 / scale.toFloat()
    }

    fun isWifiConnected(context: Context): Boolean {
        try {
            val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            return (wifi != null && wifi.isConnected)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking wifi: ${e.message}")
            return true
        }
    }

    fun isTimeInRange(startHour: Int, endHour: Int): Boolean {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        return hourOfDay in startHour until endHour
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / AndroidSensorManager.GRAVITY_EARTH
            val gY = y / AndroidSensorManager.GRAVITY_EARTH
            val gZ = z / AndroidSensorManager.GRAVITY_EARTH

            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            if (gForce > 2.5) {
                val now = System.currentTimeMillis()

                if (now - lastShakeTime > 500) {
                    lastShakeTime = now
                    shakeCount++
                    Log.d(TAG, "Shake detected! Count: $shakeCount")

                    if (shakeCount >= 3) {
                        Log.d(TAG, "3 shakes detected! Unlocking...")
                        onLockStatusChanged(2, true)
                        shakeCount = 0
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }
}