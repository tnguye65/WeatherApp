package com.example.weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var settingsButton: Button
    private lateinit var historyButton: Button
    private var lat: Double = 35.0
    private var lon: Double = 139.0
    private lateinit var unit: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadPreferences()
        textView = findViewById(R.id.weatherInfo)
        settingsButton = findViewById(R.id.settingsButton)
        historyButton = findViewById(R.id.historyButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineLocationGranted != PackageManager.PERMISSION_GRANTED || coarseLocationGranted != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 123)
        } else {
            getLocationAndLoadWeather()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val colorStr = prefs.getString("themeColor", "White")
        unit = prefs.getString("temperatureUnit", "fahrenheit").toString()
        val color = when(colorStr) {
            "Cyan" -> Color.CYAN
            "Yellow" -> Color.YELLOW
            "LightGray" -> Color.LTGRAY
            else -> Color.WHITE
        }
        window.decorView.setBackgroundColor(color)
        loadWeather(unit)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndLoadWeather()
            } else {
                Toast.makeText(this, "Location permission denied. Using default coords.", Toast.LENGTH_SHORT).show()
                loadWeather(unit)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    private fun getLocationAndLoadWeather() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocationGranted == PackageManager.PERMISSION_GRANTED) {
            val location: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                lat = location.latitude
                lon = location.longitude
            } else {
                Toast.makeText(this, "No last known location, using default coords.", Toast.LENGTH_SHORT).show()
            }
        }
        loadWeather(unit)
    }
    private fun loadWeather(unit: String) {
        val weatherView = WeatherView(this)
        weatherView.getModel().setUnits(unit)
        weatherView.displayCurrentWeather(lat, lon) { weatherInfo ->
            runOnUiThread {
                textView.text = weatherInfo
                val cityName = parseCityName(weatherInfo)
                if (cityName.isNotEmpty()) {
                    storeCityInFirebase(cityName)
                }
            }
        }
    }
    private fun parseCityName(info: String): String {
        val lines = info.split("\n")
        for (line in lines) {
            if (line.startsWith("City: ")) {
                return line.substringAfter("City: ").trim()
            }
        }
        return ""
    }
    private fun storeCityInFirebase(city: String) {
        val db = FirebaseDatabase.getInstance().getReference("searched_cities")
        val key = db.push().key
        if (key != null) {
            db.child(key).setValue(city)
        }
    }
}
