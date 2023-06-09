package com.upastithi.upastithi

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import com.upastithi.upastithi.modules.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream

class GenerateAttendance : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_attendance_form)

        // Online Checker AlertDialog
        if (!Modules.isOnline(applicationContext)){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("No Internet Connection!").setMessage("Device is not connected to Internet").setCancelable(false)
                .setPositiveButton("Okay!"){_,_ ->
                    finish()
                }
            builder.show()
        }

        val lectureCode = findViewById<TextView>(R.id.lectureCode)

        val gson = Gson()

        val userdataJSON = File(applicationContext.cacheDir, "userdata.json")
        val userJSON = FileInputStream(userdataJSON).bufferedReader().use { it.readText() }
        val userData = gson.fromJson(userJSON, User::class.java)

        val userinfoJSON = File(applicationContext.cacheDir, "userinfo.json")
        val userinfoData = FileInputStream(userinfoJSON).bufferedReader().use { it.readText() }
        val userInfo = gson.fromJson(userinfoData, TeacherInfo::class.java)


        // Need to fetch this from server
        val subject = userInfo.subjects.toString().split(',')

        val lectureTime = listOf("8:00", "8:50", "9:50", "10:00", "11:00", "1:30", "2:30", "3:30")
        val lectureType = listOf("Theory", "Practicals")
        val year = listOf("FE", "SE", "TE", "BE")
        val classSelector = listOf("A", "B", "C", "D", "E")
        val batchSelector = listOf("A", "B", "C", "D")


        // Dropdown List setter
        // Find the view of AutoCompleteText
        // Load all items in an Adapter
        // set the Adapter as the view
        val subjectTextView: AutoCompleteTextView = findViewById(R.id.subjects)
        val subjectAdapter = ArrayAdapter(this, R.layout.list_item, subject)
        subjectTextView.setAdapter(subjectAdapter)

        val lectureTimeView: AutoCompleteTextView = findViewById(R.id.lectureTime)
        val lectureTimeAdapter = ArrayAdapter(this, R.layout.list_item, lectureTime)
        lectureTimeView.setAdapter(lectureTimeAdapter)

        val lectureTypeView: AutoCompleteTextView = findViewById(R.id.lectureType)
        val lectureTypeAdapter = ArrayAdapter(this, R.layout.list_item, lectureType)
        lectureTypeView.setAdapter(lectureTypeAdapter)

        val yearView: AutoCompleteTextView = findViewById(R.id.yearSelector)
        val yearAdapter = ArrayAdapter(this, R.layout.list_item, year)
        yearView.setAdapter(yearAdapter)

        val classSelectorView: AutoCompleteTextView = findViewById(R.id.classSelector)
        val classSelectorAdapter = ArrayAdapter(this, R.layout.list_item, classSelector)
        classSelectorView.setAdapter(classSelectorAdapter)

        val batchSelectorView: AutoCompleteTextView = findViewById(R.id.classBatch)
        val batchSelectorAdapter = ArrayAdapter(this, R.layout.list_item, batchSelector)
        batchSelectorView.setAdapter(batchSelectorAdapter)

        lectureTypeView.onItemClickListener = AdapterView.OnItemClickListener{
            _, _, _, _ ->
            if (lectureTypeView.text.toString().lowercase() == "practicals"){
                findViewById<CardView>(R.id.batchCard).visibility = View.VISIBLE
            } else {
                findViewById<CardView>(R.id.batchCard).visibility = View.INVISIBLE
            }
        }

        val recordAttendance = findViewById<Button>(R.id.recordAttendance)
        recordAttendance.setOnClickListener {
            if (subjectTextView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Subject", Toast.LENGTH_SHORT).show()
               return@setOnClickListener
            }
            if (lectureTimeView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Lecture Time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lectureTypeView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Lecture type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (yearView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Year", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (classSelectorView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Class", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lectureTypeView.text.toString().lowercase() == "practicals" && batchSelectorView.text.toString().isEmpty()){
                Toast.makeText(this, "Select Practical batch", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var lectureTypeString: String = lectureTypeView.text.toString().subSequence(0,2) as String

            if (lectureTypeView.text.toString().lowercase() == "practicals"){
                lectureTypeString += "-".plus(batchSelectorView.text.toString())
            }

            // Convert user input data into a json to transmit to server
            val jsonString = gson.toJson(
                GenerateCode(
                userData.email,
                userData.pass,
                userData.login_type,
                subjectTextView.text.toString(),
                lectureTimeView.text.toString().replace(":","_"),
                lectureTypeString,
                yearView.text.toString(),
                classSelectorView.text.toString()
            )
            )

            Thread{
                // Initialize http client for server
                val okHttpClient = OkHttpClient()
                val link = Modules.ipAddress.plus("api/generate")
                // Send JSON request to server
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val req = jsonString.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(link)
                    .post(req)
                    .build()

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        // Store JSON response sent by server as a string
                        val responseString =
                            gson.fromJson(response.body?.string(), GenResponseCode::class.java)

                        if (responseString.response_code == "200") {
                            runOnUiThread {
                                lectureCode.text = responseString.code.toString()
                            }
                        }

                    }
                } catch (e: Throwable){
                    Log.e("Exception: ", "Threw an exception", e)
                }
            }.start()

        }

        val stopAttendance = findViewById<Button>(R.id.stopAttendance)

        stopAttendance.setOnClickListener {
            if (subjectTextView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Subject", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lectureTimeView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Lecture Time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lectureTypeView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Lecture type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (yearView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Year", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (classSelectorView.text.toString().isEmpty()){
                Toast.makeText(this, "Select a Class", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (lectureTypeView.text.toString().lowercase() == "practicals" && batchSelectorView.text.toString().isEmpty()){
                Toast.makeText(this, "Select Practical batch", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var lectureTypeString: String = lectureTypeView.text.toString().subSequence(0,2) as String

            if (lectureTypeView.text.toString().lowercase() == "practicals"){
                lectureTypeString += "-".plus(batchSelectorView.text.toString())
            }

            // Convert user input data into a json to transmit to server
            val jsonString = gson.toJson(
                GenerateCode(
                    userData.email,
                    userData.pass,
                    userData.login_type,
                    subjectTextView.text.toString(),
                    lectureTimeView.text.toString().replace(":","_"),
                    lectureTypeString,
                    yearView.text.toString(),
                    classSelectorView.text.toString()
                )
            )

            Thread{
                // Initialize http client for server
                val okHttpClient = OkHttpClient()
                val link = Modules.ipAddress.plus("api/remove")
                // Send JSON request to server
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val req = jsonString.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(link)
                    .post(req)
                    .build()

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        // Store JSON response sent by server as a string
                        val responseString =
                            gson.fromJson(response.body?.string(), GenResponseCode::class.java)

                        if (responseString.response_code == "200") {
                            runOnUiThread {
                                lectureCode.text = ""
                                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                                builder.setTitle("Stopping attendance")
                                    .setMessage("Attendance for the lecture has been stopped!")
                                    .setCancelable(false)
                                    .setNeutralButton("View Report"){ _, _ ->
                                        Toast.makeText(this, "To-Do", Toast.LENGTH_SHORT).show()
                                    }
                                    .setNegativeButton("Back"){_,_ ->
                                        finish()
                                    }
                                builder.show()
                            }
                        } else if (responseString.response_code == "400") {
                            runOnUiThread {
                                Toast.makeText(this, "No lectures are currently going on.", Toast.LENGTH_SHORT).show()
                            }
                        } else if (responseString.response_code == "404") {
                            runOnUiThread {
                                Toast.makeText(this, "No such lecture is going on.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Internal Server Error.", Toast.LENGTH_SHORT).show()
                        }

                    }
                } catch (e: Throwable){
                    Log.e("Exception: ", "Threw an exception", e)
                }
            }.start()
        }

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAfterTransition()
                overridePendingTransition(android.R.transition.slide_bottom, android.R.transition.slide_top)
            }
        })
    }

}