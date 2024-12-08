// Complete SettingsActivity.kt:

package com.example.weatherapp

import android.content.SharedPreferences
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var colorSpinner: Spinner
    private lateinit var seekBar: SeekBar
    private lateinit var daysTV: TextView
    private lateinit var clock: AnalogClock
    private lateinit var saveButton: Button
    private lateinit var temperatureUnits: RadioGroup
    private lateinit var locationSearch: EditText
    private lateinit var searchButton: Button

    private lateinit var prefs: SharedPreferences
    private lateinit var savedColor: String
    private var savedDays: Int = 1
    private lateinit var unit: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Find views
        colorSpinner = findViewById(R.id.colorSpinner)
        seekBar = findViewById(R.id.daysSeekBar)
        daysTV = findViewById(R.id.daysTextView)
        clock = findViewById(R.id.analogClock)
        saveButton = findViewById(R.id.saveButton)
        temperatureUnits = findViewById(R.id.temperatureUnits)
        locationSearch = findViewById(R.id.locationSearch)
        searchButton = findViewById(R.id.searchButton)

        // SharedPrefs
        loadSharedPreferences()

        // Spinner
        val colors = arrayOf("White", "Cyan", "Yellow", "LightGray")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner.adapter = adapter
        val index = colors.indexOf(savedColor)
        if (index >= 0) {
            colorSpinner.setSelection(index)
        }

        // Seek bar
        seekBar.progress = savedDays
        daysTV.text = "Days: $savedDays"
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                daysTV.text = "Days: $progress"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Temperature unit settings
        when (unit) {
            "celsius" -> temperatureUnits.check(R.id.celsius)
            "kelvin" -> temperatureUnits.check(R.id.kelvin)
            else -> temperatureUnits.check(R.id.fahrenheit)
        }

        temperatureUnits.setOnCheckedChangeListener { _, checkedId ->
            unit = when (checkedId) {
                R.id.fahrenheit -> "fahrenheit"
                R.id.celsius -> "celsius"
                R.id.kelvin -> "kelvin"
                else -> "fahrenheit"
            }
        }

        // Location search
        searchButton.setOnClickListener {
            val location = locationSearch.text.toString()
            if (location.isNotEmpty()) {
                geocodeLocation(location)
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }

        saveButton.setOnClickListener {
            saveSharedPreferences()
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

                // Save coordinates to SharedPreferences
                val editor = prefs.edit()
                editor.putFloat("saved_lat", lat.toFloat())
                editor.putFloat("saved_lon", lon.toFloat())
                editor.apply()

                // Set result to trigger refresh in MainActivity
                setResult(RESULT_OK)

                Toast.makeText(this, "Location saved: ${address.locality ?: location}", Toast.LENGTH_SHORT).show()
                finish() // Close settings and return to MainActivity
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error finding location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSharedPreferences() {
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        savedColor = prefs.getString("themeColor", "White").toString()
        savedDays = prefs.getInt("forecastDays", 1)
        unit = prefs.getString("temperatureUnit", "fahrenheit").toString()

        // Load saved location if exists
        val savedLocation = prefs.getString("saved_location", "")
        locationSearch.setText(savedLocation)
    }

    private fun saveSharedPreferences() {
        val chosenColor = colorSpinner.selectedItem.toString()
        val chosenDays = seekBar.progress
        val editor = prefs.edit()
        editor.putString("themeColor", chosenColor)
        editor.putInt("forecastDays", chosenDays)
        editor.putString("temperatureUnit", unit)
        editor.putString("saved_location", locationSearch.text.toString())
        editor.apply()
        Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}