package com.example.weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var searchButton: Button
    private lateinit var settingsButton: Button
    private lateinit var historyButton: Button
    private var lat: Double = 35.0
    private var lon: Double = 139.0
    private lateinit var unit: String
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadPreferences()

        textView = findViewById(R.id.weatherInfo)
        searchButton = findViewById(R.id.searchButton)
        settingsButton = findViewById(R.id.settingsButton)
        historyButton = findViewById(R.id.historyButton)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        searchButton.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        checkLocationPermissionsAndProceed()
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
        loadWeather(unit)
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val colorStr = prefs.getString("themeColor", "White")
        unit = prefs.getString("temperatureUnit", "fahrenheit").toString()

        // Load saved coordinates if they exist
        val savedLat = prefs.getFloat("saved_lat", Float.MIN_VALUE)
        val savedLon = prefs.getFloat("saved_lon", Float.MIN_VALUE)

        if (savedLat != Float.MIN_VALUE && savedLon != Float.MIN_VALUE) {
            lat = savedLat.toDouble()
            lon = savedLon.toDouble()
            loadWeather(unit)
        }

        val color = when(colorStr) {
            "Cyan" -> Color.CYAN
            "Yellow" -> Color.YELLOW
            "LightGray" -> Color.LTGRAY
            else -> Color.WHITE
        }
        window.decorView.setBackgroundColor(color)
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationPermissionsAndProceed() {
        if (!hasLocationPermissions()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            checkLocationEnabledAndProceed()
        }
    }

    private fun checkLocationEnabledAndProceed() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showEnableLocationDialog()
        } else {
            getCurrentLocation()
        }
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location")
            .setMessage("Your location services are disabled. Please enable them to get weather for your location.")
            .setPositiveButton("Location Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                loadWeather(unit) // Load with default coordinates
            }
            .show()
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermissions()) return

        try {
            // First try last known location
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { location ->
                updateLocationAndLoadWeather(location)
                return
            }

            // If no last known location, request update
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        updateLocationAndLoadWeather(location)
                        locationManager.removeUpdates(this)
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        showEnableLocationDialog()
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                },
                Looper.getMainLooper()
            )

            // Also try network provider as backup
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        updateLocationAndLoadWeather(location)
                        locationManager.removeUpdates(this)
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                },
                Looper.getMainLooper()
            )

        } catch (e: SecurityException) {
            Toast.makeText(this, "Error accessing location: ${e.message}", Toast.LENGTH_SHORT).show()
            loadWeather(unit)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            loadWeather(unit)
        }
    }

    private fun updateLocationAndLoadWeather(location: Location) {
        lat = location.latitude
        lon = location.longitude
        loadWeather(unit)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationEnabledAndProceed()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied. Using default location.",
                    Toast.LENGTH_SHORT
                ).show()
                loadWeather(unit)
            }
        }
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

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
}