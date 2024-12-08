package com.example.weatherapp

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val weatherView = WeatherView(this)
        val textView = findViewById<TextView>(R.id.weatherInfo)

        weatherView.displayCurrentWeather(35.0, 139.0) { weatherInfo ->
            runOnUiThread {
                textView.text = weatherInfo
            }
        }

    }
}