package com.example.weatherapp

import android.content.Context

class WeatherView() {
    private lateinit var model : WeatherModel
    constructor(context: Context) : this() {
        model = WeatherModel(context, ApiClient().getClient().create(ApiService::class.java))
    }

    fun displayCurrentWeather(lat: Double, lon: Double, callback: (String) -> Unit) {
        model.getCurrentWeather(lat, lon) { weatherData ->
            val weatherInfo =
                "City: ${weatherData.name}\n" +
                        "Country: ${weatherData.sys?.country}\n" +
                        "Weather: ${weatherData.weather?.get(0)?.main}\n" +
                        "Description: ${weatherData.weather?.get(0)?.description}\n" +
                        "Temperature: ${weatherData.main?.temp}°F" // Fahrenheit for now
            callback(weatherInfo)
        }
    }
}