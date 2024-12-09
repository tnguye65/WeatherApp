package com.example.weatherapp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.DateFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.TimeZone
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class MainActivity : AppCompatActivity() {
    private lateinit var rl : RelativeLayout
    private lateinit var city: TextView
    private lateinit var temperature: TextView
    private lateinit var weather: TextView
    private lateinit var weatherDesc: TextView
    private lateinit var searchButton: Button
    private lateinit var settingsButton: Button
    private lateinit var historyButton: Button
    private lateinit var currButton: Button
    private lateinit var iconView : ImageView
    private val iconUrl = "https://openweathermap.org/img/wn/"
    private var lat: Double = 35.0
    private var lon: Double = 139.0
    private lateinit var unit: String
    private lateinit var locationManager: LocationManager
//    private lateinit var prefs: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadPreferences()

        rl = findViewById(R.id.main)
        iconView = findViewById(R.id.weatherIcon)

        // Weather Info
        city = findViewById(R.id.city)
        temperature = findViewById(R.id.temperature)
        weather = findViewById(R.id.weather)
        weatherDesc = findViewById(R.id.weatherDesc)

        searchButton = findViewById(R.id.searchButton)
        settingsButton = findViewById(R.id.settingsButton)
        historyButton = findViewById(R.id.historyButton)
        currButton = findViewById(R.id.currButton)
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

        currButton.setOnClickListener{checkLocationPermissionsAndProceed()}


        var adView: AdView = findViewById(R.id.ad_view)
        var builder : AdRequest.Builder = AdRequest.Builder()
        builder.addKeyword("weather").addKeyword("climate")
        var request : AdRequest = builder.build()
        adView.loadAd(request)

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
                val sb = weatherInfo.split("\n")
                city.text = sb.find { it.startsWith("City: ") }?.substringAfter("City: ")
                temperature.text = sb.find { it.startsWith("Temperature: ") }?.substringAfter("Temperature: ")
                weather.text = sb.find { it.startsWith("Weather: ") }?.substringAfter("Weather: ")
                weatherDesc.text = sb.find { it.startsWith("Description: ") }?.substringAfter("Description: ")

                val cityName = parseCityName(weatherInfo)
                if (cityName.isNotEmpty()) {
                    storeCityInFirebase(cityName)
                }

                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val timezone = sb.find { it.startsWith("Timezone: ") }?.substringAfter("Timezone: ")
                val timezoneShiftSeconds = timezone?.toDouble() ?: 0.0
                val timezoneShiftHours = (timezoneShiftSeconds / 3600).toInt()
                val hour = (calendar.get(Calendar.HOUR_OF_DAY) + timezoneShiftHours + 24) % 24
                when (hour) {
                    in 5..11 -> rl.background = ContextCompat.getDrawable(this, R.drawable.morning)  // 5 AM to 11:59 AM
                    in 12..16 -> rl.background = ContextCompat.getDrawable(this, R.drawable.afternoon) // 12 PM to 4:59 PM
                    in 17..20 -> rl.background = ContextCompat.getDrawable(this, R.drawable.evening)  // 5 PM to 8:59 PM
                    else -> rl.background = ContextCompat.getDrawable(this, R.drawable.night)        // 9 PM to 4:59 AM
                }

                val iconId = sb.find { it.startsWith("Icon: ") }?.substringAfter("Icon: ")
                if (iconId != null) {
                    val url = iconUrl + iconId + ".png"
                    Glide.with(this).load(url).into(iconView)
                } else {
                    Glide.with(this).load(ContextCompat.getDrawable(this, R.drawable.blank)).into(iconView)
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