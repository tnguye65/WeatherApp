package com.example.weatherapp

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var colorSpinner : Spinner
    private lateinit var seekBar : SeekBar
    private lateinit var daysTV : TextView
    private lateinit var clock : AnalogClock
    private lateinit var saveButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        colorSpinner = findViewById(R.id.colorSpinner)
        seekBar = findViewById(R.id.daysSeekBar)
        daysTV = findViewById(R.id.daysTextView)
        clock = findViewById(R.id.analogClock)
        saveButton = findViewById(R.id.saveButton)
        val colors = arrayOf("White", "Cyan", "Yellow", "LightGray")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner.adapter = adapter
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val savedColor = prefs.getString("themeColor", "White")
        val savedDays = prefs.getInt("forecastDays", 1)
        val index = colors.indexOf(savedColor)
        if (index >= 0) {
            colorSpinner.setSelection(index)
        }
        seekBar.progress = savedDays
        daysTV.text = "Days: $savedDays"
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                daysTV.text = "Days: $progress"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        saveButton.setOnClickListener {
            val chosenColor = colorSpinner.selectedItem.toString()
            val chosenDays = seekBar.progress
            val editor = prefs.edit()
            editor.putString("themeColor", chosenColor)
            editor.putInt("forecastDays", chosenDays)
            editor.apply()
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
