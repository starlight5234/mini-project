package com.upastithi.upastithi

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {

    private val splashTime: Long = 1500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        // Since A12 has its own splash screen API, no need to show our custom splash animation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S){
            Handler(Looper.getMainLooper()).post{
                startActivity( Intent (this, MainActivity::class.java))
                finish()
            }
        } else{
            Handler(Looper.getMainLooper()).postDelayed( {
                startActivity( Intent (this, MainActivity::class.java))
                finish()
            }, splashTime)
        }

    }
}