package com.example.weatherapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var database: DatabaseReference
    private val cities = ArrayList<String>()

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
    }

    private fun loadHistory() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cities.clear()
                for (child in snapshot.children) {
                    val city = child.getValue(String::class.java)
                    if (city != null && !cities.contains(city)) {
                        cities.add(city)
                    }
                }
                cities.sortDescending() // Show newest first
                adapter.notifyDataSetChanged()

                // Show/hide empty view
                emptyView.visibility = if (cities.isEmpty()) View.VISIBLE else View.GONE
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
}