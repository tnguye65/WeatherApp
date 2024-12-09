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
            etLoc.text.toString().takeIf { it.isNotEmpty() }?.let { geocodeLocation(it) }
                ?: Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun geocodeLocation(location: String) = try {
        Geocoder(this).getFromLocationName(location, 1)?.firstOrNull()?.let { address ->
            prefs.edit().apply {
                putFloat("saved_lat", address.latitude.toFloat())
                putFloat("saved_lon", address.longitude.toFloat())
                apply()
            }
            Toast.makeText(this, "Location saved: ${address.locality ?: location}", Toast.LENGTH_SHORT).show()
            finish()
        } ?: Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(this, "Error finding location: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}