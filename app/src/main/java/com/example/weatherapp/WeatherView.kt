package com.example.weatherapp

import android.content.Context

class WeatherView {
    private lateinit var model: WeatherModel
    constructor()
    constructor(context: Context) : this() {
        model = WeatherModel(context, ApiClient().getClient().create(ApiService::class.java))
    }

    fun getModel() = model

    fun getUnitAbbreviation() = when(model.getUnits()) {
        "metric" -> "°C"
        "standard" -> "K"
        else -> "°F"
    }

    fun displayCurrentWeather(lat: Double, lon: Double, callback: (String) -> Unit) {
        model.getCurrentWeather(lat, lon) { data ->
            StringBuilder().apply {
                append("City: ${data.name ?: "Unknown"}\n")
                append("Country: ${data.sys?.country ?: "N/A"}\n")
                append("Weather: ${data.weather?.get(0)?.main}\n")
                append("Description: ${data.weather?.get(0)?.description}\n")
                data.main?.temp?.let { append("Temperature: $it${getUnitAbbreviation()}\n") }
                data.main?.feelsLike?.let { append("Feels Like: $it${getUnitAbbreviation()}\n") }
                data.main?.humidity?.let { append("Humidity: $it%\n") }
                data.weather?.get(0)?.icon?.let { append("Icon: $it\n") }
                data.timezone?.let { append("Timezone: $it\n") }
                callback(toString())
            }
        }
    }
}