package com.zombiewalk;


import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;


import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.StepsRecord;

import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.content.Context;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.permission.HealthPermission;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import kotlinx.coroutines.future.FutureKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import java.util.Arrays;
import java.util.HashSet;
import kotlin.reflect.KClass;
import androidx.health.connect.client.records.StepsRecord;




//should ask for location permission on start and update location without opening google maps

public class MainActivity extends AppCompatActivity {


    private final static int REQUEST_CODE = 100;
    FusedLocationProviderClient fusedLocationProviderClient;
    TextView currentcoordinates;
   // private ActivityResultLauncher<Set<String>> requestPermissions;
   // kotlin.reflect.KClass<StepsRecord> kClass = kotlin.jvm.JvmClassMappingKt.getKotlinClass(StepsRecord.class);

/*    private final Set<String> PERMISSIONS = new HashSet<>(
            Arrays.asList(
                    HealthPermission.getReadPermission(kClass)
            )
    );*/




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        new Thread(
                () -> {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    MobileAds.initialize(this, initializationStatus -> {});
                })
                .start();




        currentcoordinates = findViewById(R.id.currentcoordinates);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        getLastLocation();
        Button privacypolicy = findViewById(R.id.PrivacyPolicy);
        privacypolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PermissionsRationaleActivity.class);
                startActivity(intent);
            }
        });

        Button play = findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(MainActivity.this, MainActivity2.class);
                startActivity(intent2);
            }
        });






   /*     checkHealthConnectStatus(this, "com.zombiewalk");*/







/*        ActivityResultContract<Set<String>, Set<String>> requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract();

        requestPermissions = registerForActivityResult(requestPermissionActivityContract, new ActivityResultCallback<Set<String>>() {
            @Override
            public void onActivityResult(Set<String> granted) {
                if (granted.containsAll(PERMISSIONS)) {
                    // Permissions successfully granted
                    System.out.println("Permissions successfully granted");
                } else {
                    // Lack of required permissions
                    System.out.println("Lack of required permissions");
                }
            }
        });*/



    }













    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addresses;
                        try {
                            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        currentcoordinates.setText(addresses.get(0).getLatitude() + ", " + addresses.get(0).getLongitude());

                    }
                }
            });
        }
    }



 /*   public void checkHealthConnectStatus(Context context, String providerPackageName) {
        int availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName);
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            return; // early return as there is no viable integration
        }
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            // Optionally redirect to package installer to find a provider, for example:
            String uriString = "market://details?id=" + providerPackageName + "&url=healthconnect%3A%2F%2Fonboarding";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage("com.android.vending");

            intent.setData(Uri.parse(uriString));
            intent.putExtra("overlay", true);
            intent.putExtra("callerId", context.getPackageName());
            context.startActivity(intent);
            return;
        }
       HealthConnectClient healthConnectClient = HealthConnectClient.getOrCreate(context);
        // Issue operations with healthConnectClient
        checkPermissionsAndRunAsync(healthConnectClient);
    }*/


/*
    private CompletableFuture<Set<String>> getGrantedPermissionsAsync(HealthConnectClient healthConnectClient) {
        CompletableFuture<Set<String>> future = new CompletableFuture<>();
        Executor executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                Set<String> grantedPermissions = healthConnectClient.getPermissionController().getGrantedPermissions();
                future.complete(grantedPermissions); // Resolve the future with the result
            } catch (Exception e) {
                future.completeExceptionally(e); // Resolve the future with an exception
            }
        });

        return future;
    }*/

/*    private void checkPermissionsAndRunAsync(HealthConnectClient healthConnectClient) {
        getGrantedPermissionsAsync(healthConnectClient)
                .thenAccept(granted -> {
                    // This code runs when the permissions are available (asynchronously)
                    if (granted.containsAll(PERMISSIONS)) {
                        // Permissions already granted; proceed with inserting or reading data
                        System.out.println("Permissions already granted");
                    } else {
                        runOnUiThread(() -> {
                            requestPermissions.launch(PERMISSIONS);
                        });
                    }
                })
                .exceptionally(e -> {
                    // Handle any exceptions that occurred during permission retrieval
                    e.printStackTrace();
                    return null; // Return null to indicate the exception was handled
                });*/
   // }





 /*   private final Set<String> PERMISSIONS = new HashSet<>(
            Arrays.asList(
                    HealthPermission.getReadPermission(kClass)
            )
    );*/
    //kotlin.reflect.KClass<StepsRecord> kClass = kotlin.jvm.JvmClassMappingKt.getKotlinClass(StepsRecord.class);
   // private HealthConnectClient healthConnectClient;
   // private ActivityResultLauncher<Set<String>> requestPermissions;




}







