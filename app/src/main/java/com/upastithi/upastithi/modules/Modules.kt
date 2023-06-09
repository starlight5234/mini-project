package com.upastithi.upastithi.modules

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import javax.net.ssl.SSLHandshakeException

object Modules {

    var ipAddress: String = "https://192.168.0.103:5000/"

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        return false
    }

    private fun isLoginValid(context: Context) : Boolean {

        val userdataJSON = File(context.cacheDir, "userdata.json")

        // Convert file into JSON object for easy access
        val userJSON = FileInputStream(userdataJSON).bufferedReader().use { it.readText() }
        val userInfo = Gson().fromJson(userJSON, User::class.java)

        // Convert user input data into a json to transmit to server
        val jsonString =
            Gson().toJson(User(userInfo.email, userInfo.pass, userInfo.login_type))

        // Initialize http client for server
        val okHttpClient = OkHttpClient()
        val link = ipAddress.plus("api/login")

        // Send JSON request to server
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val req = jsonString.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(link)
            .post(req)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val responseString =
                    Gson().fromJson(response.body!!.string(), LoginResponse::class.java)
                if (responseString.login == "success") {
                    return true
                }
            }
        } catch (e: Throwable) {
            return  false
        }

        return false
    }

    private fun isServerOnline() : Boolean {
        // Initialize http client for server
        val okHttpClient = OkHttpClient()

        // Send JSON request to server
        val request = Request.Builder()
            .url(ipAddress)
            .build()

        try{
            okHttpClient.newCall(request).execute().use { response ->
                if (response.body!!.string() == "online"){
                    return true
                }
            }
        } catch (e: Throwable) {
            return  false
        }

        return false
    }

    fun basicValidation(applicationContext: Context): Array<String> {

        var isDataValid: Array<String> = arrayOf("false", "", "")

        // Online Checker AlertDialog
        if (!isOnline(applicationContext)){
            isDataValid[1] = "No Internet Connection!"
            isDataValid[2] = "Device is not connected to Internet"
            return isDataValid
        }

        if (!isServerOnline()){
            isDataValid[1] = "Server unavailable!"
            isDataValid[2] = "Server is not currently working"
            return isDataValid
        }

        if (!isLoginValid(applicationContext)){
            applicationContext.cacheDir.deleteRecursively()
            isDataValid[1] = "Something strange happened!"
            isDataValid[2] = "It seems like your session had been logged out, please login again!"
            return isDataValid
        }

        isDataValid = arrayOf("true")
        return isDataValid
    }
}