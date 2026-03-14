package com.zombiewalk

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.HealthConnectException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

/*

        val dialog1 = AlertDialog.Builder(this).setTitle("Watch Your Step")
            .setMessage("I am requesting Health Connect read steps permission to access your StepsCadence/Steps in order to promote good health. \nThe Game is fictitious and any similarities are purely coincidental. \nIf the permission is denied \nthe game is over.")
            .setPositiveButton("Understood") { dialog, which ->
                dialog.dismiss()

                Log.d("lily", "IT WORKED")

                dialog.cancel()

            }
            .show()
*/



     //   dialog1.setOnDismissListener {



        val zPack = findViewById<ImageView>(R.id.zPack)

        zPack.setOnClickListener {


            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)

        }
        }
   // }

}

