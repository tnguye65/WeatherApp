package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class WeatherData(
    @SerializedName("base") val base: String?,
    @SerializedName("main") val main: Main?,
    @SerializedName("weather") val weather: List<Weather>?,
    @SerializedName("coord") val coord: Coord?,
    @SerializedName("timezone") val timezone: Double?,
    @SerializedName("name") val name: String?,
    @SerializedName("sys") val sys: Sys?
)

data class Coord(
    @SerializedName("lon") val lon: Double?,
    @SerializedName("lat") val lat: Double?
)

data class Main(
    @SerializedName("temp") val temp: Double?,
    @SerializedName("feels_like") val feelsLike: Double?
)

data class Weather(
    @SerializedName("main") val main: String?,
    @SerializedName("description") val description: String?
)

data class Sys(
    @SerializedName("country") val country: String?,
)

