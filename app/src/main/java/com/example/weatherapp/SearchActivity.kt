package com.example.weatherapp

import android.content.SharedPreferences
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.FirebaseDatabase

class SearchActivity : AppCompatActivity() {
    private lateinit var etLoc: EditText
    private lateinit var btnSearch: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        setSupportActionBar(findViewById<Toolbar>(R.id.tbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        etLoc = findViewById(R.id.etLoc)
        btnSearch = findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener {
            val locationText = etLoc.text.toString()
            if (locationText.isNotEmpty()) {
                geocodeLocation(locationText)
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun geocodeLocation(location: String) {
        try {
            val addresses = Geocoder(this).getFromLocationName(location, 1)
            if (addresses != null && addresses.size > 0) {
                val address = addresses[0]
                val editor = prefs.edit()
                editor.putFloat("saved_lat", address.latitude.toFloat())
                editor.putFloat("saved_lon", address.longitude.toFloat())
                editor.apply()
                val locationName = address.locality ?: location
                Toast.makeText(this, "Location saved: $locationName", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error finding location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}