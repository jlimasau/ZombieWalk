package com.zombiewalk



import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.zombiewalk.databinding.ActivityMain2Binding
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.random.Random

import androidx.health.connect.client.request.ChangesTokenRequest


class MainActivity2 : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {


    //allow permission from both the app and the health app like samsung health
    //make sure to handle permission denied
    //make sure to tell user why your asking for permission
    //instruct user to turn on health connect in samsung health or google fit and allow step writing
    //if user doesnt set up health connect provide an in app step counter? or use cities only


    //have counter start when user returns to app to show distance since last use
    //save steps to a variable then get the difference between last known step count and current. use this value

    //have an in app step counter so users can play without sync. then if users want they can add sync for more items or stuff

    //check android version then direct user to use in app step counter otherwise run health connect app step count code


/*    // we have assigned sensorManger to nullable
    var sensorManager: SensorManager? = null

    // Creating a variable which will give the running status
    // and initially given the boolean value as false
    var running = false

    // Creating a variable which will counts total steps
    // and it has been given the value of 0 float
    private var totalSteps = 0f

    // Creating a variable  which counts previous total
    // steps and it has also been given the value of 0 float
    private var previousTotalSteps = 0f*/


    private lateinit var binding: ActivityMain2Binding

   private lateinit var healthConnectClient: HealthConnectClient

    private val providerPackageName = "com.google.android.apps.healthdata"

    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    val requestPermissionActivityContract =
        PermissionController.createRequestPermissionResultContract()

    val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(PERMISSIONS)) {
                // Permissions successfully granted
                println("Permissions successfully granted")

            } else {
                // Lack of required permissions
                println("Lack of required permissions")
            }
        }


    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val sensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }



    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 100


    private lateinit var sharedPreferences: SharedPreferences


    private var mInterstitialAd: InterstitialAd? = null
    private final val TAG = "MainActivity2"

    var followers = 0

    private var changesToken: String? = null





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)


        Thread {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(
                this
            ) { initializationStatus: InitializationStatus? -> }
        }
            .start()
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                Log.d(TAG, "Ad failed to load: ${adError.message}") // Print the error message
                Log.d(TAG, "Ad failed to load: ${adError.code}") // Print the error code
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
                mInterstitialAd?.show(this@MainActivity2)
            }
        })

        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show: ${adError.message}") // Print the error message
                Log.e(TAG, "Ad failed to show: ${adError.code}") // Print the error code
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }

        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.")
        }




        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)




        checkAndRequestPermissions()


        if (sensor == null) {
            binding.inAppSteps.text = "Step counter sensor is not present on this device"
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            binding.stepsTextView.text =
                "This Feature is only available on Version Android 14 and up"

        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            //healthConnectClient = HealthConnectClient.getOrCreate(this)

            checkHealthConnectStatus(this, providerPackageName)
            runBlocking {
                checkPermissionsAndRun()
            }

            lifecycleScope.launch {
                getChangesToken()
                startListeningForChanges()
            }

        }



        sharedPreferences = getSharedPreferences(StepCounterService.PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)






        val dailySteps = sharedPreferences.getInt(StepCounterService.PREF_DAILY_STEPS, 0)
        binding.inAppSteps.text = "In App Steps : $dailySteps"


        //var amountOfSteps = dailySteps



        binding.followers.text = "You have $followers followers"

}



    private suspend fun getChangesToken() {
        changesToken = healthConnectClient.getChangesToken(
            ChangesTokenRequest(
                setOf(StepsRecord::class)
            )
        )
    }

    private suspend fun startListeningForChanges() {
        while (true) {
            changesToken?.let { token ->
                val changes = healthConnectClient.getChanges(token)
                if (changes.changes.isNotEmpty()) {
                    readStepsByTimeRange(healthConnectClient, Instant.now().minusSeconds(60 * 60 * 24), Instant.now())
                }
                changesToken = changes.nextChangesToken
            }
            kotlinx.coroutines.delay(5000) // Check for changes every 5 seconds
        }
    }







    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == StepCounterService.PREF_DAILY_STEPS) {
            val dailySteps = sharedPreferences?.getInt(StepCounterService.PREF_DAILY_STEPS, 0)
            binding.inAppSteps.text = "In App Steps: $dailySteps"

            if (dailySteps != null) {
                if (dailySteps % 20 == 0){


                    var roll = (1..2).random()
                    if( roll == 1){

                        followers++
                        binding.followers.text = "You have $followers followers"
                    }
                }
            }

        }
    }


    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }





    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )
            } else {
                // Permission already granted, start the service
                startStepCounterService()
            }
        } else {
            // No need to request permission for versions lower than Android 10
            startStepCounterService()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the service
                startStepCounterService()
            } else {
                // Permission denied, handle accordingly (e.g., show a message)
            }
        }
    }


    private fun startStepCounterService() {
        val serviceIntent = Intent(this, StepCounterService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)    }

    /* private fun loadData() {
         TODO("Not yet implemented")
     }
 */

    private suspend fun checkPermissionsAndRun() {
        val timeoutMillis = 5000L // 5 seconds timeout (adjust as needed)

        val grantedResult = withTimeoutOrNull(timeoutMillis) {
            healthConnectClient.permissionController.getGrantedPermissions()
        }

        if (grantedResult == null) {
            println("Timeout: Failed to get granted permissions within $timeoutMillis ms")
            // Handle timeout here (e.g., show an error message)
            binding.stepsTextView.text =
                "Please Open the Health Connect App and Allow Step Writing from your fitness app. Also update your google play services"

            return
        }

        val granted = grantedResult
        if (granted.containsAll(PERMISSIONS)) {
            // Permissions already granted; proceed with inserting or reading data
            println("Permissions already granted")
            val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
            val endTime = Instant.now()

            lifecycleScope.launch {
                val readStepsResult = withTimeoutOrNull(timeoutMillis) {
                    readStepsByTimeRange(healthConnectClient, startTime, endTime)
                }
                if (readStepsResult == null) {
                    println("Timeout: Failed to read steps within $timeoutMillis ms")
                    // Handle timeout here (e.g., show an error message)
                }
            }
        } else {
            requestPermissions.launch(PERMISSIONS)
        }
    }


    @SuppressLint("SetTextI18n")
    private suspend fun readStepsByTimeRange(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {


        try {
            val response =
                healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )

            for (stepRecord in response.records) {
                // Process each step record
                binding.stepsTextView.text = "Steps today: ${stepRecord.count}"

            }


        } catch (e: Exception) {
            // Run error handling here.
            println("Error reading steps: $e")
        }

    }


    private fun checkHealthConnectStatus(context: Context, providerPackageName: String) {
        println("Checking Health Connect status...")
        println("Android API level: ${Build.VERSION.SDK_INT}")
        // Check if the Android version is 13 or lower
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            println("Android version is 13 or lower.")
            // Check if Health Connect is installed
            val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)
            println("Health Connect availability status: $availabilityStatus")
            if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
                println("Health Connect is not installed.")
                // Redirect to the Play Store to install Health Connect
                val uriString =
                    "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setPackage("com.android.vending")
                        data = Uri.parse(uriString)
                        putExtra("overlay", true)
                        putExtra("callerId", context.packageName)
                    }
                )
                return // Exit early since we're redirecting to the Play Store
            }
            if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                println("Health Connect needs to be updated.")
                // Optionally redirect to package installer to find a provider, for example:
                val uriString =
                    "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setPackage("com.android.vending")
                        data = Uri.parse(uriString)
                        putExtra("overlay", true)
                        putExtra("callerId", context.packageName)
                    }
                )
                return
            }
        }
        println("Health Connect is available or Android version is 14 or higher.")
        // For Android 14 and higher, or if Health Connect is installed on lower versions
        healthConnectClient = HealthConnectClient.getOrCreate(context)
        // Issue operations with healthConnectClient
    }



/*    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }


    override fun onResume() {
        super.onResume()
        running = true
        //returns the number of steps since last reboot while activated
        var stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

    }

    override fun onSensorChanged(p0: SensorEvent?) {
        // Calling the TextView that we made in activity_main.xml
        // by the id given to that TextView
        var inAppSteps1 = findViewById<TextView>(R.id.inAppSteps)

        if (running) {
            totalSteps = p0!!.values[0]

            // Current steps are calculated by taking the difference of total steps
            // and previous steps
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()

            // It will show the current steps to the user
            binding.inAppSteps.text = "$currentSteps"
           // inAppSteps1.text = ("$currentSteps")
        }
    }*/








}





