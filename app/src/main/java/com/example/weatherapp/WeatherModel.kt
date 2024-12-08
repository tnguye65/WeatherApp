package com.example.weatherapp

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import retrofit2.Call

class WeatherModel(private val context: Context, private val api : ApiService) {
    private val key : String
    private var units : String

    init {
        val ai: ApplicationInfo = context.applicationContext.packageManager
            .getApplicationInfo(context.applicationContext.packageName, PackageManager.GET_META_DATA)
        val value = context.getString(R.string.weather_api_key)
        key = value
        units = "imperial"
    }

    fun getUnits() : String {
        return units
    }

    // Invalid string will default to imperial (°F)
    fun setUnits(unit: String?) {
        if (unit == "celsius") {
            units = "metric"
        } else if (unit == "kelvin") {
            units = "standard"
        } else {
            units = "imperial"
        }
    }

    fun getCurrentWeather(lat: Double, lon: Double, callback: (WeatherData) -> Unit) {
        val call = api.getCurrentWeather(lat, lon, key, units)
        call.enqueue(object : retrofit2.Callback<WeatherData> {
            override fun onResponse(call: Call<WeatherData>, response: retrofit2.Response<WeatherData>) {
                if (response.isSuccessful && response.body() != null) {
                    callback(response.body()!!)
                } else {
                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                Toast.makeText(context, "Failed to load weather: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}