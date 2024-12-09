package com.example.weatherapp

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {
    private lateinit var sb: SeekBar
    private lateinit var tvDays: TextView
    private lateinit var clkAnalog: AnalogClock
    private lateinit var btnSave: Button
    private lateinit var rgTemp: RadioGroup
    private lateinit var prefs: SharedPreferences
    private var savedDays = 1
    private lateinit var unit: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById<Toolbar>(R.id.tbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        initializeViews()
        loadSharedPreferences()
        setupSeekBar()
        setupTemperatureUnits()
        btnSave.setOnClickListener { saveSharedPreferences() }
    }

    private fun initializeViews() {
        sb = findViewById(R.id.sbDays)
        tvDays = findViewById(R.id.tvDays)
        clkAnalog = findViewById(R.id.clkAnalog)
        btnSave = findViewById(R.id.btnSave)
        rgTemp = findViewById(R.id.rgTemp)
    }

    private fun setupSeekBar() {
        sb.progress = savedDays
        tvDays.text = "Cities in History: $savedDays"
        sb.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDays.text = "Cities in History: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupTemperatureUnits() {
        rgTemp.check(when(unit) {
            "celsius" -> R.id.rbC
            "kelvin" -> R.id.rbK
            else -> R.id.rbF
        })
        rgTemp.setOnCheckedChangeListener { _, checkedId ->
            unit = when(checkedId) {
                R.id.rbC -> "celsius"
                R.id.rbK -> "kelvin"
                else -> "fahrenheit"
            }
        }
    }

    private fun loadSharedPreferences() {
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        savedDays = prefs.getInt("forecastDays", 1)
        unit = prefs.getString("temperatureUnit", "fahrenheit").toString()
    }

    private fun saveSharedPreferences() {
        prefs.edit().apply {
            putInt("numCities", sb.progress)
            putString("temperatureUnit", unit)
            apply()
        }
        Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}