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
import android.widget.*
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
    private val iUrl = "https://openweathermap.org/img/wn/"
    private var lat = 35.0
    private var lon = 139.0
    private lateinit var unit: String
    private lateinit var locMgr: LocationManager

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
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
        locMgr = getSystemService(LOCATION_SERVICE) as LocationManager
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
        val e = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
        e.putFloat("saved_lat", lat.toFloat())
        e.putFloat("saved_lon", lon.toFloat())
        e.apply()
    }

    private fun loadPreferences() {
        val p = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        unit = p.getString("temperatureUnit", "fahrenheit").toString()
        val tc = p.getString("themeColor", "White")
        if (tc == "Cyan") {
            window.decorView.setBackgroundColor(Color.CYAN)
        } else if (tc == "Yellow") {
            window.decorView.setBackgroundColor(Color.YELLOW)
        } else if (tc == "LightGray") {
            window.decorView.setBackgroundColor(Color.LTGRAY)
        } else {
            window.decorView.setBackgroundColor(Color.WHITE)
        }
        val sl = p.getFloat("saved_lat", Float.MIN_VALUE)
        val sn = p.getFloat("saved_lon", Float.MIN_VALUE)
        if (sl != Float.MIN_VALUE && sn != Float.MIN_VALUE) {
            lat = sl.toDouble()
            lon = sn.toDouble()
            loadWeather(unit)
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
        if (!locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) showEnableLocationDialog()
        else getCurrentLocation()
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location")
            .setMessage("Your location services are disabled. Please enable them to get weather for your location.")
            .setPositiveButton("Location Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { d, _ ->
                d.dismiss()
                loadWeather(unit)
            }
            .show()
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermissions()) return
        try {
            val l = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (l != null) {
                updateLocationAndLoadWeather(l)
                return
            }
            val ll = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    updateLocationAndLoadWeather(loc)
                    locMgr.removeUpdates(this)
                }
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) { showEnableLocationDialog() }
                override fun onStatusChanged(p: String?, s: Int, ex: Bundle?) {}
            }
            locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, ll, Looper.getMainLooper())
            locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, ll, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            loadWeather(unit)
        }
    }

    private fun updateLocationAndLoadWeather(l: Location) {
        lat = l.latitude
        lon = l.longitude
        saveLocationToPreferences(lat, lon)
        loadWeather(unit)
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        if (rc == LOCATION_PERMISSION_REQUEST_CODE) {
            if (gr.isNotEmpty() && gr[0] == PackageManager.PERMISSION_GRANTED) checkLocationEnabledAndProceed()
            else {
                Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show()
                loadWeather(unit)
            }
        }
    }

    private fun loadWeather(u: String) {
        val w = WeatherView(this)
        w.getModel().setUnits(u)
        w.displayCurrentWeather(lat, lon) { wi ->
            runOnUiThread {
                val wl = wi.split("\n")
                updateWeatherUI(wl)
                val cn = parseCityName(wi)
                if (cn.isNotEmpty()) {
                    storeCityInFirebase(cn)
                }
                updateBackground(wl)
                updateWeatherIcon(wl)
            }
        }
    }

    private fun updateWeatherUI(wl: List<String>) {
        for (l in wl) {
            if (l.startsWith("City: ")) {
                tvCity.text = l.substringAfter("City: ")
            } else if (l.startsWith("Temperature: ")) {
                tvTemp.text = l.substringAfter("Temperature: ")
            } else if (l.startsWith("Weather: ")) {
                tvWeather.text = l.substringAfter("Weather: ")
            } else if (l.startsWith("Description: ")) {
                tvDesc.text = l.substringAfter("Description: ")
            }
        }
    }

    private fun updateBackground(wl: List<String>) {
        var tz = 0.0
        for (l in wl) {
            if (l.startsWith("Timezone: ")) {
                tz = l.substringAfter("Timezone: ").toDoubleOrNull() ?: 0.0
                break
            }
        }
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val h = (c.get(Calendar.HOUR_OF_DAY) + (tz / 3600).toInt() + 24) % 24
        val br = if (h in 5..11) {
            R.drawable.morning
        } else if (h in 12..16) {
            R.drawable.afternoon
        } else if (h in 17..20) {
            R.drawable.evening
        } else {
            R.drawable.night
        }
        rl.background = ContextCompat.getDrawable(this, br)
    }

    private fun updateWeatherIcon(wl: List<String>) {
        var ic: String? = null
        for (l in wl) {
            if (l.startsWith("Icon: ")) {
                ic = l.substringAfter("Icon: ")
                break
            }
        }
        if (ic != null) {
            Glide.with(this).load("$iUrl$ic.png").into(imgWeather)
        } else {
            Glide.with(this)
                .load(ContextCompat.getDrawable(this, R.drawable.blank))
                .into(imgWeather)
        }
    }

    private fun parseCityName(i: String): String {
        for (l in i.split("\n")) {
            if (l.startsWith("City: ")) {
                return l.substringAfter("City: ").trim()
            }
        }
        return ""
    }

    private fun storeCityInFirebase(c: String) {
        val db = FirebaseDatabase.getInstance()
        val r = db.getReference("searched_cities")
        val k = r.push().key
        if (k != null) {
            r.child(k).setValue(c)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
}