package com.upastithi.upastithi

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.upastithi.upastithi.modules.LoginResponse
import com.upastithi.upastithi.modules.Modules
import com.upastithi.upastithi.modules.User
import java.io.File
import java.io.FileInputStream


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.navigationBarColor = applicationContext.getColor(R.color.nordFrost)

        // Online Checker AlertDialog
        if (!Modules.isOnline(applicationContext)){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("No Internet Connection!").setMessage("Device is not connected to Internet").setCancelable(false)
                .setPositiveButton("Okay!"){_,_ ->
                    finishAffinity()
                }
            builder.show()
        } else {
            autoLogin()
        }

        //button for login as teacher
        val teacherButton = findViewById<Button>(R.id.teacher_button)

        teacherButton.setOnClickListener {
            val teacherIntent = Intent(this,Login::class.java)
            teacherIntent.putExtra("loginType", "T")
            startActivity(teacherIntent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        // button for login as student
        val studentButton = findViewById<Button>(R.id.student_button)

        studentButton.setOnClickListener {
            val studentIntent = Intent(this,Login::class.java)
            studentIntent.putExtra("loginType", "S")
            startActivity(studentIntent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }

    private fun autoLogin(){
        // check for login.json
        // if login.json exists then check loginType in the json
        // open teacher activity if login type is T else student activity if S
        // else open login activity if string doesn't exist or null
        val loginJSON = File(applicationContext.cacheDir, "login.json")

        if (loginJSON.exists()) {
            val inputAsString = FileInputStream(loginJSON).bufferedReader().use { it.readText() }
            val loginInfo = Gson().fromJson(inputAsString, LoginResponse::class.java)

            val userdataJSON = File(applicationContext.cacheDir, "userdata.json")
            val userString = FileInputStream(userdataJSON).bufferedReader().use { it.readText() }
            val userInfo = Gson().fromJson(userString, User::class.java)

            if (inputAsString.isNotEmpty()) {
                if (loginInfo.login.lowercase() == "success") {
                    if (userInfo.login_type.lowercase() == "s") {
                        val loadIntent =
                            Intent(applicationContext, StudentScreen::class.java)
                        finishAffinity()
                        startActivity(loadIntent)
                    } else if (userInfo.login_type.lowercase() == "t") {
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
            }
        }
    }
}