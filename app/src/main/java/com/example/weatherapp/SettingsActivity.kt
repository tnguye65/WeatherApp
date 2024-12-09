// Complete SettingsActivity.kt:

package com.example.weatherapp

import android.content.SharedPreferences
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {
    private lateinit var colorSpinner: Spinner
    private lateinit var seekBar: SeekBar
    private lateinit var daysTV: TextView
    private lateinit var clock: AnalogClock
    private lateinit var saveButton: Button
    private lateinit var temperatureUnits: RadioGroup

    private lateinit var prefs: SharedPreferences
    private lateinit var savedColor: String
    private var savedDays: Int = 1
    private lateinit var unit: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setup back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Find views
        colorSpinner = findViewById(R.id.colorSpinner)
        seekBar = findViewById(R.id.daysSeekBar)
        daysTV = findViewById(R.id.daysTextView)
        clock = findViewById(R.id.analogClock)
        saveButton = findViewById(R.id.saveButton)
        temperatureUnits = findViewById(R.id.temperatureUnits)

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
        daysTV.text = "Cities in History: $savedDays"
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                daysTV.text = "Cities in History: $progress"
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

        saveButton.setOnClickListener {
            saveSharedPreferences()
        }
    }

    private fun loadSharedPreferences() {
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        savedColor = prefs.getString("themeColor", "White").toString()
        savedDays = prefs.getInt("forecastDays", 1)
        unit = prefs.getString("temperatureUnit", "fahrenheit").toString()
    }

    private fun saveSharedPreferences() {
        val chosenColor = colorSpinner.selectedItem.toString()
        val numCities = seekBar.progress
        val editor = prefs.edit()
        editor.putString("themeColor", chosenColor)
        editor.putInt("numCities", numCities)
        editor.putString("temperatureUnit", unit)
        editor.apply()
        Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}