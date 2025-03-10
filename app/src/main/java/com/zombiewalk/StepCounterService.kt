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
import androidx.lifecycle.lifecycleScope
import com.zombiewalk.MainActivity2.DailyStepsChangeListener
import com.zombiewalk.databinding.ActivityMain2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch



class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    private var dailySteps: Int = 0
    private var lastResetTime: LocalDate = LocalDate.now()
    private lateinit var sharedPreferences: SharedPreferences
    private var totalStepsAtReboot = 0
    private var lastSensorValue = 0f

    var followerCount1 = 0
    private var isFirstRun = true
    private var initialSensorValue: Float = 0f
    private var sensorUpdated = false

    companion object {
        const val PREFS_NAME = "StepCounterPrefs"
        const val PREF_PREVIOUS_TOTAL_STEPS = "previousTotalSteps"
        const val PREF_DAILY_STEPS = "dailySteps"
        const val PREF_LAST_RESET_TIME = "lastResetTime"
        const val PREF_TOTAL_STEPS_AT_REBOOT = "totalStepsAtReboot"
        const val PREF_LAST_SENSOR_VALUE = "lastSensorValue"
        const val PREF_IS_FIRST_RUN = "isFirstRun"
    }





    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isFirstRun = sharedPreferences.getBoolean(PREF_IS_FIRST_RUN, true)


            startForegroundService()
            dailySteps = sharedPreferences.getInt("lastStepCount", 0)
            loadData()
        }
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

    private val dailyStepsChangeListener = object : DailyStepsChangeListener {
        override fun onDailyStepsChanged(dailySteps: Int): Boolean {
            // Handle the change in daily steps here
            followerCount1 = sharedPreferences.getInt("followerCount", followerCount1)
            sharedPreferences.edit().putInt("lastStepCount", dailySteps).commit()
            if(dailySteps == 0) {
            }
            else if (dailySteps % 2 == 0) {
                var roll = (1..2).random()
                if (roll == 1) {

                    //var followerC = sharedPreferences.getInt("followerCount", followerCount1)
                    followerCount1++
                    sharedPreferences.edit().putInt("followerCount", followerCount1).apply()
                }
            }
            return true // Return true to indicate the change was accepted
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val sensorValue = event.values[0]

            if(isFirstRun && !sensorUpdated){
                initialSensorValue = sensorValue
                sensorUpdated = true
                previousTotalSteps = initialSensorValue
                lastSensorValue = initialSensorValue

               // isFirstRun = false
                saveData()
               //sensorManager.unregisterListener(this)

            } else {
                //detect reboot

                val rebooted = sensorValue < lastSensorValue
                if (rebooted) {
                    totalStepsAtReboot = dailySteps
                    //calculate daily steps
                    dailySteps = sensorValue.toInt() + totalStepsAtReboot
                } else {
                    val stepIncrement = (sensorValue - lastSensorValue).toInt()
                    if(stepIncrement > 0){
                        dailySteps += stepIncrement
                    }
                    else{

                    }



                }

                lastSensorValue = sensorValue

               // val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
                // Do something with the step count (e.g., update UI, save to database)


                    //dailySteps += currentSteps


               // previousTotalSteps = totalSteps
                saveData()



                println("Steps:    |   Steps today: $dailySteps")

                dailyStepsChangeListener.onDailyStepsChanged(dailySteps)

                /*        var followerC = sharedPreferences.getInt("followerCount", 0)
                       // binding.inAppSteps.text = "In App Steps: $dailySteps"
                        //loadItems()
                        //the number of steps to trigger a follower
                        if (dailySteps %5 == 0) {
                            var roll = (1..2).random()
                            if (roll == 1) {
                                followerC++
                                sharedPreferences.edit().putInt("followerCount", followerC).apply()
                                //loadOneZombie()
                               // binding.followers.text = "You have $followers followers"

                            }
                        }*/

            }

        }
    }




    private fun saveData() {

        with(sharedPreferences.edit()) {
            putInt(PREF_TOTAL_STEPS_AT_REBOOT, totalStepsAtReboot)
            putFloat(PREF_LAST_SENSOR_VALUE, lastSensorValue)
            putFloat(PREF_PREVIOUS_TOTAL_STEPS, previousTotalSteps)
            putInt(PREF_DAILY_STEPS, dailySteps)
            putString(PREF_LAST_RESET_TIME, lastResetTime.toString())
            putBoolean(PREF_IS_FIRST_RUN, isFirstRun)
            commit()
        }
    }


    private fun loadData() {
        if(isFirstRun){
            stepCounterSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        else {



        totalStepsAtReboot = sharedPreferences.getInt(PREF_TOTAL_STEPS_AT_REBOOT, 0)
        lastSensorValue = sharedPreferences.getFloat(PREF_LAST_SENSOR_VALUE, 0f)
        previousTotalSteps = sharedPreferences.getFloat(PREF_PREVIOUS_TOTAL_STEPS, 0f)
        dailySteps = sharedPreferences.getInt(PREF_DAILY_STEPS, dailySteps)
        lastResetTime = LocalDate.parse(sharedPreferences.getString(PREF_LAST_RESET_TIME, LocalDate.now().toString()))
        }
        resetDailyStepsIfNeeded()
    }

    private fun resetDailyStepsIfNeeded() {

        val today = LocalDate.now()
        if (today != lastResetTime){
            dailySteps = 0
            totalStepsAtReboot = 0
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
        saveData()
        sensorManager.unregisterListener(this)
    }


}