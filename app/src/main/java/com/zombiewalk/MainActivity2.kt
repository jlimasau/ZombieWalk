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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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

//bug: on phone restart step count was -37222 FIXED
    //animation that goes from tap to zombie that was removed coordinates representing the arrow


    //items usage should save across sessions

    //test for followers appearing at 0 steps, could have been from previous day, in app steps not zero, and items preloaded

    //a chancethat a zombie drops an item after each removal


    //fix one zombie and all items on start

    //items not loading on screen when added on start up

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
    var timesBatUsed = 0
    var timesMolotovUsed = 0
    var timesTomahawkUsed = 0
    var timesCrossbowUsed = 0
    var timesMacheteUsed = 0

    var droppedItem1 = "noItem"

    var testerMode = false

    private val targetedZombies = mutableSetOf<ImageView>()

    private var isDroppingItem = false

    private var itemDropped = false
    //private val handler = Handler(Looper.getMainLooper())
    var crossbowInUse = false
    var itemRarity = 30



    interface DailyStepsChangeListener {
        fun onDailyStepsChanged(dailySteps: Int): Boolean
    }

    private val dailyStepsChangeListener = object : DailyStepsChangeListener {
        override fun onDailyStepsChanged(dailySteps: Int): Boolean {
            binding.inAppSteps.text = "In App Steps: $dailySteps"
            loadItems()
            loadZombies()

            //the number of steps to trigger items
            //rarity
            if (dailySteps % 20 == 0 && !items.contains("bat")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("bat")
                    saveItems()

                }
            }
            if (dailySteps % 45 == 0 && !items.contains("tomahawk")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("tomahawk")
                    saveItems()

                }
            }
            if (dailySteps % 90 == 0 && !items.contains("molotov")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("molotov")
                    saveItems()

                }
            }
            if (dailySteps % 60 == 0 && !items.contains("crossbow")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("crossbow")
                    saveItems()

                }
            }

            if (dailySteps % 30 == 0 && !items.contains("machete")) {
                var roll = (1..2).random()
                if (roll == 1) {
                    items.add("machete")
                    saveItems()

                }
            }
            if (dailySteps % 50 == 0) {
                val adRequest = AdRequest.Builder().build()

                InterstitialAd.load(this@MainActivity2,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
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
                    mInterstitialAd?.show(this@MainActivity2)
                } else {
                    Log.d("TAG", "The interstitial ad wasn't ready yet.")
                }
            } //loads ad every x steps






            runOnUiThread{
                loadItems()
            }



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

        if( testerMode == true) {
            for (i in 1 .. 10) {
                followers++
                loadOneZombie()
            }
        }

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
                "molotov" -> {
                    val radius = 500F
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
                        molotov.x = xtap - molotov.width/2
                        molotov.y = ytap - molotov.height/2
                        molotov.visibility = View.VISIBLE



                    }
                    molotov.bringToFront()
                    lifecycleScope.launch(Dispatchers.IO) {
                        loadGifIntoImageView(molotov, R.drawable.molotov, 500, 500) // Example: Resize to 200x200 pixels
                    }

                    // Use coroutines to remove the view after 1 second
                    lifecycleScope.launch {
                        delay(1000) // Wait for 1 second
                        binding.parentLayout.removeView(molotov)
                    }


                    if(timesMolotovUsed > 10){
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
                        Bat.x = xtap - Bat.width+50
                        Bat.y = ytap - Bat.height+125
                        Bat.visibility = View.VISIBLE

                        Bat.postDelayed({
                            swipeAnimation(Bat)
                        }, 100)
                    }


                    if(timesBatUsed > 20){
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
                        tomahawk.x = xtap - tomahawk.width+50
                        tomahawk.y = ytap - tomahawk.height+125
                        tomahawk.visibility = View.VISIBLE

                        tomahawk.postDelayed({
                            swipeAnimation(tomahawk)
                        }, 100)
                    }

                    if(timesTomahawkUsed > 50){
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


                    if( crossbowInUse){
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
                        crossbow.x = xtap - crossbow.width/2
                        crossbow.y = ytap - crossbow.height/2
                        crossbow.visibility = View.VISIBLE

                        crossbow.postDelayed({
                            angleAnimation(crossbow, closestZombieX, closestZombieY, closestZombie1)
                        }, 100)

                       /* lifecycleScope.launch {
                            delay(600) // Wait for 1 second
                            binding.parentLayout.removeView(crossbow)

                        }*/

                    }

                    if(timesCrossbowUsed > 20){
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
                        machete.x = xtap - machete.width+50
                        machete.y = ytap - machete.height+100
                        machete.visibility = View.VISIBLE

                        machete.postDelayed({
                            swipeAnimation(machete)
                        }, 100)
                    }





                    /* //use this if it's a gif
                      lifecycleScope.launch(Dispatchers.IO) {
                             loadGifIntoImageView(machete, R.drawable.zombie, 200, 200) // Example: Resize to 200x200 pixels
                         }*/

                    if(timesMacheteUsed > 15){



                        items.remove("machete")

                        saveItems()
                        loadItems()
                        //if item runs out{
                        timesMacheteUsed = 0
                        currentItem = null
                        sharedPreferences.edit().putString("CI", currentItem).commit()
                    }
                }
              
            }
            saveZombies()
            loadItems()

        }
        return super.onTouchEvent(event)
    }




    private fun saveItems() {
        val gson = Gson()
        val json = gson.toJson(items)
        sharedPreferences.edit().putString("items", json).commit()
    }


    private fun handleZombieTap(xtap: Float, ytap: Float) {

        val iterator = zombies.iterator()
        while (iterator.hasNext()){
            val zombie2 = iterator.next()
            if(isTapped(zombie2, xtap, ytap)){
                //val zombieObject = Zombie(zombie2)
                val zombieId = returnId(zombie2)
                var hp = sharedPreferences.getInt(zombieId.toString(), 3)

                if(currentItem == "bat"){
                    hp--
                    sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()

                }
                else if(currentItem == "tomahawk"){
                    hp-=2
                    sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()

                }
                else if(currentItem == "machete"){
                    hp-=2
                    sharedPreferences.edit().putInt(zombieId.toString(), hp).commit()

                }



                if (hp <= 0){
                    sharedPreferences.edit().putInt(zombieId.toString(), 3).commit()

                    iterator.remove()
                    followers--
                    binding.followers.text = "You have $followers followers"
                    binding.parentLayout.removeView(zombie2) // Remove from zombieLayout




                    if(!itemDropped){
                        val roll = (1..itemRarity).random() // Generate roll here
                        droppedItem(xtap,ytap, roll) // Pass roll to droppedItem
                        itemDropped = true
                    }



                   // zombies.remove(zombie2)
                    experience++
                    saveExperience()
                    saveZombies()
                }




            }
        }

       // handler.postDelayed({
            itemDropped = false
       // }, 500)


    }

    private fun droppedItem(xtap: Float, ytap: Float, roll: Int) {





       // var roll = (1..20).random()

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


        if (droppedItem1 == "noItem") {

        } else {
            val droppedItem = ImageView(this@MainActivity2)
            droppedItem.setImageResource(resources.getIdentifier(droppedItem1, "drawable", packageName)
            )

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
                    if(droppedItem1 == "noItem"){

                    }
                    else if(!items.contains(droppedItem1)){


                        items.add(droppedItem1!!)
                        saveItems()
                        loadItems()

                    }
                    loadItems()
                    saveItems()
                    binding.parentLayout.removeView(droppedItem)
                    droppedItem1 = "noItem"

                })



                lifecycleScope.launch {
                    delay(5000)
                    binding.parentLayout.removeView(droppedItem)
                }
            }
        }
        // Reset the flag after a delay (e.g., 500 milliseconds)



    }



    private fun isTapped(zombie: ImageView, xtap: Float, ytap: Float): Boolean {
        val zombieRect = Rect()
        zombie.getGlobalVisibleRect(zombieRect)
        return zombieRect.contains(xtap.toInt(), ytap.toInt())


    }
    private fun closestZombie(xtap: Float, ytap: Float): ImageView? {

        var closestZombie: ImageView? = null
        var closestDistance = Float.MAX_VALUE
        var closestZombieX = 0F
        var closestZombieY = 0F

        for (zombie in zombies) {
            if(targetedZombies.contains(zombie)) {
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

        closestZombie?.let{ targetedZombies.add(it)}

        return closestZombie
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
        runOnUiThread {
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
        }
        lifecycleScope.launch {
            delay(5000)


        if (items.isEmpty()) {
            binding.buttonlayout.visibility = View.GONE
        }
        else{
            binding.buttonlayout.visibility = View.VISIBLE
        }
        }
    }


    private fun loadOneZombie(){

        val zombie = ImageView(this@MainActivity2)

        zombie.post {
            // Now we know the width and height
            zombie.x = binding.mainLayout.width.toFloat()/2
            zombie.y = -350F

        }


        // Remove any existing background (not really needed for ImageView, but good practice)
        zombie.background = null
        val zombie7 = createZombie(zombie)

       // zombieMap[zombie7.imageView] = zombie7
       // val zombieId = zombie7.newId
        //give zombie health 3
       // val zombieId
        val editor = sharedPreferences.edit()
        editor.putInt(zombie7.newId.toString(), 3).apply()

        zombie.visibility = View.INVISIBLE



        lifecycleScope.launch(Dispatchers.IO) {
            loadGifIntoImageView(zombie, R.drawable.zombie, 200, 200) // Example: Resize to 200x200 pixels
        }

        binding.parentLayout.addView(zombie)
        zombies.add(zombie)


            zombie.bringToFront()
        zombie.postDelayed({
            zombie.visibility = View.VISIBLE

            animateZombie(zombie)
        }, 100)
        saveZombies()
    }


    /*
        }*/

    private fun animateZombie(zombie: ImageView) {
        var width = Random.nextInt(0, binding.parentLayout.width-200)
        var height = Random.nextInt(0+260, binding.parentLayout.height-200)
        zombie.animate().x(width.toFloat()).y(height.toFloat()).setDuration(5000)
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

    private fun angleAnimation(
        item2: ImageView, closestZombieX: Float?, closestZombieY: Float?, closestZombie1: ImageView?) {
        if(zombies.size == 0){
            binding.parentLayout.removeView(item2)
        }
        else {

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

                        followers--
                        binding.followers.text = "You have $followers followers"
                        binding.parentLayout.removeView(closestZombie1) // Remove from zombieLayout
                        zombies.remove(closestZombie1)
                        if(!itemDropped){
                            val roll = (1..itemRarity).random() // Generate roll here
                            droppedItem(closestZombieX, closestZombieY, roll)
                            itemDropped = true
                        }

                        experience++
                        saveExperience()
                        saveZombies()

                    }
                })


        }
        itemDropped = false
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

 /*   override fun onBackPressed() {
        saveZombies()
        super.onBackPressed()
    }*/


}





