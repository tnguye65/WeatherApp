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

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_history)
        val t = findViewById<Toolbar>(R.id.tbar)
        setSupportActionBar(t)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        lvHist = findViewById(R.id.lvHist)
        tvEmpty = findViewById(R.id.tvEmpty)
        adapter = ArrayAdapter(this, R.layout.history_list_item, cities)
        lvHist.adapter = adapter
        lvHist.emptyView = tvEmpty
        database = FirebaseDatabase.getInstance().getReference("searched_cities")
        loadHistory()
        lvHist.setOnItemClickListener { _, _, pos, _ ->
            geocodeAndUpdateLocation(cities[pos])
        }
    }

    private fun loadHistory() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                cities.clear()
                val snaps = s.children.toList().reversed()
                for (snap in snaps) {
                    val city = snap.getValue(String::class.java)
                    if (city != null && !cities.contains(city)) {
                        cities.add(city)
                    }
                }
                prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                val n = cities.take(prefs.getInt("numCities", 13))
                adapter.clear()
                adapter.addAll(n)
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (n.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(e: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun geocodeAndUpdateLocation(c: String) = GeocodeTask().execute(c)

    inner class GeocodeTask : AsyncTask<String, Void, Pair<Double, Double>?>() {
        override fun doInBackground(vararg p: String?): Pair<Double, Double>? {
            return try {
                val city = p[0]
                if (city != null) {
                    val addr = Geocoder(this@HistoryActivity, Locale.getDefault()).getFromLocationName(city, 1)
                    if (addr != null && addr.size > 0) {
                        Pair(addr[0].latitude, addr[0].longitude)
                    } else null
                } else null
            } catch (e: IOException) { null }
        }

        override fun onPostExecute(r: Pair<Double, Double>?) {
            if (r != null) {
                saveLocationToPreferences(r.first, r.second)
                Toast.makeText(this@HistoryActivity, "Location updated to $r", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@HistoryActivity, "Unable to find location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveLocationToPreferences(lat: Double, lon: Double) {
        val e = getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
        e.putFloat("saved_lat", lat.toFloat())
        e.putFloat("saved_lon", lon.toFloat())
        e.apply()
    }
}