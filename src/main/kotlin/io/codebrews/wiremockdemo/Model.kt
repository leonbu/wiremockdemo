package io.codebrews.wiremockdemo

import com.fasterxml.jackson.annotation.JsonProperty

data class CityId(val cityId: String)

data class Temperature(
    val temp: Double,
    val pressure: Int,
    val humidity: Int,
    val temp_min: Double,
    val temp_max: Double,
    val feels_like: Double
)

data class CurrentWeather(@JsonProperty("main") var details: Temperature)
