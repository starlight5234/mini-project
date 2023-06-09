package com.upastithi.upastithi

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.upastithi.upastithi.modules.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.PrintWriter


@Suppress("DEPRECATION")
class MarkAttendance : AppCompatActivity() {
    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 30
        fastestInterval = 10
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        maxWaitTime = 60
    }

    var locationRes: Location? = null

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()
                Log.d("Location", location.toString())
                locationRes = location
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark_attendance)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        val markAttendance = findViewById<Button>(R.id.markAttendance)
        val lectureCode = findViewById<EditText>(R.id.lectureCode)
        var latitudeRightNow: String? = null
        var longitudeRightNow: String? = null

        println("Fetching Location...")
        fusedLocationProvider?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        markAttendance.setOnClickListener {
            val progressDialog = ProgressDialog(this)

            if (lectureCode.text.isEmpty()){
                Toast.makeText(this, "Please input class code", Toast.LENGTH_SHORT).show()
            } else {
                progressDialog.setTitle("Fetching Location")
                progressDialog.setMessage("Please wait...")
                progressDialog.show()
                Handler().postDelayed({
                    progressDialog.dismiss()
                    latitudeRightNow = locationRes?.latitude.toString()
                    longitudeRightNow = locationRes?.longitude.toString()
                    val locationString = "$latitudeRightNow, $longitudeRightNow"

                    if (!locationString.contains("null")) {
                        progressDialog.setTitle("Marking Attendance")
                        progressDialog.setMessage("Please wait...")
                        progressDialog.show()
                        markStudentAttendance(lectureCode.text.toString(), locationString)
                        progressDialog.dismiss()
                    } else {
                        Toast.makeText(this, "Unable to fetch your current location", Toast.LENGTH_SHORT).show()
                    }

                }, 5000)
            }
        }
    }

    private fun markStudentAttendance(code: String, locationString: String) {
        Thread{
            // Fetch Data from server
            /* User Model JSON */
            // Initialize gson object
            val gson = Gson()
            val userinfoJSON = File(applicationContext.cacheDir, "userinfo.json")
            val userdataJSON = File(applicationContext.cacheDir, "userdata.json")

            // Convert file into JSON object for easy access
            val userJSON = FileInputStream(userdataJSON).bufferedReader().use { it.readText() }
            val userData = gson.fromJson(userJSON, User::class.java)

            // Convert user input data into a json to transmit to server
            val jsonString =
                gson.toJson(StudentMark(userData.email, userData.pass, userData.login_type, code, locationString))

//            println(jsonString)

            // Initialize http client for server
            val okHttpClient = OkHttpClient()
            val link = Modules.ipAddress.plus("api/mark")
            // Send JSON request to server
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val req = jsonString.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(link)
                .post(req)
                .build()

            try{
                okHttpClient.newCall(request).execute().use { response ->

//                    println(response)
                    // Store JSON response sent by server as a string
                    val responseString = gson.fromJson(response.body?.string(), StudentMarkResponse::class.java)

                    // delete existing userdata.json
                    if (userinfoJSON.exists()) {
                        userinfoJSON.delete()
                    }

                    // Write user entered data to userdata.json
                    try {
                        PrintWriter(FileWriter(userinfoJSON)).use {
                            it.write(gson.toJson(responseString))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                val responseJSON = FileInputStream(userinfoJSON).bufferedReader().use { it.readText() }
                val markedData = gson.fromJson(responseJSON, StudentMarkResponse::class.java)

                println(markedData)

//                val builder = StringBuilder()
//                builder.append("Name: ")
//                    .append(studentData.name)
//                    .append("\n")
//                    .append("Email: ")
//                    .append(studentData.email)
//                    .append("\n\n")
//                    .append("Subjects: ")
//                    .append(studentData.subjects)
//
//                val card1Text = builder.toString()
//
//                runOnUiThread {
//                    profileCard.text = card1Text
//                }

                val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)

                if (markedData.response_code == "403"){
                    Handler(Looper.getMainLooper()).post {
                        builder.setTitle("Outside VPPCOE")
                            .setMessage("Your location was not within the bounds of VPPCOE")
                            .setCancelable(false)
                            .setPositiveButton("Back") { _, _ ->
                                finishActivity(0)
                            }
                        builder.show()
                    }
                } else if (markedData.response_code == "208"){
                    Handler(Looper.getMainLooper()).post {
                        builder.setTitle("Already marked")
                            .setMessage("Your attendance has already been marked")
                            .setCancelable(false)
                            .setPositiveButton("Great!") { _, _ ->
                                finishActivity(0)
                            }
                        builder.show()
                    }
                }

            } catch (e: Throwable){
                Log.e("Exception: ", "Threw an exception", e)
                Handler(Looper.getMainLooper()).post {
                    val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
                    builder.setTitle(getString(R.string.exception_occurred)).setMessage(getString(R.string.throwable_exception)).setCancelable(false)
                        .setPositiveButton("Exit!"){_,_ ->
                            finishAndRemoveTask()
                        }
                    builder.show()
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationProvider?.removeLocationUpdates(locationCallback)
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        } else {
            checkBackgroundLocation()
        }
    }

    private fun checkBackgroundLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
//                        fusedLocationProvider?.requestLocationUpdates(
//                            locationRequest,
//                            locationCallback,
//                            Looper.getMainLooper()
//                        )

                        // Now check background location
                        checkBackgroundLocation()
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()

                    // Check if we are in a state where the user has denied the permission and
                    // selected Don't ask again
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", this.packageName, null),
                            ),
                        )
                    }
                }
                return
            }
            MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
//                        fusedLocationProvider?.requestLocationUpdates(
//                            locationRequest,
//                            locationCallback,
//                            Looper.getMainLooper()
//                        )

                        Toast.makeText(
                            this,
                            "Granted Background Location Permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
                return

            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private const val MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 66
    }
}