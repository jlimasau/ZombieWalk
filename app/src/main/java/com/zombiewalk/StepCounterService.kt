package com.zombiewalk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.compose.ui.input.key.type
import androidx.core.app.NotificationCompat
import com.zombiewalk.databinding.ActivityMain2Binding
import java.time.LocalDate



class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    private var dailySteps = 0
    private var lastResetTime: LocalDate = LocalDate.now()
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
         const val PREFS_NAME = "StepCounterPrefs"
         const val PREF_PREVIOUS_TOTAL_STEPS = "previousTotalSteps"
         const val PREF_DAILY_STEPS = "dailySteps"
        const val PREF_LAST_RESET_TIME = "lastResetTime"
    }





    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()

        startForegroundService()

    }




    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = event.values[0]
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
            // Do something with the step count (e.g., update UI, save to database)

            dailySteps += currentSteps
            previousTotalSteps = totalSteps
            saveData()
            resetDailyStepsIfNeeded()


            println("Steps: $currentSteps    |   Steps today: $dailySteps")


        }
    }




    private fun saveData() {

        with(sharedPreferences.edit()) {
            putFloat(PREF_PREVIOUS_TOTAL_STEPS, previousTotalSteps)
            putInt(PREF_DAILY_STEPS, dailySteps)
            putString(PREF_LAST_RESET_TIME, lastResetTime.toString())
            apply()
        }
    }


    private fun loadData() {

        previousTotalSteps = sharedPreferences.getFloat(PREF_PREVIOUS_TOTAL_STEPS, 0f)
        dailySteps = sharedPreferences.getInt(PREF_DAILY_STEPS, 0)
        lastResetTime = LocalDate.parse(sharedPreferences.getString(PREF_LAST_RESET_TIME, LocalDate.now().toString()))
    }

    private fun resetDailyStepsIfNeeded() {

        val today = LocalDate.now()
        if (today != lastResetTime){
            dailySteps = 0
            lastResetTime = today
            saveData()
        }
    }





    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun startForegroundService() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(101, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = android.graphics.Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}