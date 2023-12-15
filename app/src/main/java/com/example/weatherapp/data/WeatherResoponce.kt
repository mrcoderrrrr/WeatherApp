package com.example.weatherapp.data


data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val main: Main,
    val wind: Wind,
)

data class Coord(
    val lon: Double,
    val lat: Double
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
)

data class Main(
    val temp: Double,
    val humidity: Int,
)

data class Wind(
    val speed: Double,
)
