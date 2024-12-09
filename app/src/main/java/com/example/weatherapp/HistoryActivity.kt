package com.example.weatherapp

import android.content.SharedPreferences
import android.location.Geocoder
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.*
import java.io.IOException
import java.util.Locale
import android.location.Address

class HistoryActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var database: DatabaseReference
    private val cities = ArrayList<String>()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setup back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Initialize views
        listView = findViewById(R.id.historyListView)
        emptyView = findViewById(R.id.emptyView)

        // Setup adapter with custom layout
        adapter = ArrayAdapter(
            this,
            R.layout.history_list_item,
            cities
        )
        listView.adapter = adapter
        listView.emptyView = emptyView

        // Initialize Firebase and load data
        database = FirebaseDatabase.getInstance().getReference("searched_cities")
        loadHistory()

        // Add item click listener
        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedCity = cities[position]
            geocodeAndUpdateLocation(selectedCity)
        }
    }

    private fun loadHistory() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cities.clear()
                for (child in snapshot.children.toList().reversed()) {
                    val city = child.getValue(String::class.java)
                    if (city != null && !cities.contains(city)) {
                        cities.add(city)
                    }
                }
                prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                val numCities = prefs.getInt("numCities", 13)
                val newCities = cities.take(numCities)
                adapter.clear()
                adapter.addAll(newCities)
                adapter.notifyDataSetChanged()
                emptyView.visibility = if (newCities.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@HistoryActivity,
                    "Error loading history: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun geocodeAndUpdateLocation(cityName: String) {
        // Perform geocoding in a background thread
        GeocodeTask().execute(cityName)
    }

    // AsyncTask to perform geocoding
    inner class GeocodeTask : AsyncTask<String, Void, Pair<Double, Double>?>() {
        override fun doInBackground(vararg params: String?): Pair<Double, Double>? {
            val city = params[0]
            val geocoder = Geocoder(this@HistoryActivity, Locale.getDefault())
            try {
                val addresses: List<Address>? = city?.let { geocoder.getFromLocationName(it, 1) }
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val lat = address.latitude
                    val lon = address.longitude
                    return Pair(lat, lon)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: Pair<Double, Double>?) {
            if (result != null) {
                saveLocationToPreferences(result.first, result.second)
                Toast.makeText(
                    this@HistoryActivity,
                    "Location updated to $result",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // Return to MainActivity
            } else {
                Toast.makeText(
                    this@HistoryActivity,
                    "Unable to find location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveLocationToPreferences(lat: Double, lon: Double) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putFloat("saved_lat", lat.toFloat())
        editor.putFloat("saved_lon", lon.toFloat())
        editor.apply()
    }
}
