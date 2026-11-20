package com.nurvakti.global.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class Heading(val trueDeg: Float, val accuracy: Int, val declination: Float, val pitchOk: Boolean)

/** Telefonun gerçek sensörlerinden (döndürme vektörü; yoksa ivmeölçer + manyetometre) gerçek kuzeye göre yön. */
object Compass {
    fun available(ctx: Context): Boolean {
        val sm = ctx.getSystemService(SensorManager::class.java)
        return sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null &&
            (sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null || sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
    }

    fun headings(ctx: Context, lat: Double, lng: Double): Flow<Heading> = callbackFlow {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val decl = GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis()).declination
        val rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        val g = FloatArray(3); val m = FloatArray(3); var hasG = false; var hasM = false
        val rot = FloatArray(9); val ori = FloatArray(3)
        var smooth = Float.NaN
        fun emit() {
            SensorManager.getOrientation(rot, ori)
            var az = Math.toDegrees(ori[0].toDouble()).toFloat()
            az = (az + decl + 360f) % 360f
            smooth = if (smooth.isNaN()) az else {
                var d = az - smooth; if (d > 180) d -= 360; if (d < -180) d += 360
                (smooth + d * 0.15f + 360f) % 360f
            }
            val pitch = Math.toDegrees(ori[1].toDouble()); val roll = Math.toDegrees(ori[2].toDouble())
            trySend(Heading(smooth, accuracy, decl, kotlin.math.abs(pitch) < 35 && kotlin.math.abs(roll) < 35))
        }
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> { SensorManager.getRotationMatrixFromVector(rot, e.values); emit() }
                    Sensor.TYPE_ACCELEROMETER -> { System.arraycopy(e.values, 0, g, 0, 3); hasG = true }
                    Sensor.TYPE_MAGNETIC_FIELD -> { System.arraycopy(e.values, 0, m, 0, 3); hasM = true
                        if (rv == null && hasG && SensorManager.getRotationMatrix(rot, null, g, m)) emit() }
                }
            }
            override fun onAccuracyChanged(s: Sensor, a: Int) { if (s.type == Sensor.TYPE_MAGNETIC_FIELD || s.type == Sensor.TYPE_ROTATION_VECTOR) accuracy = a }
        }
        if (rv != null) sm.registerListener(l, rv, SensorManager.SENSOR_DELAY_UI) else acc?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_UI) }
        mag?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_UI) }
        awaitClose { sm.unregisterListener(l) }
    }
}
