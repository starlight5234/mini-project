package com.upastithi.upastithi

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.gson.Gson
import com.upastithi.upastithi.modules.Modules
import com.upastithi.upastithi.modules.StudentInfo
import com.upastithi.upastithi.modules.User
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.PrintWriter
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.system.exitProcess


class StudentScreen : AppCompatActivity() {
    lateinit var pieChart: PieChart
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_screen)

        val markAttendance = findViewById<CardView>(R.id.markAttendance)
        val studentProfileCard = findViewById<TextView>(R.id.studentProfileCard)
        val profileButton = findViewById<ImageView>(R.id.profileButton)
        val attendanceCardInfo = findViewById<TextView>(R.id.attendanceCardInfo)
        val studentNameCard = findViewById<TextView>(R.id.studentNameCard)
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        pieChart = findViewById(R.id.attendancePie)
        var attendanceType: String = "t"
        var studentData: StudentInfo? = null

        Thread{
            try {
                val isDataValid: Array<String> = Modules.basicValidation(applicationContext)
                if (isDataValid[0] == "false") {
                    Handler(Looper.getMainLooper()).post {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                        builder.setTitle(isDataValid[1]).setMessage(isDataValid[2]).setCancelable(false)
                            .setPositiveButton("Exit!"){_,_ ->
                                finishAffinity()
                            }
                        builder.show()
                    }
                } else {
                    studentData = fetchData(applicationContext, studentNameCard, studentProfileCard, welcomeText)
                    Handler(Looper.getMainLooper()).post {
                        if (studentData != null) {
                            loadPie(pieChart, attendanceCardInfo, studentData!!, attendanceType)
                        }
                    }
                }
            }catch (e: Throwable){
                Log.e("Error", "Couldn't communicate with server", e)
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.exception_occurred).setMessage(R.string.throwable_exception).setCancelable(false)
                    .setPositiveButton("Exit!"){_,_ ->
                        finishAffinity()
                    }
                builder.show()
            }
        }.start()

        markAttendance.setOnClickListener {
            val attendanceIntent = Intent(this, MarkAttendance::class.java)
            startActivity(attendanceIntent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        attendanceCardInfo.setOnClickListener {
            if (attendanceType == "t"){
                attendanceType = "p"
                pieChart.invalidate()
                loadPie(pieChart, attendanceCardInfo, studentData!!, attendanceType)
            } else if (attendanceType == "p") {
                attendanceType = "t"
                pieChart.invalidate()
                loadPie(pieChart, attendanceCardInfo, studentData!!, attendanceType)
            }
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

    private fun fetchData(
        applicationContext: Context,
        studentNameCard: TextView,
        profileCard: TextView,
        welcomeText: TextView
    ): StudentInfo? {
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
                val responseString = gson.fromJson(response.body?.string(), StudentInfo::class.java)

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
            val studentData = gson.fromJson(responseJSON, StudentInfo::class.java)

            val infoBuilder = StringBuilder()
            infoBuilder.append("ID: ")
                .append(studentData.id)
                .append("\n")
                .append("Year: ")
                .append(studentData.year)
                .append(" ")
                .append(studentData.course)
                .append("\n")
                .append("Div: ")
                .append(studentData.div)
                .append("\n")
                .append("Batch: ")
                .append(studentData.batch)

            val card3Text = infoBuilder.toString()

            runOnUiThread {
                studentNameCard.text = studentData.name
                welcomeText.text = "Welcome ".plus(studentData.name.split(' ').first().plus(" \uD83D\uDC4B"))
                welcomeText.visibility = View.VISIBLE
                profileCard.text = card3Text
            }
            return studentData
        } catch (e: Throwable){
            Log.e("Exception: ", "Threw an exception", e)
            Handler(Looper.getMainLooper()).post {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.exception_occurred).setMessage(R.string.throwable_exception).setCancelable(false)
                    .setPositiveButton("Exit!"){_,_ ->
                        this.recreate()
                    }
                builder.show()
            }
        }
        return null
    }

    private fun loadPie(
        pieChart: PieChart,
        attendanceCardInfo: TextView,
        studentData: StudentInfo,
        attendanceType: String
    ){
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 5f, 5f, 5f)

        // Center Gap
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.holeRadius = 58f
        pieChart.transparentCircleRadius = 61f

        // A bit of interaction
        pieChart.rotationAngle = 0f
        pieChart.isRotationEnabled = true
        pieChart.isHighlightPerTapEnabled = true

        // Animate and style
        pieChart.animateY(1400, Easing.EaseInOutQuad)
        pieChart.legend.isEnabled = false

        var attended: Float = 1F
        var notAttended: Float = 0F

        if (attendanceType == "t") {
            if (studentData.theory_total != 0){
                attended =
                    studentData.theory_attended.toFloat() / studentData.theory_total.toFloat()
                notAttended = 1.toFloat() - attended
            }
        } else if (attendanceType == "p") {
            if (studentData.prac_total != 0) {
                attended = studentData.prac_attended.toFloat() / studentData.prac_total.toFloat()
                notAttended = 1.toFloat() - attended
            }
        }

        // Need 2 values always
        val entries: ArrayList<PieEntry> = ArrayList()
        entries.add(PieEntry(attended))
        entries.add(PieEntry(notAttended))

        // Corresponding colors for the values
        val colors: ArrayList<Int> = ArrayList()
        if (attended >= 0.75f){
            colors.add(this.getColor(R.color.nordGreen))
        } else if (attended >= 0.65f){
            colors.add(this.getColor(R.color.nordYellow))
        } else {
            colors.add(this.getColor(R.color.nordRed))
        }

        colors.add(Color.WHITE)

        val dataSet = PieDataSet(entries, "Attendance")
        dataSet.setDrawIcons(false)
        // Space between 2 pie items
        dataSet.sliceSpace = 0f
        dataSet.selectionShift = 5f
        // Set the colors as intended
        dataSet.colors = colors

        // load data set into Pie Chart
        val data = PieData(dataSet)
        // Disable individual label of slices
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(0f)
        // Set Pie Chart data
        pieChart.data = data

        // Text that should be in center
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "%.1f".format(((attended * 100).toString()).toFloat()).plus("%")

        val builder = StringBuilder()

        builder.append("Lecture attended:\n")
            .append(studentData.theory_attended)
            .append(" out of ")
            .append(studentData.theory_total)
            .append("\n\n")
            .append("Practicals attended:\n")
            .append(studentData.prac_attended)
            .append(" out of ")
            .append(studentData.prac_total)

        val card3Text = builder.toString()

        runOnUiThread {
            attendanceCardInfo.text = card3Text
        }

        pieChart.highlightValues(null)
        pieChart.visibility = View.VISIBLE
        pieChart.invalidate() // Refresh Pie
    }
}