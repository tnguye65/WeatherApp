package com.example.weatherapp

import android.content.SharedPreferences
import android.location.Geocoder
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.FirebaseDatabase

class SearchActivity : AppCompatActivity() {
    private lateinit var locationSearch : EditText
    private lateinit var searchButton: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setup back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Initialize views
        locationSearch = findViewById(R.id.locationSearch)
        searchButton = findViewById(R.id.searchButton)

        // Location search
        searchButton.setOnClickListener {
            val location = locationSearch.text.toString()
            if (location.isNotEmpty()) {
                geocodeLocation(location)
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun geocodeLocation(location: String) {
        val geocoder = Geocoder(this)
        try {
            val addresses = geocoder.getFromLocationName(location, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val lat = address.latitude
                val lon = address.longitude

                // Set result to trigger refresh in MainActivity
                val editor = prefs.edit()
                editor.putFloat("saved_lat", lat.toFloat())
                editor.putFloat("saved_lon", lon.toFloat())
                editor.apply()

                Toast.makeText(this, "Location saved: ${address.locality ?: location}", Toast.LENGTH_SHORT).show()
                finish() // Close settings and return to MainActivity
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error finding location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}