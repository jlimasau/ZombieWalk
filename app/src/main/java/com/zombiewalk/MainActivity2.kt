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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.foundation.background
import androidx.compose.ui.input.key.type
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.contains
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
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.zombiewalk.databinding.ActivityMain2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.random.Random

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

    //check that fresh start up is smooth and user is directed to accept permissions with a reason

    //fix return location initiation
    //write privacy policy
    //rewarded add for cool items
    //add item ammo or use amount
    //add zombie animation when tapped
    //zombies added over time

//bug: on phone restart step count was -37222




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

    private var items: ArrayList<String> = arrayListOf()

    var currentItem: String? = null
    var numberOfButtons = 0

    data class Zombie(val imageView: ImageView, val newId: Int)
    companion object {
        private var nextId = 1 //static counter for sequential id

        fun createZombie(imageView: ImageView): Zombie {
            return Zombie(imageView, nextId++)
        }

    }

    private var zombies: MutableList<ImageView> = mutableListOf()

    private val zombieMap: MutableMap<ImageView, Zombie> = mutableMapOf()

    data class ZombieData(val newId: Int, var hp: Int)
    fun returnId(imageView: ImageView): Int? {
        val zombie = zombieMap[imageView]
        return zombie?.newId // Use safe call to handle potential null
    }

    private var experience = 0
    var timesPressed = 0
    var timesBatUsed = 0
    var timesMolotovUsed = 0
    var timesTomahawkUsed = 0
    var timesCrossbowUsed = 0


    interface DailyStepsChangeListener {
        fun onDailyStepsChanged(dailySteps: Int): Boolean
    }

    private val dailyStepsChangeListener = object : DailyStepsChangeListener {
        override fun onDailyStepsChanged(dailySteps: Int): Boolean {
           /* if(dailySteps < 0){

            }*/
            binding.inAppSteps.text = "In App Steps: $dailySteps"
            loadItems()
            loadZombies()
            //the number of steps to trigger a follower
      /*      if (dailySteps %5 == 0) {
                var roll = (1..2).random()
                if (roll == 1) {
                    followers++
                    loadOneZombie()
                    binding.followers.text = "You have $followers followers"

                }
            }*/
            //the number of steps to trigger items
            if (dailySteps % 3 == 0 && !items.contains("Bat")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("Bat")
                    saveItems()
                    loadItems()


                }
            }
            if (dailySteps % 7 == 0 && !items.contains("Tomahawk")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("Tomahawk")
                    saveItems()
                    loadItems()
                }
            }
            if (dailySteps % 25 == 0 && !items.contains("Molotov")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("Molotov")
                    saveItems()
                    loadItems()
                }
            }
            if (dailySteps % 15 == 0 && !items.contains("Crossbow")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("Crossbow")
                    saveItems()
                    loadItems()
                }
            }
      /*      if (dailySteps % 30 == 0 && !items.contains("Crossbow")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("Crossbow")
                    loadItems()
                }
            }*/
            loadItems()
            saveItems()
            return true // Return true to indicate the change was accepted
        }
    }


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
        binding.followers.text = "You have $followers followers"

        loadItems()
        loadExperience()
        loadZombies()

}
    private fun saveZombies(){
        sharedPreferences.edit().putInt("followerCount", followers).apply()
        //type of zombie count to load other zombies
    }

    private fun loadZombies() {
        followers = sharedPreferences.getInt("followerCount", 0)
        binding.followers.text = "You have $followers followers"

        if(zombies.size < followers){
            for (i in zombies.size until followers) {
                loadOneZombie()

            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event?.action == MotionEvent.ACTION_DOWN) {
            val xtap = event.x
            val ytap = event.y
            when (currentItem ){
                "Molotov" -> {
                    val radius = 500F
                    blastRadius(xtap, ytap, radius)
                    timesMolotovUsed++

                    if(timesMolotovUsed > 10){
                        items.remove("Molotov")


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
                "Bat" -> {
                    handleZombieTap(xtap, ytap)
                    timesBatUsed++
                    if(timesBatUsed > 20){
                        items.remove("Bat")

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
                "Tomahawk" -> {
                    handleZombieTap(xtap, ytap)
                    timesTomahawkUsed++
                    if(timesTomahawkUsed > 50){
                        items.remove("Tomahawk")

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
                
                "Crossbow" -> {
                    removeClosestZombie(xtap, ytap)
                    timesCrossbowUsed++
                    if(timesCrossbowUsed > 20){
                        items.remove("Crossbow")

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
              
            }

        }
        return super.onTouchEvent(event)
    }

    private fun saveItems() {
        val gson = Gson()
        val json = gson.toJson(items)
        sharedPreferences.edit().putString("items", json).apply()
    }


    private fun handleZombieTap(xtap: Float, ytap: Float) {

        val iterator = zombies.iterator()
        while (iterator.hasNext()){
            val zombie2 = iterator.next()
            if(isTapped(zombie2, xtap, ytap)){
                //val zombieObject = Zombie(zombie2)
                val zombieId = returnId(zombie2)
                var hp = sharedPreferences.getInt(zombieId.toString(), 3)

                if(currentItem == "Bat"){
                    hp--
                    sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()

                }
                else if(currentItem == "Tomahawk"){
                    hp-=2
                    sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()

                }
                if (hp <= 0){
                    sharedPreferences.edit().putInt(zombieId.toString(), 3).commit()

                    iterator.remove()
                    followers--
                    binding.followers.text = "You have $followers followers"
                    binding.parentLayout.removeView(zombie2) // Remove from zombieLayout
                   // zombies.remove(zombie2)
                    experience++
                    saveExperience()
                    saveZombies()
                }




            }
        }

    }
    private fun isTapped(zombie: ImageView, xtap: Float, ytap: Float): Boolean {
        val zombieRect = Rect()
        zombie.getGlobalVisibleRect(zombieRect)
        return zombieRect.contains(xtap.toInt(), ytap.toInt())


    }
    private fun removeClosestZombie(xtap: Float, ytap: Float) {

        var closestZombie: ImageView? = null
        var closestDistance = Float.MAX_VALUE

        for (zombie in zombies){
            val middleOfZombieX = zombie.x + zombie.width/2
            val middleOfZombieY = zombie.y + zombie.height/2
            val distance = Math.sqrt(Math.pow((middleOfZombieX - xtap).toDouble(), 2.0) + Math.pow((middleOfZombieY - ytap).toDouble(), 2.0)).toFloat()
            if(distance < closestDistance){
                closestDistance = distance
                closestZombie = zombie
            }
        }
        closestZombie?.let{
            followers--
            binding.followers.text = "You have $followers followers"
            binding.parentLayout.removeView(it) // Remove from zombieLayout
            zombies.remove(it)
            experience++
            saveExperience()
            saveZombies()
        }


    }

    private fun blastRadius(xtap: Float, ytap: Float, radius: Float) {
        //use while loop to iterate without supplying the total size of zombies
        val iterator = zombies.iterator()
        while (iterator.hasNext()){
            val zombie1 = iterator.next()
            val middleOfZombieX = zombie1.x + zombie1.width/2
            val middleOfZombieY = zombie1.y + zombie1.height/2
            val distance = Math.sqrt(Math.pow((middleOfZombieX - xtap).toDouble(), 2.0) + Math.pow((middleOfZombieY - ytap).toDouble(), 2.0)).toFloat()

            if(distance < radius){

                iterator.remove()
                followers--
                binding.followers.text = "You have $followers followers"
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
        items = try {  //if the json is not a valid representation of an ArrayList<String>, then the fromJson method will throw an exception
            if (json != null) {
                gson.fromJson(json, type) ?: arrayListOf()
            } else {
                arrayListOf()
            }
        } catch (e: JsonSyntaxException) {
            Log.e("loadItems", "Error parsing JSON: ${e.message}")
            arrayListOf() // Return an empty list in case of error
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
        val buttons = arrayOfNulls<Button>(numberOfButtons)

        for (i in 0 until numberOfButtons) {
            val button = AppCompatButton(this)

            button.text = items[i]

            binding.buttonlayout.addView(button)

            button.setOnClickListener {
                currentItem = button.text.toString()
                sharedPreferences.edit().putString("CI", currentItem).commit()
            }
            buttons[i] = button
        }
        if (items.isEmpty()) {
            binding.buttonlayout.visibility = View.GONE
        }
    }


    private fun loadOneZombie(){

        val zombie = ImageView(this@MainActivity2)
        zombie.x = binding.mainLayout.width.toFloat()/2
        zombie.y = -350F

        // Remove any existing background (not really needed for ImageView, but good practice)
        zombie.background = null
        val zombie7 = createZombie(zombie)

       // zombieMap[zombie7.imageView] = zombie7
       // val zombieId = zombie7.newId
        //give zombie health 3
       // val zombieId
        val editor = sharedPreferences.edit()
        editor.putInt(zombie7.newId.toString(), 3).apply()




        lifecycleScope.launch(Dispatchers.IO) {
            loadGifIntoImageView(zombie, R.drawable.zombie, 200, 200) // Example: Resize to 200x200 pixels
        }

        binding.parentLayout.addView(zombie)
        zombies.add(zombie)


            zombie.bringToFront()
        zombie.postDelayed({
            animateZombie(zombie)
        }, 100)
        saveZombies()
    }


    /*
        }*/

    private fun animateZombie(zombie: ImageView) {
        var width = Random.nextInt(0, binding.parentLayout.width-200)
        var height = Random.nextInt(0+260, binding.parentLayout.height-200)
        zombie.animate().x(width.toFloat()).y(height.toFloat()).setDuration(4000)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Animation has finished!
                    Log.d("Animation", "Animation has finished for zombie")
                    // Perform your action here
                    // For example, you could start another animation, update the UI, etc.
                    animateZombie(zombie)
                }
            })

    }


    private fun loadGifIntoImageView(imageView: ImageView, gifResId: Int, width: Int, height: Int) {

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


    private suspend fun checkPermissionsAndRun() {
        val timeoutMillis = 5000L // 5 seconds timeout (adjust as needed)

        val grantedResult = withTimeoutOrNull(timeoutMillis) {
            healthConnectClient.permissionController.getGrantedPermissions()
        }

        if (grantedResult == null) {
            println("Timeout: Failed to get granted permissions within $timeoutMillis ms")
            // Handle timeout here (e.g., show an error message)
            binding.stepsTextView.text =
                "Please Open the Health Connect App and Allow Step Writing from your fitness app. Also update your google play services."

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



}





