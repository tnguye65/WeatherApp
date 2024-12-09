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
    private lateinit var rl: RelativeLayout
    private lateinit var tvCity: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvDesc: TextView
    private lateinit var btnSearch: Button
    private lateinit var btnSettings: Button
    private lateinit var btnHist: Button
    private lateinit var btnCurr: Button
    private lateinit var imgWeather: ImageView
    private val iconUrl = "https://openweathermap.org/img/wn/"
    private var lat = 35.0
    private var lon = 139.0
    private lateinit var unit: String
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadPreferences()
        initializeViews()
        setupButtons()
        setupAds()
        checkLocationPermissionsAndProceed()
    }

    private fun initializeViews() {
        rl = findViewById(R.id.main)
        imgWeather = findViewById(R.id.imgWeather)
        tvCity = findViewById(R.id.tvCity)
        tvTemp = findViewById(R.id.tvTemp)
        tvWeather = findViewById(R.id.tvWeather)
        tvDesc = findViewById(R.id.tvDesc)
        btnSearch = findViewById(R.id.btnSearch)
        btnSettings = findViewById(R.id.btnSettings)
        btnHist = findViewById(R.id.btnHist)
        btnCurr = findViewById(R.id.btnCurr)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private fun setupButtons() {
        btnSearch.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        btnHist.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        btnCurr.setOnClickListener { checkLocationPermissionsAndProceed() }
    }

    private fun setupAds() {
        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder()
            .addKeyword("weather")
            .addKeyword("climate")
            .build())
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
        loadWeather(unit)
    }

    private fun saveLocationToPreferences(lat: Double, lon: Double) {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().apply {
            putFloat("saved_lat", lat.toFloat())
            putFloat("saved_lon", lon.toFloat())
            apply()
        }
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        unit = prefs.getString("temperatureUnit", "fahrenheit").toString()
        window.decorView.setBackgroundColor(when(prefs.getString("themeColor", "White")) {
            "Cyan" -> Color.CYAN
            "Yellow" -> Color.YELLOW
            "LightGray" -> Color.LTGRAY
            else -> Color.WHITE
        })
        prefs.getFloat("saved_lat", Float.MIN_VALUE).let { savedLat ->
            prefs.getFloat("saved_lon", Float.MIN_VALUE).let { savedLon ->
                if (savedLat != Float.MIN_VALUE && savedLon != Float.MIN_VALUE) {
                    lat = savedLat.toDouble()
                    lon = savedLon.toDouble()
                    loadWeather(unit)
                }
            }
        }
    }

    private fun hasLocationPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun checkLocationPermissionsAndProceed() {
        if (!hasLocationPermissions()) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else checkLocationEnabledAndProceed()
    }

    private fun checkLocationEnabledAndProceed() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) showEnableLocationDialog()
        else getCurrentLocation()
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
                loadWeather(unit)
            }
            .show()
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermissions()) return
        try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                updateLocationAndLoadWeather(it)
                return
            }
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    updateLocationAndLoadWeather(location)
                    locationManager.removeUpdates(this)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) { showEnableLocationDialog() }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper())
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            loadWeather(unit)
        }
    }

    private fun updateLocationAndLoadWeather(location: Location) {
        lat = location.latitude
        lon = location.longitude
        saveLocationToPreferences(lat, lon)
        loadWeather(unit)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) checkLocationEnabledAndProceed()
            else {
                Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show()
                loadWeather(unit)
            }
        }
    }

    private fun loadWeather(unit: String) {
        WeatherView(this).apply {
            getModel().setUnits(unit)
            displayCurrentWeather(lat, lon) { weatherInfo ->
                runOnUiThread {
                    val sb = weatherInfo.split("\n")
                    updateWeatherUI(sb)
                    parseCityName(weatherInfo).takeIf { it.isNotEmpty() }?.let { storeCityInFirebase(it) }
                    updateBackground(sb)
                    updateWeatherIcon(sb)
                }
            }
        }
    }

    private fun updateWeatherUI(sb: List<String>) {
        tvCity.text = sb.find { it.startsWith("City: ") }?.substringAfter("City: ")
        tvTemp.text = sb.find { it.startsWith("Temperature: ") }?.substringAfter("Temperature: ")
        tvWeather.text = sb.find { it.startsWith("Weather: ") }?.substringAfter("Weather: ")
        tvDesc.text = sb.find { it.startsWith("Description: ") }?.substringAfter("Description: ")
    }

    private fun updateBackground(sb: List<String>) {
        val timezone = sb.find { it.startsWith("Timezone: ") }?.substringAfter("Timezone: ")?.toDouble() ?: 0.0
        val hour = (Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.HOUR_OF_DAY) + (timezone / 3600).toInt() + 24) % 24
        rl.background = ContextCompat.getDrawable(this, when(hour) {
            in 5..11 -> R.drawable.morning
            in 12..16 -> R.drawable.afternoon
            in 17..20 -> R.drawable.evening
            else -> R.drawable.night
        })
    }

    private fun updateWeatherIcon(sb: List<String>) {
        sb.find { it.startsWith("Icon: ") }?.substringAfter("Icon: ")?.let {
            Glide.with(this).load("$iconUrl$it.png").into(imgWeather)
        } ?: Glide.with(this).load(ContextCompat.getDrawable(this, R.drawable.blank)).into(imgWeather)
    }

    private fun parseCityName(info: String) = info.split("\n")
        .find { it.startsWith("City: ") }
        ?.substringAfter("City: ")
        ?.trim() ?: ""

    private fun storeCityInFirebase(city: String) {
        FirebaseDatabase.getInstance().getReference("searched_cities").push().key?.let {
            FirebaseDatabase.getInstance().getReference("searched_cities").child(it).setValue(city)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
}