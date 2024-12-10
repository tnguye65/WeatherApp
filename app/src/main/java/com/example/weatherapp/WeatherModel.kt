package com.example.weatherapp

import android.content.Context
import android.widget.Toast
import retrofit2.Call

class WeatherModel(private val context: Context, private val api: ApiService) {
    private val key = context.getString(R.string.weather_api_key)
    private var units = "imperial"

    fun getUnits() = units

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
        api.getCurrentWeather(lat, lon, key, units).enqueue(object : retrofit2.Callback<WeatherData> {
            override fun onResponse(call: Call<WeatherData>, response: retrofit2.Response<WeatherData>) {
                val weatherData = response.body()
                if (weatherData != null) {
                    callback(weatherData)
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