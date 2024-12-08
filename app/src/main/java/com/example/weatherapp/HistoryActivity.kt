package com.example.weatherapp
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var database: DatabaseReference
    private val cities = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        listView = findViewById(R.id.historyListView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cities)
        listView.adapter = adapter
        database = FirebaseDatabase.getInstance().getReference("searched_cities")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cities.clear()
                for (child in snapshot.children) {
                    val city = child.getValue(String::class.java)
                    if (city != null) cities.add(city)
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Error loading history", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
