package com.example.weatherapp

import android.content.Context

class WeatherView() {
    private lateinit var model : WeatherModel
    constructor(context: Context) : this() {
        model = WeatherModel(context, ApiClient().getClient().create(ApiService::class.java))
    }

    fun getModel() : WeatherModel{
        return model
    }

    fun getUnitAbbreviation() : String {
        val unit = model.getUnits()
        if (unit == "metric") {
            return "°C"
        } else if (unit == "standard") {
            return "K"
        } else {
            return "°F"
        }
    }

    fun displayCurrentWeather(lat: Double, lon: Double, callback: (String) -> Unit) {
        model.getCurrentWeather(lat, lon) { weatherData ->
            val mainData = weatherData.main
            val weatherDesc = weatherData.weather?.get(0)
            val city = weatherData.name ?: "Unknown"
            val country = weatherData.sys?.country ?: "N/A"
            val temp = mainData?.temp
            val feelsLike = mainData?.feelsLike
            val humidity = mainData?.humidity
            val wMain = weatherDesc?.main
            val wDescription = weatherDesc?.description
            val iconId = weatherData.weather?.get(0)?.icon
            val timezone = weatherData.timezone
            val sb = StringBuilder()
            sb.append("City: $city\n")
            sb.append("Country: $country\n")
            sb.append("Weather: $wMain\n")
            sb.append("Description: $wDescription\n")
            if (temp != null) sb.append("Temperature: $temp${getUnitAbbreviation()}\n")
            if (feelsLike != null) sb.append("Feels Like: $feelsLike${getUnitAbbreviation()}\n")
            if (humidity != null) sb.append("Humidity: $humidity%\n")
            if (iconId != null) sb.append("Icon: $iconId\n")
            if (timezone != null) sb.append("Timezone: $timezone\n")
            callback(sb.toString())
        }
    }
}
