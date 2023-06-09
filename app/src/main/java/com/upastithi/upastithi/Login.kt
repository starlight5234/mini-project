package com.upastithi.upastithi

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.upastithi.upastithi.modules.LoginResponse
import com.upastithi.upastithi.modules.Modules
import com.upastithi.upastithi.modules.User
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        window.navigationBarColor = applicationContext.getColor(R.color.nordDark)

        // Online Checker AlertDialog
        if (!Modules.isOnline(applicationContext)){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("No Internet Connection!").setMessage("Device is not connected to Internet").setCancelable(false)
                .setPositiveButton("Okay!"){_,_ ->
                    finishAffinity()
                }
            builder.show()
        }

        //forgot password button
        val forgotButton = findViewById<TextView>(R.id.forgotPass)
        forgotButton.setOnClickListener {
            val toDoToast = Toast.makeText(this, getString(R.string.to_do), Toast.LENGTH_SHORT)
            toDoToast.show()
        }

        val loginButton = findViewById<Button>(R.id.loginButton)
        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)

        // Extract login_type field based on the button clicked in Main Activity
        val extras = intent.extras
        var loginType = ""
        if (extras != null) {
            loginType = extras.getString("loginType").toString()
        }

        // Initialize http client for server
        val okHttpClient = OkHttpClient()

        loginButton.setOnClickListener {

            if (username.text.toString().isEmpty()){
                return@setOnClickListener
            }
            if (password.text.toString().isEmpty()){
                return@setOnClickListener
            }

            // Initialize gson object
            val gson = Gson()

            // Convert user input data into a json to transmit to server
            val jsonString =
                gson.toJson(User(username.text.toString(), password.text.toString(), loginType))

            // Save user input data to userdata.json
            val userdataJSON = File(applicationContext.cacheDir, "userdata.json")

            // Save response to login.json
            val loginJSON = File(applicationContext.cacheDir, "login.json")

            println("Starting Req Thread")

            // Send JSON request to server
            val link = Modules.ipAddress.plus("api/login")

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val req = jsonString.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(link)
                .post(req)
                .build()

            // Async call to server
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println(e)
                    call.cancel()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            applicationContext,
                            "Server Unavailable",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        // Store JSON response sent by server as a string
                        val responseString =
                            gson.fromJson(response.body?.string(), LoginResponse::class.java)

                        // delete existing userdata.json
                        if (userdataJSON.exists()) {
                            userdataJSON.delete()
                            loginJSON.delete()
                        }

                        // Write user entered data to userdata.json
                        try {
                            PrintWriter(FileWriter(userdataJSON)).use {
                                it.write(jsonString)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Write server's login response to login.json
                        try {
                            PrintWriter(FileWriter(loginJSON)).use {
                                if (responseString.toString().isNotEmpty()) {
                                    it.write(gson.toJson(responseString))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Read login.json
                        val responseJSON =
                            FileInputStream(loginJSON).bufferedReader().use { it.readText() }

                        // Convert file into JSON object for easy access
                        val loginInfo = gson.fromJson(responseJSON, LoginResponse::class.java)
                        // Check what login.json contains
                        // null -> Server didn't respond (501)
                        // success -> Login granted
                        // failed -> Login denied (password mismatch)
                        // 404/anything else -> Missing user in login DB
                        if (responseJSON.isNotEmpty()) {
                            println("Login: ".plus(loginInfo.login))
                            when (loginInfo.login.lowercase()) {
                                "success" -> {
                                    if (loginType.lowercase() == "s") {
                                        val loadIntent =
                                            Intent(applicationContext, StudentScreen::class.java)
                                        finishAffinity()
                                        startActivity(loadIntent)
                                    } else if (loginType.lowercase() == "t") {
                                        val loadIntent =
                                            Intent(applicationContext, TeacherScreen::class.java)
                                        finishAffinity()
                                        startActivity(loadIntent)
                                    } else {
                                        Handler(Looper.getMainLooper()).post {
                                            Toast.makeText(
                                                applicationContext,
                                                "Login Failed",
                                                Toast.LENGTH_LONG
                                            )
                                                .show()
                                        }
                                    }
                                }
                                "failed" -> {
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(
                                            applicationContext,
                                            "Login Failed",
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                    }
                                }
                                else -> {
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(
                                            applicationContext,
                                            "User not found",
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                    }
                                }
                            }
                        } else {
                            // Domain is available but backend is not running
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    applicationContext,
                                    "501 - Internal Server Error",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            })
        }

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAfterTransition()
                overridePendingTransition(android.R.transition.slide_bottom, android.R.transition.slide_top)
            }
        })
    }
}