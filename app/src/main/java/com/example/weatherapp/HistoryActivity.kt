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
    private lateinit var lvHist: ListView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var database: DatabaseReference
    private val cities = ArrayList<String>()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        val tbar = findViewById<Toolbar>(R.id.tbar)
        setSupportActionBar(tbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        lvHist = findViewById(R.id.lvHist)
        tvEmpty = findViewById(R.id.tvEmpty)
        adapter = ArrayAdapter(this, R.layout.history_list_item, cities)
        lvHist.adapter = adapter
        lvHist.emptyView = tvEmpty
        database = FirebaseDatabase.getInstance().getReference("searched_cities")
        loadHistory()
        lvHist.setOnItemClickListener { _, _, position, _ ->
            geocodeAndUpdateLocation(cities[position])
        }
    }

    private fun loadHistory() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cities.clear()
                snapshot.children.toList().reversed().forEach { child ->
                    child.getValue(String::class.java)?.let { city ->
                        if (!cities.contains(city)) cities.add(city)
                    }
                }
                prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                val newCities = cities.take(prefs.getInt("numCities", 13))
                adapter.clear()
                adapter.addAll(newCities)
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (newCities.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Error loading history: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun geocodeAndUpdateLocation(cityName: String) = GeocodeTask().execute(cityName)

    inner class GeocodeTask : AsyncTask<String, Void, Pair<Double, Double>?>() {
        override fun doInBackground(vararg params: String?): Pair<Double, Double>? {
            return try {
                params[0]?.let { city ->
                    Geocoder(this@HistoryActivity, Locale.getDefault()).getFromLocationName(city, 1)?.firstOrNull()?.let {
                        Pair(it.latitude, it.longitude)
                    }
                }
            } catch (e: IOException) { null }
        }

        override fun onPostExecute(result: Pair<Double, Double>?) {
            result?.let {
                saveLocationToPreferences(it.first, it.second)
                Toast.makeText(this@HistoryActivity, "Location updated to $result", Toast.LENGTH_SHORT).show()
                finish()
            } ?: Toast.makeText(this@HistoryActivity, "Unable to find location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLocationToPreferences(lat: Double, lon: Double) {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().apply {
            putFloat("saved_lat", lat.toFloat())
            putFloat("saved_lon", lon.toFloat())
            apply()
        }
    }
}