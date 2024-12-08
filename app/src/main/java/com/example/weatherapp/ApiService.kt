package com.example.weatherapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("data/2.5/weather")
    fun getCurrentWeather(
        @Query("lat") lat : Double,
        @Query("lon") lon : Double,
        @Query("appid") apiKey : String,
        @Query("units") units : String
    ) : Call<WeatherData>
}