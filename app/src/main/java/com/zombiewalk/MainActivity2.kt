package com.zombiewalk


import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorManager
import android.health.connect.HealthConnectException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.zombiewalk.databinding.ActivityMain2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.random.Random
import kotlin.reflect.jvm.internal.impl.resolve.constants.StringValue


class MainActivity2 : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    //check that fresh start up is smooth and user is directed to accept permissions with a reason
    //allow permission from both the app and the health app like samsung health
    //make sure to handle permission denied
    //make sure to tell user why your asking for permission
    //instruct user to turn on health connect in samsung health or google fit and allow step writing
    //if user doesnt set up health connect provide an in app step counter? or use cities and streets only


    //have counter start when user returns to app to show distance since last use
    //save steps to a variable then get the difference between last known step count and current. use this value

    //have an in app step counter so users can play without sync. then if users want they can add sync for more items or stuff

    //check android version then direct user to use in app step counter otherwise run health connect app step count code
    //go to health connect website and get version support then have app match, update play console versioning



    //fix return location initiation
    //write privacy policy
    //rewarded ad for cool items
    //add item ammo or use amount

    //zombies added over time






    //test for followers appearing at 0 steps, could have been from previous day, in app steps not zero, and items preloaded




    //double check privacy policy appears on healthconnect permission request

    // after accepting both permissions buttons should dissapear or leave a message to begin walking
    //dismissing health connect permission shouldnt try again


    //grant health connect permissions button should open settings menu




    private lateinit var binding: ActivityMain2Binding

    private var healthConnectClient: HealthConnectClient? = null

    private val providerPackageName = "com.google.android.apps.healthdata"

    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    val requestPermissionActivityContract =
        PermissionController.createRequestPermissionResultContract()
    private var healthConnectPermissionsGranted = false

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

    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val sensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 100

    private lateinit var sharedPreferences: SharedPreferences

    private var mInterstitialAd: InterstitialAd? = null
    private final val TAG = "MainActivity2"

    var followers = 0

    private var changesToken: String? = null

    private var items: ArrayList<String> = arrayListOf()

    var currentItem: String? = null
    var numberOfButtons = 0

    data class Zombie(val imageView: ImageView, val newId: Int)
    companion object {
        private val random = Random(System.nanoTime())


        private var nextId = 1 //static counter for sequential id

        fun createZombie(imageView: ImageView): Zombie {
            return Zombie(imageView, nextId++)
        }

        private const val PERMISSION_REQUEST_CODE = 100
        const val PREFS_NAME = "StepCounterPrefs"
        const val PREF_IS_FIRST_RUN = "isFirstRun"
        const val PREF_HAS_GRANTED_PERMISSIONS = "hasGrantedPermissions"
        const val PREF_DAILY_STEPS = "dailySteps"
    }

    private var zombies: MutableList<ImageView> = mutableListOf()

    private val zombieMap: MutableMap<ImageView, Zombie> = mutableMapOf()

    data class ZombieData(val newId: Int, var hp: Int)

    fun returnId(imageView: ImageView): Int? {
        val zombie = zombieMap[imageView]
        return zombie?.newId // Use safe call to handle potential null
    }

    private var experience = 0
    var timesBatUsed = 0
    var timesMolotovUsed = 0
    var timesTomahawkUsed = 0
    var timesCrossbowUsed = 0
    var timesMacheteUsed = 0



    var testerMode = false


    private val targetedZombies = mutableSetOf<ImageView>()

    private var isDroppingItem = false

    private var itemDropped = false

    //private val handler = Handler(Looper.getMainLooper())
    var crossbowInUse = false
    var itemRarity = 45

    private var isFirstRun = true

    private var isFirstTimeGrantingPermissions = true

    private var permissionController: PermissionController? = null

    private var permissionsGranted = false


    var roll1 = Random.nextInt(1, itemRarity)
    var randzombie = Random.nextInt(1,2)

    var molotovAmmo = 0
    var macheteAmmo = 0
    var crossbowAmmo = 0
    var batAmmo = 0
    var tomahawkAmmo = 0

    var currentItemAmmo: Int = 0

    var lastDailySteps = 0

    var dailySteps1 = 0

    var firstTime = true

    var dismissedPermissions = false
    var activityPermissionGranted = false
    var healthConnectFirstTime = true
    var activityFirstTime = true

    var health = 9
    var noFootSteps = false

    private var footSteps: ImageView? = null



    var gameOver = false
    var zombieCounter = 0

    var shieldIsOn = false
    private var shield: ImageView? = null
    var shieldHealth = 0
    var lastZombie: ImageView? = null
    var oneAd = true
    var pauseSensor = false

    var stepTotal = 0
    var latestZombie: ImageView? = null
    private var rewardedAd: RewardedAd? = null
   // private final var TAG = "MainActivity2"

    interface DailyStepsChangeListener {
        fun onDailyStepsChanged(dailySteps: Int): Boolean
    }

    private val dailyStepsChangeListener = object : DailyStepsChangeListener {
        override fun onDailyStepsChanged(dailySteps: Int): Boolean {
   /*         if(dailySteps> lastDailySteps){
                for( i in lastDailySteps .. dailySteps){
                    dailySteps1=i
                    sharedPreferences.edit().putInt(StepCounterService.PREF_DAILY_STEPS, dailySteps1).apply()
                }
            }*/



            pauseSensor = sharedPreferences.getBoolean("pauseSensor", false)
            if(pauseSensor){

            }
            else {

                binding.inAppSteps.text = "In App Steps: $dailySteps"
            }

            lastDailySteps = dailySteps
            with(sharedPreferences.edit()){
                putInt(PREF_DAILY_STEPS, dailySteps)
                commit()
            }
            stepTotal = dailySteps
            sharedPreferences.edit().putInt("stepTotal", stepTotal).commit()
            //loadItems()
            loadZombies()


           if( dailySteps == 0){

           }
            else {
               //the number of steps to trigger items
               //rarity
     /*          if (dailySteps % 60 == 0) {
                   var roll = (1..2).random()
                   if (roll == 1) {
                       if (!items.contains("bat")) {
                           items.add("bat")
                       }

                           batAmmo += 25
                           setUses()
                       saveItems()

                       loadItems()


                   }
               }*/
         /*      if (dailySteps % 55 == 0) {
                   var roll = (1..2).random()
                   if (roll == 1) {
                       if (!items.contains("tomahawk")) {
                           items.add("tomahawk")
                       }
                           tomahawkAmmo += 50
                           setUses()
                       saveItems()

                       loadItems()
                   }
               }*/

/*               if (dailySteps % 90 == 0) {
                   var roll = (1..2).random()
                   if (roll == 1) {
                       if (!items.contains("molotov")) {
                           items.add("molotov")
                       }
                           molotovAmmo += 10
                           setUses()
                       saveItems()

                       loadItems()
                   }
               }*/
/*
               if (dailySteps % 60 == 0) {
                   var roll = (1..2).random()
                   if (roll == 1) {
                       if (!items.contains("crossbow")) {
                           items.add("crossbow")
                       }
                           crossbowAmmo += 20
                           setUses()
                       saveItems()
                       loadItems()

                   }
               }*/
/*
               if (dailySteps % 40 == 0) {
                   var roll = (1..2).random()
                   if (roll == 1) {
                       if (!items.contains("machete")) {
                           items.add("machete")
                       }
                           macheteAmmo+= 15
                           setUses()
                       saveItems()

                       loadItems()
                   }
               }*/

               if (dailySteps % 30 == 0) {
                   lifecycleScope.launch(Dispatchers.IO){
                       val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
                       val endTime = Instant.now()
                       healthConnectClient?.let { readStepsByTimeRange(it, startTime, endTime) }
                   }
               }




                   //loads ad every x steps

           }

        /*    runOnUiThread {
                loadItems()

            }*/
         /*   lifecycleScope.launch(Dispatchers.IO){
                val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
                val endTime = Instant.now()

                healthConnectClient?.let { readStepsByTimeRange(it, startTime, endTime) }
            }*/

            // can readd this line but somewhere within oncreate with a recursive call that happens slowly




            return true // Return true to indicate the change was accepted
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)






        try {

            healthConnectClient = HealthConnectClient.getOrCreate(this)
            permissionController = healthConnectClient?.permissionController

        } catch (e: IllegalStateException) {
            //binding.stepsTextView.text = "Not available"
        }


        Thread {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(
                this
            ) { initializationStatus: InitializationStatus? -> }
        }
            .start()

        var adRequest = AdRequest.Builder().build()
        RewardedAd.load(this,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
            }
        })


        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)



        if (sensor == null) {
            binding.inAppSteps.text = "Step counter sensor is not present on this device"
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            binding.stepsTextView.text =
                "This Feature is only available on Version Android 14 and up"
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



                 /*   intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)*/
                }

            }
        lifecycleScope.launch(Dispatchers.IO){
        checkAndRequestPermissions()
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        var dailySteps = sharedPreferences.getInt(StepCounterService.PREF_DAILY_STEPS, 0)
        pauseSensor = sharedPreferences.getBoolean("pauseSensor", false)
        if(pauseSensor){

        }
        else {

            /*lastDailySteps = sharedPreferences.getInt("lastStepCount", 0)
            if(gameOver){
                binding.inAppSteps.text = "In App Steps: $lastDailySteps"
            }
            else {*/


            dailySteps = sharedPreferences.getInt(StepCounterService.PREF_DAILY_STEPS, 0)
            binding.inAppSteps.text = "In App Steps: $dailySteps"
        }

        //binding.inAppSteps.text = "In App Steps : $dailySteps"
        if(followers == 0){
            binding.followers.text = "No zombies here"
        }
        else {
            binding.followers.text = "You have $followers followers"
        }


        lifecycleScope.launch(Dispatchers.IO){
            val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
            val endTime = Instant.now()
            healthConnectClient?.let { readStepsByTimeRange(it, startTime, endTime) }
        }


     /*   var lastRecordOfSteps = sharedPreferences.getInt(StepCounterService., 0)
        binding.stepsTextView.text = "Last Record of Steps: $lastRecordOfSteps"
*/
        if (testerMode == true) {
            health = 1
            sharedPreferences.edit().putInt("health" , health).commit()
            for (i in 1..50) {
                followers++
                loadOneZombie()
            }
            if(!items.contains("tomahawk")){
                tomahawkAmmo = 50

                items.add("tomahawk")
            }
            if(!items.contains("machete")){
                macheteAmmo = 15

                items.add("machete")
            }
            if(!items.contains("molotov")){
                molotovAmmo =10
                items.add("molotov")
            }
            if(!items.contains("bat")){
                batAmmo = 25

                items.add("bat")
            }
            if(!items.contains("crossbow")){
                crossbowAmmo = 50

                items.add("crossbow")
            }


            saveItems()
        }

        sharedPreferences.getBoolean("firsttime", true)
        //loadItems()
  /*      if(firstTime){
            if(!items.contains("bat")){


            items.add("bat")
            batAmmo = 25
            saveItems()
        }
            firstTime = false
            sharedPreferences.edit().putBoolean("firsttime", firstTime).commit()
            }*/

      /*   activityPermissionGranted = sharedPreferences.getBoolean("activityPermissionGranted", false)

        permissionsGranted = sharedPreferences.getBoolean("permissionsGranted", false)

        if(permissionsGranted ){
            binding.healthConnectPermissions.visibility = View.INVISIBLE
           // binding.dismissButton.visibility = View.VISIBLE
        }
        if(activityPermissionGranted){
            binding.inAppStepsPermission.visibility = View.INVISIBLE
           // binding.dismissButton.visibility = View.VISIBLE
        }
        if(activityPermissionGranted && permissionsGranted){
            binding.dismissButton.visibility = View.INVISIBLE
        }*/


        //make sure each button opens its respective permission
        binding.healthConnectPermissions.setOnClickListener {
            lifecycleScope.launch {
                checkPermissionsAndRun()
            }
        }
        binding.inAppStepsPermission.setOnClickListener {
            lifecycleScope.launch {
                checkPermissionsAndRun()
            }
        }



        if(healthConnectFirstTime){
            binding.healthConnectPermissions.visibility = View.VISIBLE
            binding.dismissButton.visibility = View.VISIBLE
            healthConnectFirstTime = false
        }

   /*     if(activityFirstTime){
            binding.inAppStepsPermission.visibility = View.VISIBLE
            binding.dismissButton.visibility = View.VISIBLE
            activityFirstTime = false
        }*/
        //dismissedPermissions = sharedPreferences.getBoolean("dismissedPermissions", false)
        //dismissedPermissions = false

        binding.dismissButton.setOnClickListener {
            binding.inAppStepsPermission.visibility = View.INVISIBLE
            binding.healthConnectPermissions.visibility = View.INVISIBLE
            binding.dismissButton.visibility = View.INVISIBLE
            dismissedPermissions = true
            sharedPreferences.edit().putBoolean("dismissedPermissions", dismissedPermissions).commit()
        }

        if(permissionsGranted){
            binding.healthConnectPermissions.visibility = View.INVISIBLE
        }
        if(activityPermissionGranted){
            binding.inAppStepsPermission.visibility = View.INVISIBLE
        }
        if(permissionsGranted && activityPermissionGranted){
            binding.dismissButton.visibility = View.INVISIBLE
        }



        health = sharedPreferences.getInt("health" , 9)
        if(shieldIsOn){

                sharedPreferences.getInt("shieldHealth", shieldHealth)
                binding.health.text = "Health: $health Shield: $shieldHealth"
                //sharedPreferences.edit().putInt("health", health).commit()
        }
        else {
            binding.health.text = "Health: $health"

        }


        loadExperience()
        loadZombies()

        loadFootSteps()
        loadItems()
    /*    intent = Intent(this, MainActivity2::class.java)
        startActivity(intent)*/
        if( health < 1) {
            health = 0
            sharedPreferences.edit().putInt("health" , health).commit()

            gameOver = true
            sharedPreferences.edit().putBoolean("gameOver", gameOver).commit()
            stepTotal = sharedPreferences.getInt("stepTotal", 0)
            binding.inAppSteps.text = "In App Steps: $stepTotal"
            //binding.followers.text = "No zombies here"
            currentItem = "nothing"
            binding.health.text = "Game Over"
            binding.buttonlayout.visibility = View.INVISIBLE
            items.removeAll(items)
            saveItems()
            //loadItems()
           // items.removeAll(items)
            binding.respawnWithItems.visibility = View.VISIBLE
            binding.respawn.visibility = View.VISIBLE


            noFootSteps = true
            loadFootSteps()





        }





        binding.respawn.setOnClickListener {
            var pauseSensor = false
            sharedPreferences.edit().putBoolean("pauseSensor", pauseSensor).commit()
            binding.inAppSteps.text = "In App Steps: 0"
            //sharedPreferences.edit().putInt(StepCounterService.PREF_DAILY_STEPS, 0).commit()


            oneAd = true
            gameOver = false
            sharedPreferences.edit().putBoolean("gameOver", gameOver).commit()
            var dontResetCount = false
            sharedPreferences.edit().putBoolean("dontResetCount", dontResetCount).commit()
            sharedPreferences.edit().putInt("lastStepCount", 0).commit()

            followers = 0
            binding.followers.text = "No zombies here"
            sharedPreferences.edit().putInt("followerCount", followers).commit()
            for( zombie in zombies){

               // zombies.remove(zombie)
                binding.parentLayout.removeView(zombie)

                //zombies.removeAll(zombies)
                //binding.zombieLayout.removeAllViews()
            }
            zombies.removeAll(zombies)


            items.removeAll(items)
            currentItem = null
           // items.add("bat")
            batAmmo = 0
            macheteAmmo = 0
            tomahawkAmmo = 0
            crossbowAmmo = 0
            molotovAmmo = 0
            saveItems()
            loadItems()
            binding.buttonlayout.visibility = View.VISIBLE

            binding.respawn.visibility = View.INVISIBLE
            binding.respawnWithItems.visibility = View.INVISIBLE
            binding.buttonlayout.post {
                noFootSteps = false
                loadFootSteps()
                health = 9
                if(shieldIsOn) {
                    sharedPreferences.getInt("shieldHealth", shieldHealth)
                    binding.health.text = "Health: $health Shield: $shieldHealth"
                }
                else {
                    binding.health.text = "Health: $health"
                }
                //save health
                sharedPreferences.edit().putInt("health", health).commit()
               // binding.parentLayout.addView(zombie)

            }
        }

        binding.respawnWithItems.setOnClickListener {

      /*      var adRequest = AdRequest.Builder().build()
            RewardedAd.load(this,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad was loaded.")
                    rewardedAd = ad
                }
            })*/
            //add ad

     /*       private var rewardedAd: RewardedAd? = null
            private final var TAG = "MainActivity"
*/

            rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    rewardedAd = null
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

            rewardedAd?.let { ad ->
                ad.show(this, OnUserEarnedRewardListener { rewardItem ->
                    // Handle the reward.






            //var lastStepCount = sharedPreferences.getInt("lastStepCount", 0)
            var pauseSensor = false
            sharedPreferences.edit().putBoolean("pauseSensor", pauseSensor).commit()


            //binding.inAppSteps.text = "In App Steps: 0"
            //sharedPreferences.edit().putInt(StepCounterService.PREF_DAILY_STEPS, 0).commit()

            oneAd = true
            gameOver = false
            sharedPreferences.edit().putBoolean("gameOver", gameOver).commit()
            var dontResetCount = true
            sharedPreferences.edit().putBoolean("dontResetCount", dontResetCount).commit()
/*
            followers = 0
            binding.followers.text = "No zombies here"
            sharedPreferences.edit().putInt("followerCount", followers).commit()
            for( zombie in zombies){

                // zombies.remove(zombie)
                binding.parentLayout.removeView(zombie)

                //zombies.removeAll(zombies)
                //binding.zombieLayout.removeAllViews()
            }
            zombies.removeAll(zombies)


            currentItem = null
            // items.add("bat")
            batAmmo = 0
            macheteAmmo = 0
            tomahawkAmmo = 0
            crossbowAmmo = 0
            molotovAmmo = 0
            saveItems()*/
            loadItems()
            binding.buttonlayout.visibility = View.VISIBLE
            binding.respawn.visibility = View.INVISIBLE
            binding.respawnWithItems.visibility = View.INVISIBLE
            binding.buttonlayout.post {
                noFootSteps = false
                loadFootSteps()
                health = 9
                binding.health.text = "Health: $health"
                sharedPreferences.edit().putInt("health", health).commit()
                //shieldHealth = sharedPreferences.getInt("shieldHealth", shieldHealth)
             /*   if(shieldIsOn) {
                    sharedPreferences.getInt("shieldHealth", shieldHealth)
                    binding.health.text = "Health: $health Shield: $shieldHealth"
                }*/



                //save health
               // sharedPreferences.edit().putInt("health", health).commit()
                // binding.parentLayout.addView(zombie)

            }



       /*     sharedPreferences.edit().putInt(StepCounterService.PREF_DAILY_STEPS, lastStepCount).commit()

            sharedPreferences.edit().putInt("lastStepCount", lastStepCount).commit()*/
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
                    Log.d(TAG, "User earned the reward.")
                })
            } ?: run {
                Log.d(TAG, "The rewarded ad wasn't ready yet.")
            }
         /*   adRequest = AdRequest.Builder().build()
            RewardedAd.load(this,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad was loaded.")
                    rewardedAd = ad
                }
            })*/

        }


        if(binding.inAppSteps.text.equals("In App Steps: ")){

            /*  checkAndRequestPermissions()

              lifecycleScope.launch(Dispatchers.IO){
                  val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
                  val endTime = Instant.now()
                  healthConnectClient?.let { readStepsByTimeRange(it, startTime, endTime) }
              }*/
            lastDailySteps = sharedPreferences.getInt("stepTotal", 0)
            dailySteps = lastDailySteps
            sharedPreferences.edit().putInt("lastStepCount", dailySteps).commit()

            //sharedPreferences.edit().putInt(StepCounterService.PREF_DAILY_STEPS, dailySteps).commit()
            binding.inAppSteps.text = "In App Steps: $dailySteps"
            sharedPreferences.edit().putBoolean("pauseSensor", false).commit()


        }


    }











private fun updateShield() {
    shield?.let {
        binding.parentLayout.removeView(it)
    }
    if(shieldIsOn){

        shield = ImageView(this@MainActivity2)
        shield?.setImageResource(R.drawable.shield)

        shield?.background = null
        shield?.visibility = View.INVISIBLE


        binding.parentLayout.addView(shield)
        shield?.layoutParams?.width = 200
        shield?.layoutParams?.height = 200

        shield?.bringToFront()
        // Use post() to get the correct width and height
        shield?.post {
            // Now we know the width and height
            shield?.x = binding.mainLayout.width.toFloat() / 2 - 100
            shield?.y = binding.mainLayout.height.toFloat() - binding.buttonlayout.height - 100 -200
            shield?.visibility = View.VISIBLE
        }
    }
}



    private fun loadFootSteps() {



        footSteps?.let {
            binding.parentLayout.removeView(it)
        }

        footSteps = ImageView(this@MainActivity2)
        footSteps?.post {
            // Now we know the width and height
            footSteps?.x = binding.mainLayout.width.toFloat() / 2 - 100
            footSteps?.y = binding.mainLayout.height.toFloat() - binding.buttonlayout.height - 100

            // Remove any existing background (not really needed for ImageView, but good practice)
            footSteps?.background = null


            footSteps?.visibility = View.INVISIBLE

            //change to footsteps gif

        }
        if(!noFootSteps){

            System.out.println("loadFootSteps")


            lifecycleScope.launch(Dispatchers.IO) {
                footSteps?.let {
                    loadGifIntoImageView(
                        it,
                        R.drawable.walking,
                        200,
                        200
                    ) // Example: Resize to 200x200 pixels
                }
            }

            footSteps?.let {
                binding.parentLayout.addView(it)
                //zombies.add(zombie)


                it.bringToFront()
                it.postDelayed({
                    it.visibility = View.VISIBLE

                    //animateZombie(footSteps)
                }, 100)
            }

        }
        else{
            println("no foot steps")
            footSteps?.let {
                binding.parentLayout.removeView(it)
            }

            footSteps?.postDelayed({
                footSteps?.visibility = View.INVISIBLE

                //animateZombie(footSteps)
            }, 100)




        }

        // if (gameOver == true){
        // binding.parentLayout.removeView(footSteps)
        // }
        //  saveZombies()    }
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
               // getChangesToken()
                //startListeningForChanges()
            }

                }




    }


    private fun saveZombies() {
        sharedPreferences.edit().putInt("followerCount", followers).commit()
        //type of zombie count to load other zombies
    }

    private fun loadZombies() {
        followers = sharedPreferences.getInt("followerCount", 0)
        if(followers <= 0){
            binding.followers.text = "No zombies here"
          //  zombies.removeAll(zombies)
           // binding.zombieLayout.removeAllViews()

        }
        else {
            binding.followers.text = "You have $followers followers"

        }

        if (zombies.size < followers) {
            for (i in zombies.size until followers) {
                if(gameOver == false){
                    loadOneZombie()
                }


            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event?.action == MotionEvent.ACTION_DOWN) {
            val xtap = event.x
            val ytap = event.y
            when (currentItem) {
                "molotov" -> {
                    molotovAmmo--
                    saveItems()
                    setUses()
                    val radius = 400F
                    blastRadius(xtap, ytap, radius)
                    timesMolotovUsed++

                    val molotov = ImageView(this@MainActivity2)
                    // molotov.setImageResource(R.drawable.molotov)

                    molotov.background = null
                    molotov.visibility = View.INVISIBLE


                    binding.parentLayout.addView(molotov)
                    molotov.layoutParams.width = radius.toInt()
                    molotov.layoutParams.height = radius.toInt()


                    // Use post() to get the correct width and height
                    molotov.post {
                        // Now we know the width and height
                        molotov.x = xtap - molotov.width / 2
                        molotov.y = ytap - molotov.height / 2
                        molotov.visibility = View.VISIBLE


                    }
                    molotov.invalidate() // Force redraw
                    molotov.setImageResource(0) // Clear any previous image
                    molotov.requestLayout()

                    molotov.bringToFront()
                    lifecycleScope.launch(Dispatchers.IO) {
                        loadGifIntoImageView(
                            molotov,
                            R.drawable.molotov,
                            500,
                            500
                        ) // Example: Resize to 200x200 pixels
                    }

                    // Use coroutines to remove the view after 1 second
                    lifecycleScope.launch {
                        delay(1000) // Wait for 1 second
                        binding.parentLayout.removeView(molotov)
                    }


                    if (currentItemAmmo<1) {
                        molotovAmmo = 0
                        setUses()
                        items.remove("molotov")


                        saveItems()
                        loadItems()
                        timesMolotovUsed = 0
                        currentItem = null
                        sharedPreferences.edit().putString("CI", currentItem).commit()

                    }
                    //add logic for multiple items or multiple uses
                    //if item runs out{
                    //currentItem = null
                    //sharePreferences.edit().putString("CI", currentItem).commit()
                }

                "bat" -> {
                    batAmmo--
                    saveItems()
                    setUses()

                    handleZombieTap(xtap, ytap)
                    timesBatUsed++

                    val Bat = ImageView(this@MainActivity2)
                    Bat.setImageResource(R.drawable.bat)

                    Bat.background = null
                    Bat.visibility = View.INVISIBLE


                    binding.parentLayout.addView(Bat)
                    Bat.layoutParams.width = 270
                    Bat.layoutParams.height = 270

                    Bat.bringToFront()
                    // Use post() to get the correct width and height
                    Bat.post {
                        // Now we know the width and height
                        Bat.x = xtap - Bat.width + 50
                        Bat.y = ytap - Bat.height + 125
                        Bat.visibility = View.VISIBLE

                        Bat.postDelayed({
                            swipeAnimation(Bat)
                        }, 100)
                    }


                    if (currentItemAmmo < 1) {
                        batAmmo=0
                        setUses()
                        items.remove("bat")

                        saveItems()
                        loadItems()
                        timesBatUsed = 0
                        currentItem = null
                        sharedPreferences.edit().putString("CI", currentItem).commit()
                    }
                    //add logic for multiple items or multiple uses
                    //if item runs out{
                    //currentItem = null
                    //sharePreferences.edit().putString("CI", currentItem).commit()
                }

                "tomahawk" -> {
                    tomahawkAmmo--
                    saveItems()
                    setUses()
                    handleZombieTap(xtap, ytap)


                    timesTomahawkUsed++

                    val tomahawk = ImageView(this@MainActivity2)
                    tomahawk.setImageResource(R.drawable.tomahawk)

                    tomahawk.background = null
                    tomahawk.visibility = View.INVISIBLE


                    binding.parentLayout.addView(tomahawk)
                    tomahawk.layoutParams.width = 250
                    tomahawk.layoutParams.height = 250

                    tomahawk.bringToFront()
                    // Use post() to get the correct width and height
                    tomahawk.post {
                        // Now we know the width and height
                        tomahawk.x = xtap - tomahawk.width + 50
                        tomahawk.y = ytap - tomahawk.height + 125
                        tomahawk.visibility = View.VISIBLE

                        tomahawk.postDelayed({
                            swipeAnimation(tomahawk)
                        }, 100)
                    }

                    if (currentItemAmmo < 1) {
                        tomahawkAmmo = 0
                        setUses()
                        items.remove("tomahawk")

                        saveItems()
                        loadItems()
                        timesTomahawkUsed = 0
                        currentItem = null
                        sharedPreferences.edit().putString("CI", currentItem).commit()
                    }
                    //add logic for multiple items or multiple uses
                    //if item runs out{
                    //currentItem = null
                    //sharePreferences.edit().putString("CI", currentItem).commit()
                }

                "crossbow" -> {

                    crossbowAmmo--
                    saveItems()
                    setUses()
                    if (crossbowInUse) {
                        return super.onTouchEvent(event)
                    }

                    crossbowInUse = true
                    //in the future change this to aim
                    var closestZombie1 = closestZombie(xtap, ytap)
                    timesCrossbowUsed++

                    var closestZombieX = closestZombie1?.x
                    var closestZombieY = closestZombie1?.y

                    val crossbow = ImageView(this@MainActivity2)
                    crossbow.setImageResource(R.drawable.crossbow)

                    crossbow.background = null
                    crossbow.visibility = View.INVISIBLE


                    binding.parentLayout.addView(crossbow)
                    crossbow.layoutParams.width = 250
                    crossbow.layoutParams.height = 250

                    crossbow.bringToFront()
                    // Use post() to get the correct width and height
                    crossbow.post {
                        // Now we know the width and height
                        crossbow.x = xtap - crossbow.width / 2
                        crossbow.y = ytap - crossbow.height / 2
                        crossbow.visibility = View.VISIBLE

                        crossbow.postDelayed({
                            angleAnimation(crossbow, closestZombieX, closestZombieY, closestZombie1)
                        }, 100)

                        /* lifecycleScope.launch {
                             delay(600) // Wait for 1 second
                             binding.parentLayout.removeView(crossbow)

                         }*/

                    }

                /*    var crossbowUses = 20
                    if (testerMode == true){
                        crossbowUses = 100
                    }*/
                    if (currentItemAmmo < 1) {
                        crossbowAmmo = 0

                        setUses()


                        items.remove("crossbow")

                        saveItems()
                        loadItems()
                        //if item runs out{
                        timesCrossbowUsed = 0
                        currentItem = null
                        sharedPreferences.edit().putString("CI", currentItem).commit()
                    }
                    // handler.postDelayed({
                    crossbowInUse = false
                    //}, 500)
                }

                "machete" -> {
                    macheteAmmo--
                    saveItems()
                    setUses()
                    handleZombieTap(xtap, ytap)
                    timesMacheteUsed++

                    val machete = ImageView(this@MainActivity2)
                    machete.setImageResource(R.drawable.machete)

                    machete.background = null
                    machete.visibility = View.INVISIBLE


                    binding.parentLayout.addView(machete)
                    machete.layoutParams.width = 200
                    machete.layoutParams.height = 200

                    machete.bringToFront()
                    // Use post() to get the correct width and height
                    machete.post {
                        // Now we know the width and height
                        machete.x = xtap - machete.width + 50
                        machete.y = ytap - machete.height + 100
                        machete.visibility = View.VISIBLE

                        machete.postDelayed({
                            swipeAnimation(machete)
                        }, 100)
                    }


                    /* //use this if it's a gif
                      lifecycleScope.launch(Dispatchers.IO) {
                             loadGifIntoImageView(machete, R.drawable.zombie, 200, 200) // Example: Resize to 200x200 pixels
                         }*/

                    if (currentItemAmmo < 1) {

                        macheteAmmo = 0
                        setUses()

                        items.remove("machete")

                        saveItems()
                        loadItems()
                        //if item runs out{
                        timesMacheteUsed = 0
                        currentItem = null
                        sharedPreferences.edit().putString("CI", currentItem).commit()
                    }
                }

                "hands" -> {


                    setUses()

                    handleZombieTap(xtap, ytap)


                }
                null -> {
                    handleZombieTap(xtap, ytap)
                }






            }
           // loadItems()
            setUses()
            saveZombies()


        }
        return super.onTouchEvent(event)
    }


    private fun saveItems() {
        val gson = Gson()
        val json = gson.toJson(items)
        sharedPreferences.edit().putString("items", json).commit()

        sharedPreferences.edit().putInt("batAmmo", batAmmo).commit()
        sharedPreferences.edit().putInt("molotovAmmo", molotovAmmo).commit()
        sharedPreferences.edit().putInt("tomahawkAmmo", tomahawkAmmo).commit()
        sharedPreferences.edit().putInt("macheteAmmo", macheteAmmo).commit()
        sharedPreferences.edit().putInt("crossbowAmmo", crossbowAmmo).commit()


    }



  /*  private fun getRect(image: ImageView, callback: (Rect) -> Unit) {
        image.post {
            val rect = Rect(image.left, image.top, image.right, image.bottom)
            callback(rect)
        }


    }*/

    private fun handleZombieTap(xtap: Float, ytap: Float) {



        //System.out.println("THIS:   ")

        var chosenZombie: ImageView ?= null

        //calculate all zombie that were withing the click zone
        val clickZoneZombies = ArrayList<ImageView>()
        for (zombie in zombies) {


            val zombieX = zombie.x.toInt()
            val zombieY = zombie.y.toInt()
            val zombieWidth = zombie.width
            val zombieHeight = zombie.height
            val rect = Rect(zombieX, zombieY, zombieX + zombieWidth, zombieY + zombieHeight)

            /* getRect(zombie) { rect ->
                 println("THIS:   " + rect)*/

            if (rect.contains(xtap.toInt(), ytap.toInt())) {
                clickZoneZombies.add(zombie)


                //}
            }
            //System.out.println(getRect(zombie))

            //if click is within zombie.rect then add to clickzonezombies list


        }
        System.out.println("THIS:   " + clickZoneZombies)
        //if there is a zombie within the clickzone find the closest one
        if (clickZoneZombies.isNotEmpty()) {

            var closestZombie: ImageView = clickZoneZombies[0]
            var minDistance = calculateDistance(xtap, ytap, closestZombie)

            for (i in 1 until clickZoneZombies.size) {
                val currentDistance = calculateDistance(xtap, ytap, clickZoneZombies[i])
                if (currentDistance < minDistance) {
                    minDistance = currentDistance
                    closestZombie = clickZoneZombies[i]
                }
            }




            //lower chosenZombies id's hp using sharedpref


         /*   val iterator = zombies.iterator()
            while (iterator.hasNext()) {
                val zombie2 = iterator.next()
                if (zombie2 == chosenZombie) {*/
                    //val zombieObject = Zombie(zombie2)


                    val zombieId = closestZombie.tag
            println("Zombie ID is $zombieId")

                    var hp = sharedPreferences.getInt(zombieId.toString(), 6)

            if(hp <= 0){
                return
            }

                    if ( currentItem == "hands"){
                        hp--
                        sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()


                    }

                    if (currentItem == "bat") {
                        hp -=2

                        sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()
                        System.out.println("THIS:   " + hp)

                    } else if (currentItem == "tomahawk") {
                        hp -= 4
                        sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()

                    } else if (currentItem == "machete") {
                        hp -= 4
                        sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()

                    }

                    hp = sharedPreferences.getInt(zombieId.toString(), 6)


                    if (hp <= 0) {
                        experience++
                        saveExperience()
                        followers--
                        if (followers == 0) {
                            binding.followers.text = "No zombies here"
                        } else {
                            binding.followers.text = "You have $followers followers"
                        }
                        roll1 = getRandomNumber()
                        droppedItem(
                            xtap,
                            ytap,
                            roll1,
                            closestZombie
                        )
                        closestZombie.setOnClickListener(null)
                        //rotate zombie then on animation finish remove zombie
                        closestZombie.animate().rotation(90.toFloat()).setDuration(400)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {



                                    sharedPreferences.edit().putInt(zombieId.toString(), 6).commit()

                                    zombies.remove(closestZombie)
                                    binding.parentLayout.removeView(closestZombie) // Remove from zombieLayout

                                    //iterator.remove()



                                    // val roll = (1..itemRarity).random() // Generate roll here
                                     // Pass roll to droppedItem
                                    // itemDropped = true


                                    // zombies.remove(zombie2)

                                    saveZombies()
                                }
                            })
                    }
                }
            }


    fun calculateDistance(xtap: Float, ytap: Float, zombie: ImageView): Double {
        val zombieCenterX = zombie.x + zombie.width / 2
        val zombieCenterY = zombie.y + zombie.height / 2

        return Math.sqrt(
            Math.pow((xtap - zombieCenterX).toDouble(), 2.0) +
                    Math.pow((ytap - zombieCenterY).toDouble(), 2.0)
        )
    }
/*
        // handler.postDelayed({
        itemDropped = false
        // }, 500)*/




    private fun droppedItem(xtap: Float, ytap: Float, roll: Int, zombie1: ImageView) {

        var droppedItem1 = "noItem"
    /*    if(isDroppingItem){
            return
        }
        isDroppingItem = true*/
        // var roll = (1..20).random()
        if(sharedPreferences.getString(zombie1.tag.toString()+ "item", "nothing") == "shield") {
            if (roll % 2 == 0) {
                droppedItem1 = "shield"
                sharedPreferences.edit().putString(zombie1.tag.toString() + "item", "nothing")
                    .commit()
            }
        }

        else {

            if (roll == 1) {
                droppedItem1 = "molotov"
            }
            if (roll == 2) {
                droppedItem1 = "tomahawk"
            }
            if (roll == 3) {
                droppedItem1 = "crossbow"
            }
            if (roll == 4) {
                droppedItem1 = "machete"
            }
            if (roll == 5) {
                droppedItem1 = "bat"
            }
            if (roll == 6) {
                droppedItem1 = "medicine"
            }
        }



        if (droppedItem1 != "noItem") {

            if(zombie1 != latestZombie) {

                createDroppedItem(xtap, ytap, droppedItem1)
                latestZombie = zombie1
            }

        }
        return
        // Reset the flag after a delay (e.g., 500 milliseconds)
   /*     handler.postDelayed({
            isDroppingItem = false
        }, 1000)*/

    }

    private fun createDroppedItem(xtap: Float, ytap: Float, droppedItem1: String) {
        val droppedItem = ImageView(this@MainActivity2)
        if(droppedItem1 == "molotov"){
            droppedItem.setImageResource(
                resources.getIdentifier(droppedItem1 + "1", "drawable", packageName)
            )
        }
        else {
            droppedItem.setImageResource(
                resources.getIdentifier(droppedItem1, "drawable", packageName)
            )
        }
        droppedItem.background = null
        droppedItem.visibility = View.INVISIBLE
        binding.parentLayout.addView(droppedItem)
        droppedItem.layoutParams.width = 100
        droppedItem.layoutParams.height = 100

        droppedItem.bringToFront()
        // Use post() to get the correct width and height
        droppedItem.post {
            // Now we know the width and height
            droppedItem.x = xtap - droppedItem.width / 2
            droppedItem.y = ytap - droppedItem.height / 2
            droppedItem.visibility = View.VISIBLE

            droppedItem.setOnClickListener(View.OnClickListener {
                droppedItem.setOnClickListener(null)
                loadItems()
               if (!items.contains(droppedItem1)) {

                   if(droppedItem1 == "medicine") {
                       health += 5

                       if(shieldIsOn){
                           sharedPreferences.getInt("shieldHealth", shieldHealth)
                           binding.health.text = "Health: $health Shield: $shieldHealth"
                       }
                       else {
                           binding.health.text = "Health: $health"
                       }

                       sharedPreferences.edit().putInt("health", health).commit()
                   }
                   else if(droppedItem1 == "shield"){
                       shieldHealth += 25
                       sharedPreferences.edit().putInt("shieldHealth", shieldHealth).commit()
                       shieldIsOn = true
                       updateShield()
                       binding.health.text = "Health: $health Shield: $shieldHealth"

                   }
                   else {
                       items.add(droppedItem1)


/*
                       saveItems()
                       loadItems()*/
                   }

                }
                if (droppedItem1 == "molotov") {
                    molotovAmmo += 10
                }
                if (droppedItem1 == "tomahawk") {
                    tomahawkAmmo += 50
                }
                if (droppedItem1 == "crossbow") {
                    crossbowAmmo += 20
                }
                if (droppedItem1 == "machete") {
                    macheteAmmo += 15
                }
                if (droppedItem1 == "bat") {
                    batAmmo += 25
                }
              /*  if (droppedItem1 == "shield"){
                    shieldHealth += 25
                }*/




                //update value
                setUses()

                saveItems()
                loadItems()
                binding.parentLayout.removeView(droppedItem)


            })

            lifecycleScope.launch {
                delay(6000)
                binding.parentLayout.removeView(droppedItem)
            }
        }

    }


    private fun isTapped(image: ImageView, xtap: Float, ytap: Float): Boolean {
        val imageRect = Rect()
        image.getGlobalVisibleRect(imageRect)
        return imageRect.contains(xtap.toInt(), ytap.toInt())


    }

    private fun closestZombie1(xtap: Float, ytap: Float, clickZoneZombies: ArrayList<ImageView>): ImageView? {

        var closestZombie: ImageView? = null
        var closestDistance = Float.MAX_VALUE
        var closestZombieX = 0F
        var closestZombieY = 0F

        for (zombie in zombies) {
            if (clickZoneZombies.contains(zombie)) {
                continue
            }
            val middleOfZombieX = zombie.x + zombie.width / 2
            val middleOfZombieY = zombie.y + zombie.height / 2
            val distance = Math.sqrt(
                Math.pow(
                    (middleOfZombieX - xtap).toDouble(),
                    2.0
                ) + Math.pow((middleOfZombieY - ytap).toDouble(), 2.0)
            ).toFloat()
            if (distance < closestDistance) {
                closestDistance = distance
                closestZombie = zombie
                closestZombieX = middleOfZombieX
                closestZombieY = middleOfZombieY
            }
        }

        closestZombie?.let { clickZoneZombies.add(it) }

        clickZoneZombies.removeAll(zombies)
        return closestZombie
    }

    private fun closestZombie(xtap: Float, ytap: Float): ImageView? {

        var closestZombie: ImageView? = null
        var closestDistance = Float.MAX_VALUE
        var closestZombieX = 0F
        var closestZombieY = 0F




        for (zombie in zombies) {
            println("HERE: " + zombie.tag)
           /* if (targetedZombies.contains(zombie)) {
                continue
            }*/
            val middleOfZombieX = zombie.x + zombie.width / 2
            val middleOfZombieY = zombie.y + zombie.height / 2
            val distance = Math.sqrt(
                Math.pow(
                    (middleOfZombieX - xtap).toDouble(),
                    2.0
                ) + Math.pow((middleOfZombieY - ytap).toDouble(), 2.0)
            ).toFloat()
            if (distance < closestDistance) {
                closestDistance = distance
                closestZombie = zombie
                closestZombieX = middleOfZombieX
                closestZombieY = middleOfZombieY
            }
        }

        /*closestZombie?.let { targetedZombies.add(it) }*/

        return closestZombie
    }


    private fun blastRadius(xtap: Float, ytap: Float, radius: Float) {
        //use while loop to iterate without supplying the total size of zombies
        val iterator = zombies.iterator()
        while (iterator.hasNext()) {
            val zombie1 = iterator.next()
            val middleOfZombieX = zombie1.x + zombie1.width / 2
            val middleOfZombieY = zombie1.y + zombie1.height / 2
            val distance = Math.sqrt(
                Math.pow(
                    (middleOfZombieX - xtap).toDouble(),
                    2.0
                ) + Math.pow((middleOfZombieY - ytap).toDouble(), 2.0)
            ).toFloat()

            if (distance < radius-100) {

                iterator.remove()
                followers--
                if(followers == 0){
                    binding.followers.text = "No zombies here"
                }
                else {
                    binding.followers.text = "You have $followers followers"
                }
                binding.parentLayout.removeView(zombie1) // Remove from zombieLayout
                zombies.remove(zombie1)
                experience++
                saveExperience()
                saveZombies()
            }
        }

    }

    private fun loadExperience() {
        experience = sharedPreferences.getInt("experience", 0)
        binding.experience.text = "Experience: $experience"

    }

    private fun saveExperience() {
        binding.experience.text = "Experience: $experience"
        //val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("experience", experience).apply()

    }


    private fun loadItems() {



        batAmmo = sharedPreferences.getInt("batAmmo", batAmmo)
        molotovAmmo = sharedPreferences.getInt("molotovAmmo", molotovAmmo)
        tomahawkAmmo = sharedPreferences.getInt("tomahawkAmmo", tomahawkAmmo)
        macheteAmmo = sharedPreferences.getInt("macheteAmmo", macheteAmmo)
        crossbowAmmo = sharedPreferences.getInt("crossbowAmmo", crossbowAmmo)
        //shieldHealth = sharedPreferences.getInt("shieldHealth", shieldHealth)


        if(currentItem == null){
            binding.uses.text = "Choose an item"

        }

        // val sharedPreferences: SharedPreferences = getSharedPreferences("items", Context.MODE_PRIVATE)


        //load items from shared preferences
        /*     val gson = Gson()
              val json = sharedPreferences.getString("items", null)
             val type = object : TypeToken<ArrayList<String>>() {}.type
              if (json != null) {
                  items = gson.fromJson(json, type) ?: arrayListOf()
              }*/
        val gson = Gson()
        val json = sharedPreferences.getString("items", null)
        val type = object : TypeToken<ArrayList<String>>() {}.type
        items =
            try {  //if the json is not a valid representation of an ArrayList<String>, then the fromJson method will throw an exception
                if (json != null) {
                    gson.fromJson(json, type) ?: arrayListOf()
                } else {
                    arrayListOf()
                }
            } catch (e: JsonSyntaxException) {
                Log.e("loadItems", "Error parsing JSON: ${e.message}")
                arrayListOf() // Return an empty list in case of error
            }

        if(!items.contains("hands")){
            items.add("hands")
            saveItems()

        }

        binding.buttonlayout.removeAllViewsInLayout()

        //sets the currentItem as the last item in the items array

        /*
         //load current item from shared preferences
         currentItem = sharedPreferences.getString("CI", null)
          if (currentItem == null) {
              if (items.size != 0){
                  currentItem = items[items.size-1]
              }
          }*/
        numberOfButtons = items.size
        runOnUiThread {
            val buttons = arrayOfNulls<Button>(numberOfButtons)

            for (i in 0 until numberOfButtons) {
                val button = AppCompatButton(this)

                button.text = items[i]

                binding.buttonlayout.addView(button)

                button.setOnClickListener {

                    currentItem = button.text.toString()
                    setUses()
                    sharedPreferences.edit().putString("CI", currentItem).commit()
                }
                buttons[i] = button
            }
        }
        lifecycleScope.launch {
            delay(5000)


            if (gameOver == true) {
                binding.buttonlayout.visibility = View.GONE
            } else {
                binding.buttonlayout.visibility = View.VISIBLE
            }
        }
    }

    private fun setUses() {

        if(currentItem == "molotov"){
            currentItemAmmo = molotovAmmo
            binding.uses.text = "Uses: $currentItemAmmo"
        }
        if(currentItem == "machete"){
            currentItemAmmo = macheteAmmo
            binding.uses.text = "Uses: $currentItemAmmo"
        }
        if(currentItem == "bat"){
            currentItemAmmo = batAmmo
            binding.uses.text = "Uses: $currentItemAmmo"
        }
        if(currentItem == "tomahawk"){
            currentItemAmmo = tomahawkAmmo
            binding.uses.text = "Uses: $currentItemAmmo"
        }
        if(currentItem == "crossbow"){
            currentItemAmmo = crossbowAmmo
            binding.uses.text = "Uses: $currentItemAmmo"
        }
        if(currentItem == "hands"){
            binding.uses.text = "Uses: ∞"
        }

    }


    private fun loadOneZombie() {


        val zombie = ImageView(this@MainActivity2)

        zombie.post {
            // Now we know the width and height
            zombie.x = binding.mainLayout.width.toFloat() / 2
            zombie.y = -350F


            // Remove any existing background (not really needed for ImageView, but good practice)
            zombie.background = null
            val zombie7 = createZombie(zombie)
            zombie.setTag(zombie7.newId)

            // zombieMap[zombie7.imageView] = zombie7
        // val zombieId = zombie7.newId
        //give zombie health 3
        // val zombieId
            val editor = sharedPreferences.edit()
            //set hp
            zombie.visibility = View.INVISIBLE
            zombieCounter++
            if (zombieCounter % 11 == 0) {
                lifecycleScope.launch(Dispatchers.IO) {
                    loadGifIntoImageView(
                        zombie,
                        R.drawable.shirtzombie4,
                        200,
                        200
                    ) // Example: Resize to 200x200 pixels
                }
                editor.putInt(zombie7.newId.toString(), 12).commit()
                editor.putString(zombie7.newId.toString() + "item", "nothing").apply()



            }
            else if (zombieCounter % 15 ==0) {
                randzombie = getRandomZombie(2)
                if (randzombie == 1) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        loadGifIntoImageView(
                            zombie,
                            R.drawable.suitzombie2,
                            200,
                            200
                        ) // Example: Resize to 200x200 pixels
                    }

                } else if (randzombie == 2) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        loadGifIntoImageView(
                            zombie,
                            R.drawable.staggardzombie,
                            200,
                            200
                        ) // Example: Resize to 200x200 pixels
                    }

                }
                editor.putInt(zombie7.newId.toString(), 18).commit()
                editor.putString(zombie7.newId.toString() + "item", "nothing").apply()




            }


            else if(zombieCounter % 20 == 0) {
                lifecycleScope.launch(Dispatchers.IO) {
                    loadGifIntoImageView(
                        zombie,
                        R.drawable.riotgear,
                        200,
                        200
                    ) // Example: Resize to 200x200 pixels
                }
                editor.putInt(zombie7.newId.toString(), 24).commit()
                editor.putString(zombie7.newId.toString() + "item", "shield").apply()


            }

            else {


                randzombie = getRandomZombie(3)
                if (randzombie == 1) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        loadGifIntoImageView(
                            zombie,
                            R.drawable.shirtzombie1,
                            200,
                            200
                        ) // Example: Resize to 200x200 pixels
                    }

                } else if (randzombie == 2) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        loadGifIntoImageView(
                            zombie,
                            R.drawable.shirtzombiestaggard,
                            200,
                            200
                        ) // Example: Resize to 200x200 pixels
                    }

                }
                else if (randzombie == 3) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        loadGifIntoImageView(
                            zombie,
                            R.drawable.staggard3,
                            200,
                            200
                        ) // Example: Resize to 200x200 pixels
                    }

                }
                editor.putInt(zombie7.newId.toString(), 6).commit()
                editor.putString(zombie7.newId.toString() + "item", "nothing").commit()

            }
        }

        binding.parentLayout.addView(zombie)
        zombies.add(zombie)

        shield?.bringToFront()


        zombie.postDelayed({
            zombie.visibility = View.VISIBLE

            animateZombie(zombie)
        }, 100)
        saveZombies()
    }


    /*
        }*/

    private fun animateZombie(zombie: ImageView) {


        var footStepsX = binding.mainLayout.width / 2 - 100
        var footStepsY = binding.mainLayout.height - binding.buttonlayout.height - 100

        val footStepsRect = Rect(
            footStepsX, footStepsY, footStepsX + 200, footStepsY + 200
        )

        val shieldArea = Rect(
            0, footStepsY - 100, binding.mainLayout.width, footStepsY + 300
        )

        var shieldX = binding.mainLayout.width / 2 - 100
        var shieldY = binding.mainLayout.height - binding.buttonlayout.height - 100 - 200

        val shieldRect = Rect(
            shieldX, shieldY, shieldX + 200, shieldY + 200
        )


        var width = Random.nextInt(0, binding.parentLayout.width - 200)
        var height = Random.nextInt(0 + 260, binding.parentLayout.height - 200)
        if (shieldIsOn) {

            if (shieldArea.contains(width + 100, height + 100)) {
                width = Random.nextInt(0, binding.parentLayout.width - 200)
                height = Random.nextInt(0 + 260, binding.parentLayout.height - 200 - 200 - 200)
            }
        }
        zombie.animate().x(width.toFloat()).y(height.toFloat()).setDuration(5000)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {

                    //width+100 and height +100 represent the middle of the zombie
                    if (shieldRect.contains(width + 100, height + 100) && shieldIsOn) {
                        shieldHealth = sharedPreferences.getInt("shieldHealth", shieldHealth)
                        shieldHealth--
                        binding.health.text = "Health: $health Shield: $shieldHealth"
                        sharedPreferences.edit().putInt("shieldHealth", shieldHealth).commit()
                        if (shieldHealth <= 0) {
                            shieldIsOn = false
                            updateShield()
                        }
                    }


                    // if width and height are within footstep rectangle


                    if (footStepsRect.contains(
                            width + 100,
                            height + 100
                        ) && zombies.size > 0 && !shieldIsOn
                    ) {

                        //if zombie touches footsteps remove one health point


                        health--

                        binding.health.text = "Health: $health"
                        sharedPreferences.edit().putInt("health", health).commit()

                        if (health < 1) {

                          /*  var highestStepCount = sharedPreferences.getInt("lastStepCount", lastDailySteps)
                            if(highestStepCount > 0){
                                binding.inAppSteps.text = "In App Steps: $highestStepCount"
                                var pauseSensor = true
                                sharedPreferences.edit().putBoolean("pauseSensor", pauseSensor).commit()
                               // sharedPreferences.edit().putBoolean("", true).commit()
                            }*/
                            var pauseSensor = true
                            sharedPreferences.edit().putBoolean("pauseSensor", pauseSensor).commit()

                            gameOver = true
                            sharedPreferences.edit().putBoolean("gameOver", gameOver).commit()
                            health = 0
                            sharedPreferences.edit().putInt("health", health).commit()



                            currentItem = "nothing"
                            binding.health.text = "Game Over"



                            noFootSteps = true
                            loadFootSteps()
                           // items.removeAll(items)
                            binding.buttonlayout.visibility = View.GONE

                            saveItems()
                            loadItems()

                            /*gameOver = true
                                loadFootSteps()*/

                            //binding.parentLayout.removeView(footSteps)


                            //load an ad on gameover

                            if(oneAd) {

                                val adRequest = AdRequest.Builder().build()
                                RewardedAd.load(this@MainActivity2,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
                                    override fun onAdFailedToLoad(adError: LoadAdError) {
                                        Log.d(TAG, adError.toString())
                                        rewardedAd = null
                                    }

                                    override fun onAdLoaded(ad: RewardedAd) {
                                        Log.d(TAG, "Ad was loaded.")
                                        rewardedAd = ad
                                    }
                                })

                                val adRequest2 = AdRequest.Builder().build()

                                InterstitialAd.load(
                                    this@MainActivity2,
                                    "ca-app-pub-3940256099942544/1033173712",
                                    adRequest2,
                                    object : InterstitialAdLoadCallback() {
                                        override fun onAdFailedToLoad(adError: LoadAdError) {
                                            Log.d(TAG, adError.toString())
                                            Log.d(
                                                TAG,
                                                "Ad failed to load: ${adError.message}"
                                            ) // Print the error message
                                            Log.d(
                                                TAG,
                                                "Ad failed to load: ${adError.code}"
                                            ) // Print the error code
                                            mInterstitialAd = null
                                        }

                                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                            Log.d(TAG, "Ad was loaded.")
                                            mInterstitialAd = interstitialAd
                                            mInterstitialAd?.show(this@MainActivity2)
                                            binding.respawn.visibility = View.VISIBLE
                                            binding.respawnWithItems.visibility = View.VISIBLE
                                        }
                                    })

                                mInterstitialAd?.fullScreenContentCallback =
                                    object : FullScreenContentCallback() {
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
                                            Log.e(
                                                TAG,
                                                "Ad failed to show: ${adError.message}"
                                            ) // Print the error message
                                            Log.e(
                                                TAG,
                                                "Ad failed to show: ${adError.code}"
                                            ) // Print the error code
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
                                    mInterstitialAd?.show(this@MainActivity2)
                                } else {
                                    Log.d("TAG", "The interstitial ad wasn't ready yet.")


                                }

                                oneAd = false


                            }





                        }
                    }


                    // Animation has finished!
                    Log.d("Animation", "Animation has finished for zombie")
                    // Perform your action here
                    // For example, you could start another animation, update the UI, etc.
                    animateZombie(zombie)
                }
            })


    }

    private fun angleAnimation(
        item2: ImageView, closestZombieX: Float?, closestZombieY: Float?, closestZombie1: ImageView?
    ) {
       // var hp1 = 0
        if (zombies.size == 0) {
            binding.parentLayout.removeView(item2)
        } else {

            if (closestZombieX == null || closestZombieY == null) {
                binding.parentLayout.removeView(item2)
                return
            }


            // Get the crossbow's center coordinates
            val crossbowX = item2.x + item2.width / 2
            val crossbowY = item2.y + item2.height / 2

            var calculatedAngle = kotlin.math.atan2(
                closestZombieY - crossbowY,
                closestZombieX - crossbowX
            ) * 180 / Math.PI
            calculatedAngle += 90f



            item2.animate().rotation(calculatedAngle.toFloat()).setDuration(400)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Animation has finished!
                        Log.d("Animation", "Animation has finished for zombie")
                        // Perform your action here
                        // For example, you could start another animation, update the UI, etc.
                        binding.parentLayout.removeView(item2)

                    /*    followers--
                        if (followers == 0) {
                            binding.followers.text = "No zombies here"
                        } else {

                            binding.followers.text = "You have $followers followers"
                        }*/

                        if (closestZombie1 != null) {
                           var hp1 = sharedPreferences.getInt(closestZombie1.tag.toString(), 0)
                            println(hp1)


                           // var hp = sharedPreferences.getInt(zombieId.toString(), 6)

                            if(hp1 <= 0){
                                return
                            }
                            hp1 -= 10
                            sharedPreferences.edit().putInt(closestZombie1.tag.toString(), hp1)
                                .commit()


                            println(hp1)





                            if (hp1 <= 0 && closestZombie1 != lastZombie) {
                                experience++
                                saveExperience()

                                followers--
                                if (followers <= 0) {
                                    binding.followers.text = "No zombies here"
                                } else {
                                    binding.followers.text =
                                        "You have $followers followers"
                                }
                                roll1 = getRandomNumber()
                                droppedItem(
                                    closestZombieX,
                                    closestZombieY,
                                    roll1,
                                    closestZombie1
                                )

                                //sharedPreferences.edit().putInt(closestZombie1.getTag().toString(), 6).commit()
                                closestZombie1.setOnClickListener(null)

                                closestZombie1.animate().rotation(90.toFloat()).setDuration(400)
                                    .setListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {

                                            lastZombie = closestZombie1
                                            zombies.remove(closestZombie1)

                                            //iterator.remove()

                                            saveZombies()
                                            loadZombies()


                                            binding.parentLayout.removeView(closestZombie1) // Remove from zombieLayout


                                            // val roll = (1..itemRarity).random() // Generate roll here
                                             // Pass roll to droppedItem
                                            // itemDropped = true


                                            // zombies.remove(zombie2)

                                            saveZombies()
                                        }
                                    })
                            }

                            /*binding.parentLayout.removeView(closestZombie1) // Remove from zombieLayout
                        zombies.remove(closestZombie1)
                        //var lastZombie = closestZombie1
                       // if (!itemDropped) {
                        // (1..itemRarity).random()

                            roll1 = getRandomNumber() // Generate roll here
                            droppedItem(closestZombieX, closestZombieY, roll1)
                           // itemDropped = true
                       // }

                        experience++
                        saveExperience()
                        saveZombies()*/

                        }
                    }
                })


        }
   /*     itemDropped = false*/
    }
    private fun getRandomNumber(): Int {
        return random.nextInt(1, itemRarity)
    }
    private fun getRandomZombie(i: Int): Int {
        return random.nextInt(1,i+1)
    }


    private fun swipeAnimation(item1: ImageView) {
        //var width = Random.nextInt(0, binding.parentLayout.width-200)
        // var height = Random.nextInt(0+260, binding.parentLayout.height-200)
        // machete.animate().x(width.toFloat()).y(height.toFloat()).setDuration(4000)
        item1.animate().rotation(-90f).setDuration(500)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Animation has finished!
                    Log.d("Animation", "Animation has finished for zombie")
                    // Perform your action here
                    // For example, you could start another animation, update the UI, etc.


                    binding.parentLayout.removeView(item1)
                }
            })
    }

    private fun loadGifIntoImageView(imageView: ImageView, gifResId: Int, width: Int, height: Int) {

        var retryCount = 0
        val maxRetries = 3 // Maximum number of retries

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                Glide.with(imageView.context)
                    .asGif()
                    .load(gifResId)
                    .override(width, height)
                    .listener(object :
                        RequestListener<com.bumptech.glide.load.resource.gif.GifDrawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<com.bumptech.glide.load.resource.gif.GifDrawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("Glide", "Error loading GIF", e)
                            // Load a static image as a placeholder
                            if (retryCount < maxRetries) {
                                retryCount++
                                reinitializeLayout()
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: com.bumptech.glide.load.resource.gif.GifDrawable,
                            model: Any,
                            target: Target<com.bumptech.glide.load.resource.gif.GifDrawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            // No need to do anything here, Glide has already loaded the GIF into the ImageView
                            return false // Return false to let Glide handle the display
                        }
                    })
                    .into(imageView)
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == StepCounterService.PREF_DAILY_STEPS) {
            val dailySteps = sharedPreferences?.getInt(StepCounterService.PREF_DAILY_STEPS, 0)
            //binding.inAppSteps.text = "In App Steps: $dailySteps"
            if (dailySteps != null) {
                dailyStepsChangeListener.onDailyStepsChanged(dailySteps)
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
                onActivityRecognitionPermissionGranted()
            }
        } else {
            // No need to request permission for versions lower than Android 10
            // onActivityRecognitionPermissionGranted()
        }
    }


    //Activity permission
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

                /*    intent = Intent(this, MainActivity2::class.java)
                    startActivity(intent)*/
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
                     /*   lifecycleScope.launch {
                            getChangesToken()
                           // startListeningForChanges()
                        }*/
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


    private fun startStepCounterService() {
        val serviceIntent = Intent(this, StepCounterService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }


    private suspend fun checkPermissionsAndRun() {
        /*   if(!useHealthConnect){
           if(isFirstRun){
               isFirstRun = false
               with(sharedPreferences.edit()) {
                   putBoolean(PREF_IS_FIRST_RUN, isFirstRun)
                   apply()
               }
           }
               return
               }*/
        if(permissionController != null) {

            val timeoutMillis = 3000L // 5 seconds timeout (adjust as needed)
            val maxRetries = 1
            var retryCount = 0
            while (retryCount < maxRetries) {
                val grantedResult = withTimeoutOrNull(timeoutMillis) {
                    healthConnectClient!!.permissionController.getGrantedPermissions()
                }



                /*val grantedResult = withTimeoutOrNull(timeoutMillis) {
            healthConnectClient.permissionController.getGrantedPermissions()
        }*/

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


                /*    if(isFirstRun) {
            intent =         Log.w(TAG, "Timeout occurred while getting granted permissions. Retry attempt: ${retryCount + 1}")
            retryCount++
            delay(2000L) // Wait for 2 seconds before retryingIntent(this, MainActivity2::class.java)
            startActivity(intent)
        }*/

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
    suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMillis: Long = 1000,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: HealthConnectException) {
                if (e.errorCode == HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED) {
                    if (attempt == maxRetries - 1) {
                        throw e // Re-throw if max retries reached
                    }
                    Toast.makeText(this,"API call quota exceeded. Retrying in ${currentDelay}ms (attempt ${attempt + 1})", Toast.LENGTH_SHORT).show()
                    delay(currentDelay)
                    currentDelay *= 2 // Exponential backoff
                } else {
                    throw e // Re-throw other exceptions
                }
            }
        }
        throw IllegalStateException("Should not reach here")




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

    /*   override fun onBackPressed() {
           saveZombies()
           super.onBackPressed()
       }*/







    override fun onResume() {
        super.onResume()
        //dismissedPermissions = sharedPreferences.getBoolean("dismissedPermissions", false)
       // activityPermissionGranted = sharedPreferences.getBoolean("activityPermissionGranted", false)
        /*if(dismissedPermissions){
            binding.inAppStepsPermission.visibility = View.INVISIBLE
            binding.healthConnectPermissions.visibility = View.INVISIBLE
            binding.dismissButton.visibility = View.INVISIBLE
        }*/
    /*    if(!permissionsGranted){
            binding.healthConnectPermissions.visibility = View.INVISIBLE
        }*/

        checkAndRequestPermissions()

        lifecycleScope.launch(Dispatchers.IO){
            val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
            val endTime = Instant.now()
            healthConnectClient?.let { readStepsByTimeRange(it, startTime, endTime) }
        }


    }
/*




    override fun onRestart(){
        super.onRestart()

       reinitializeLayout()
    }*/



    override fun onPause() {
        super.onPause()
        //val dailySteps = sharedPreferences.getInt(StepCounterService.PREF_DAILY_STEPS, 0)
        //sharedPreferences.edit().putInt("onPause", lastDailySteps).commit()
       // Log.d("MainActivity2", "Saved dailySteps to SharedPreferences: $dailySteps")
    }

    private fun reinitializeLayout() {
        Log.d(TAG, "reinitializeLayout called")
        // Clear the parent layout
       // binding.textLayout.removeAllViews()
        binding.zombieLayout.removeAllViews()
        //binding.parentLayout.removeAllViews()



        val newZombieLayout = FrameLayout(this)
        newZombieLayout.id = R.id.zombieLayout

        binding.parentLayout.addView(newZombieLayout)
       // binding.zombieLayout = newZombieLayout

        // Re-inflate the layout
   /*    val newBinding = ActivityMain2Binding.inflate(layoutInflater)
        binding.zombieLayout.addView(newBinding.root)*/


/*
        val dailySteps = sharedPreferences.getInt(StepCounterService.PREF_DAILY_STEPS, 0)
        binding.inAppSteps.text = "In App Steps : $dailySteps"
        binding.followers.text = "You have $followers followers"


        lifecycleScope.launch(Dispatchers.IO){
            val startTime = Instant.now().minusSeconds(60 * 60 * 24) // 24 hours ago
            val endTime = Instant.now()
            readStepsByTimeRange(healthConnectClient, startTime, endTime)
        }
*/

        // Update the binding reference
      //  binding = newBinding


        // Reload images
       loadZombies()
        // Reload data
        loadItems()
        loadExperience()

    }


}





