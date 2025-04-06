package com.zombiewalk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.HealthConnectException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.zombiewalk.MainActivity2.Companion.PREF_HAS_GRANTED_PERMISSIONS
import com.zombiewalk.MainActivity2.Companion.PREF_IS_FIRST_RUN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.time.Duration.Companion.seconds


class MainActivity3 : AppCompatActivity() {
}


/*

    private var healthConnectPermissionsGranted = false

    private var healthConnectClient: HealthConnectClient? = null

    private val providerPackageName = "com.google.android.apps.healthdata"
    private var permissionController: PermissionController? = null
    var healthConnectFirstTime = true


    private var requestPermissions: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.all { it.value }) {
                // All permissions granted
                healthConnectPermissionsGranted = true
                lifecycleScope.launch {
                    checkPermissionsAndRun()
                    readStepsData()
                }
            } else {

                if(permissionsGranted) {
                    lifecycleScope.launch {
                        checkPermissionsAndRun()
                        readStepsData()

                    }

                }

                // Permissions denied
                // Handle accordingly
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)


        try {

            healthConnectClient = HealthConnectClient.getOrCreate(this)
            permissionController = healthConnectClient?.permissionController

        } catch (e: IllegalStateException) {
            //binding.stepsTextView.text = "Not available"
        }


        sharedPreferences =
            getSharedPreferences(StepCounterService.PREFS_NAME, Context.MODE_PRIVATE)
        isFirstRun = sharedPreferences.getBoolean(PREF_IS_FIRST_RUN, true)
        isFirstTimeGrantingPermissions =
            sharedPreferences.getBoolean(PREF_HAS_GRANTED_PERMISSIONS, true)


        requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                if (result.all { it.value }) {
                    healthConnectPermissionsGranted = true
                    lifecycleScope.launch {
                        checkPermissionsAndRun()
                    }
                } else {
                    binding.stepsTextView.text =
                        "Health Connect has not been set up, please open health connect found in your settings and allow your fitness app to write steps."
                    // healthConnectFirstTime = sharedPreferences.getBoolean("healthConnectFirstTime", true)


                    // binding.healthConnectPermissions.visibility = View.VISIBLE



                    */
/*   intent = Intent(this, MainActivity::class.java)
                       startActivity(intent)*//*

                }

            }


        lifecycleScope.launch(Dispatchers.IO){
            val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
            val endTime = Instant.now()
            healthConnectClient?.let { readStepsByTimeRange(it, startTime, endTime) }
        }


        if(healthConnectFirstTime){
            binding.healthConnectPermissions.visibility = View.VISIBLE
            binding.dismissButton.visibility = View.VISIBLE
            healthConnectFirstTime = false
        }

        if(permissionsGranted){
            binding.healthConnectPermissions.visibility = View.INVISIBLE
        }

    }




    private fun onActivityRecognitionPermissionGranted() {
        startStepCounterService()
        activityPermissionGranted = true
        sharedPreferences.edit().putBoolean("activityPermissionGranted", activityPermissionGranted)
            .commit()

        binding.inAppStepsPermission.visibility = View.INVISIBLE

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            checkHealthConnectStatus(this, providerPackageName)
            runBlocking {
                checkPermissionsAndRun()
            }
        }
        if (permissionsGranted) {
            lifecycleScope.launch {
                getChangesToken()
                startListeningForChanges()
            }

        }




    }

    private suspend fun getChangesToken() {
        if (permissionsGranted) {
            changesToken = healthConnectClient!!.getChangesToken(
                ChangesTokenRequest(
                    setOf(StepsRecord::class)
                )
            )
        }
    }



    private suspend fun startListeningForChanges() {
        if (permissionsGranted) {
            while (true) {
                changesToken?.let { token ->
                    val changes = healthConnectClient?.getChanges(token)
                    if (changes != null) {
                        if (changes.changes.isNotEmpty()) {
                            healthConnectClient?.let {
                                readStepsByTimeRange(
                                    it,
                                    Instant.now().minusSeconds(60 * 60 * 24),
                                    Instant.now()
                                )
                            }
                        }
                    }
                    if (changes != null) {
                        changesToken = changes.nextChangesToken
                    }
                }
                kotlinx.coroutines.delay(5000) // Check for changes every 5 seconds
            }
        }
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
                onActivityRecognitionPermissionGranted()
            }
        } else {
            // No need to request permission for versions lower than Android 10
            // onActivityRecognitionPermissionGranted()
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
                onActivityRecognitionPermissionGranted()

                */
/*    intent = Intent(this, MainActivity2::class.java)
                    startActivity(intent)*//*

            } else {
                // Permission denied, handle accordingly (e.g., show a message)
                Log.e(TAG, "Activity Recognition permission denied")
                binding.inAppSteps.text = "Activity Recognition permission denied. In-app step counting may not work."
                //binding.inAppStepsPermission.visibility = View.VISIBLE
                // binding.dismissButton.visibility = View.VISIBLE

                if(activityFirstTime){
                    binding.inAppStepsPermission.visibility = View.VISIBLE
                    binding.dismissButton.visibility = View.VISIBLE
                    healthConnectFirstTime = false
                    // sharedPreferences.edit().putBoolean("healthConnectFirstTime", false).commit()
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                    checkHealthConnectStatus(this, providerPackageName)
                    runBlocking {
                        checkPermissionsAndRun()
                    }
                    lifecycleScope.launch {
                        getChangesToken()
                        startListeningForChanges()
                    }
                }
                if(permissionsGranted) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
                        val endTime = Instant.now()
                        healthConnectClient?.let { readStepsByTimeRange(it, startTime, endTime) }
                    }
                }
            }

        }
    }


    private suspend fun checkPermissionsAndRun() {
        */
/*   if(!useHealthConnect){
           if(isFirstRun){
               isFirstRun = false
               with(sharedPreferences.edit()) {
                   putBoolean(PREF_IS_FIRST_RUN, isFirstRun)
                   apply()
               }
           }
               return
               }*//*

        if(permissionController != null) {

            val timeoutMillis = 3000L // 5 seconds timeout (adjust as needed)
            val maxRetries = 1
            var retryCount = 0
            while (retryCount < maxRetries) {
                val grantedResult = withTimeoutOrNull(timeoutMillis) {
                    healthConnectClient!!.permissionController.getGrantedPermissions()
                }



                */
/*val grantedResult = withTimeoutOrNull(timeoutMillis) {
            healthConnectClient.permissionController.getGrantedPermissions()
        }*//*


                if (grantedResult == null) {
                    println("Timeout: Failed to get granted permissions within $timeoutMillis ms")
                    Log.e(
                        TAG,
                        "Timeout: Failed to get granted permissions within $timeoutMillis ms"
                    )

                    // Handle timeout here (e.g., show an error message)
                    binding.stepsTextView.text =
                        "Please Open the Health Connect App and Allow Step Writing from your fitness app. Also update your google play services."

                    return
                } else {
                    Log.w(
                        TAG,
                        "Timeout occurred while getting granted permissions. Retry attempt: ${retryCount + 1}"
                    )
                    retryCount++
                    delay(2000L) // Wait for 2 seconds before retrying
                }


                */
/*    if(isFirstRun) {
            intent =         Log.w(TAG, "Timeout occurred while getting granted permissions. Retry attempt: ${retryCount + 1}")
            retryCount++
            delay(2000L) // Wait for 2 seconds before retryingIntent(this, MainActivity2::class.java)
            startActivity(intent)
        }*//*


                val granted = grantedResult
                if (granted.containsAll(PERMISSIONS)) {

                    Log.d(TAG, "Permissions already granted")

                    permissionsGranted = true
                    readStepsData()
                    // Permissions already granted; proceed with inserting or reading data


                    println("Permissions already granted")


                } else {
                    Log.d(TAG, "Requesting Health Connect permissions")

                    requestPermissions.launch(PERMISSIONS.toTypedArray())
                }
            }
        }
    }



    private fun readStepsData() {
        if (!permissionsGranted) {
            Log.e(TAG, "Cannot read steps data: Permissions not granted")
            return
        }
        lifecycleScope.launch {
            try {
                val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
                val endTime = Instant.now()
                val response = healthConnectClient?.readRecords(
                    androidx.health.connect.client.request.ReadRecordsRequest(
                        StepsRecord::class,
                        timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(
                            startTime,
                            endTime
                        )
                    )
                )
                if (response != null) {
                    for (stepRecord in response.records) {
                        // Process each step record

                        Log.d(TAG, "Steps today: ${stepRecord.count}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading steps: ${e.message}", e)
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private suspend fun readStepsByTimeRange(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {



        val timeoutMillis = 2000L
        try {
            val response = withTimeoutOrNull(timeoutMillis) {
                healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )
            }
            if (response != null) {
                for (stepRecord in response.records) {
                    // Process each step record
                    //sharedPreferences.edit().putLong("recordOfSteps", stepRecord.count).apply()
                    binding.root.post {
                        binding.stepsTextView.text = "Steps today: ${stepRecord.count}"


                        binding.healthConnectPermissions.visibility = View.INVISIBLE
                        // permissionsGranted = true

                        if (activityPermissionGranted) {
                            binding.dismissButton.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Run error handling here.
            println("Error reading steps: $e")
        }


    }



    //requires further testing
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 100,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: HealthConnectException) {
                if (e.errorCode == HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED) {
                    if (attempt == maxRetries - 1) {
                        throw e // Re-throw the exception if max retries reached
                    }
                    println("Attempt ${attempt + 1} failed. Retrying in ${currentDelay} seconds.")
                    delay(currentDelay.seconds)
                    currentDelay = (currentDelay * factor).toLong()
                } else {
                    throw e
                }
            } catch (e: Exception) {
                throw e
            }
        }
        throw Exception("Max retries reached without success")
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



    override fun onResume() {
        super.onResume()
        //dismissedPermissions = sharedPreferences.getBoolean("dismissedPermissions", false)
        // activityPermissionGranted = sharedPreferences.getBoolean("activityPermissionGranted", false)
        */
/*if(dismissedPermissions){
            binding.inAppStepsPermission.visibility = View.INVISIBLE
            binding.healthConnectPermissions.visibility = View.INVISIBLE
            binding.dismissButton.visibility = View.INVISIBLE
        }*//*

        */
/*    if(!permissionsGranted){
                binding.healthConnectPermissions.visibility = View.INVISIBLE
            }*//*


        checkAndRequestPermissions()

        lifecycleScope.launch(Dispatchers.IO){
            val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
            val endTime = Instant.now()
            healthConnectClient?.let { readStepsByTimeRange(it, startTime, endTime) }
        }


    }
    private suspend fun checkPermissionsAndRun() {
        */
/*   if(!useHealthConnect){
           if(isFirstRun){
               isFirstRun = false
               with(sharedPreferences.edit()) {
                   putBoolean(PREF_IS_FIRST_RUN, isFirstRun)
                   apply()
               }
           }
               return
               }*//*

        if(permissionController != null) {

            val timeoutMillis = 3000L // 5 seconds timeout (adjust as needed)
            val maxRetries = 1
            var retryCount = 0
            while (retryCount < maxRetries) {
                val grantedResult = withTimeoutOrNull(timeoutMillis) {
                    healthConnectClient!!.permissionController.getGrantedPermissions()
                }



                */
/*val grantedResult = withTimeoutOrNull(timeoutMillis) {
            healthConnectClient.permissionController.getGrantedPermissions()
        }*//*


                if (grantedResult == null) {
                    println("Timeout: Failed to get granted permissions within $timeoutMillis ms")
                    Log.e(
                        TAG,
                        "Timeout: Failed to get granted permissions within $timeoutMillis ms"
                    )

                    // Handle timeout here (e.g., show an error message)
                    binding.stepsTextView.text =
                        "Please Open the Health Connect App and Allow Step Writing from your fitness app. Also update your google play services."

                    return
                } else {
                    Log.w(
                        TAG,
                        "Timeout occurred while getting granted permissions. Retry attempt: ${retryCount + 1}"
                    )
                    retryCount++
                    delay(2000L) // Wait for 2 seconds before retrying
                }


                */
/*    if(isFirstRun) {
            intent =         Log.w(TAG, "Timeout occurred while getting granted permissions. Retry attempt: ${retryCount + 1}")
            retryCount++
            delay(2000L) // Wait for 2 seconds before retryingIntent(this, MainActivity2::class.java)
            startActivity(intent)
        }*//*


                val granted = grantedResult
                if (granted.containsAll(PERMISSIONS)) {

                    Log.d(TAG, "Permissions already granted")

                    permissionsGranted = true
                    readStepsData()
                    // Permissions already granted; proceed with inserting or reading data


                    println("Permissions already granted")


                } else {
                    Log.d(TAG, "Requesting Health Connect permissions")

                    requestPermissions.launch(PERMISSIONS.toTypedArray())
                }
            }
        }
    }


}*/
