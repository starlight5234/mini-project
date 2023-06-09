package com.upastithi.upastithi

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import com.upastithi.upastithi.modules.Modules
import com.upastithi.upastithi.modules.TeacherInfo
import com.upastithi.upastithi.modules.User
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import kotlin.system.exitProcess

class TeacherScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_screen)

        val markAttendance = findViewById<CardView>(R.id.markAttendance)
        val teacherProfileCard = findViewById<TextView>(R.id.teacherProfileCard)
        val profileButton = findViewById<ImageView>(R.id.profileButton)

        Thread{
            val isDataValid: Array<String> = Modules.basicValidation(applicationContext)
            if (isDataValid[0] == "false"){
                Handler(Looper.getMainLooper()).post {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle(isDataValid[1]).setMessage(isDataValid[2]).setCancelable(false)
                        .setPositiveButton("Exit!"){_,_ ->
                            finishAffinity()
                        }
                    builder.show()
                }
            } else {
                fetchData(applicationContext, teacherProfileCard)
            }
        }.start()

        markAttendance.setOnClickListener{
            val generateAttendanceIntent = Intent(this,GenerateAttendance::class.java)
            startActivity(generateAttendanceIntent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        profileButton.setOnClickListener{
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.logout_title)).setMessage(getString(R.string.logout_confirmation_message)).setCancelable(true)
                .setPositiveButton(getString(R.string.yes)){ _, _ ->
                    applicationContext.cacheDir.deleteRecursively()
                    finish()
                }
                .setNegativeButton(getString(R.string.no)){ dialogInterface, _ ->
                    dialogInterface.cancel()
                }
            builder.show()
        }
    }

    private fun fetchData(applicationContext: Context, profileCard: TextView){
            // Fetch Data from server
            /* User Model JSON */
            // Initialize gson object
            val gson = Gson()
            val userinfoJSON = File(applicationContext.cacheDir, "userinfo.json")
            val userdataJSON = File(applicationContext.cacheDir, "userdata.json")

            // Convert file into JSON object for easy access
            val userJSON = FileInputStream(userdataJSON).bufferedReader().use { it.readText() }
            val userInfo = gson.fromJson(userJSON, User::class.java)

            // Convert user input data into a json to transmit to server
            val jsonString =
                gson.toJson(User(userInfo.email, userInfo.pass, userInfo.login_type))

            // Initialize http client for server
            val okHttpClient = OkHttpClient()
            val link = Modules.ipAddress.plus("api/info")
            // Send JSON request to server
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val req = jsonString.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(link)
                .post(req)
                .build()

        try{
            okHttpClient.newCall(request).execute().use { response ->

                // Store JSON response sent by server as a string
                val responseString = gson.fromJson(response.body?.string(), TeacherInfo::class.java)

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
            val teacherData = gson.fromJson(responseJSON, TeacherInfo::class.java)

            val builder = StringBuilder()
            builder.append("Name: ")
                .append(teacherData.name)
                .append("\n")
                .append("Email: ")
                .append(teacherData.email)
                .append("\n\n")
                .append("Subjects: ")
                .append(teacherData.subjects)

            val card1Text = builder.toString()

            runOnUiThread {
                profileCard.text = card1Text
            }
        } catch (e: Throwable){
            Log.e("Exception: ", "Threw an exception", e)
            Handler(Looper.getMainLooper()).post {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.exception_occurred)).setMessage(getString(R.string.throwable_exception)).setCancelable(false)
                    .setPositiveButton("Exit!"){_,_ ->
                        finishAndRemoveTask()
                    }
                builder.show()
            }
        }
    }
}