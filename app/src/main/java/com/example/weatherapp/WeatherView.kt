package com.example.weatherapp

import android.content.Context

class WeatherView {
    private lateinit var model: WeatherModel

    constructor()

    constructor(context: Context) : this() {
        val apiService = ApiClient().getClient().create(ApiService::class.java)
        model = WeatherModel(context, apiService)
    }

    fun getModel() = model

    fun getUnitAbbreviation(): String {
        if (model.getUnits() == "metric") {
            return "°C"
        } else if (model.getUnits() == "standard") {
            return "K"
        } else {
            return "°F"
        }
    }

    fun displayCurrentWeather(lat: Double, lon: Double, callback: (String) -> Unit) {
        model.getCurrentWeather(lat, lon) { weatherData ->
            val result = StringBuilder()
            result.append("City: ${weatherData.name ?: "Unknown"}\n")
            result.append("Country: ${weatherData.sys?.country ?: "N/A"}\n")
            result.append("Weather: ${weatherData.weather?.get(0)?.main}\n")
            result.append("Description: ${weatherData.weather?.get(0)?.description}\n")
            if (weatherData.main?.temp != null) {
                result.append("Temperature: ${weatherData.main.temp}${getUnitAbbreviation()}\n")
            }
            if (weatherData.main?.feelsLike != null) {
                result.append("Feels Like: ${weatherData.main.feelsLike}${getUnitAbbreviation()}\n")
            }
            if (weatherData.main?.humidity != null) {
                result.append("Humidity: ${weatherData.main.humidity}%\n")
            }
            if (weatherData.weather?.get(0)?.icon != null) {
                result.append("Icon: ${weatherData.weather[0].icon}\n")
            }
            if (weatherData.timezone != null) {
                result.append("Timezone: ${weatherData.timezone}\n")
            }
            callback(result.toString())
        }
    }
}