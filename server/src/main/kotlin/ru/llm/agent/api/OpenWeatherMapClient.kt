package ru.llm.agent.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Клиент для работы с OpenWeatherMap API.
 * Документация: https://openweathermap.org/current
 */
class OpenWeatherMapClient(
    private val apiKey: String
) {
    private val baseUrl = "https://api.openweathermap.org/data/2.5"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * Получает текущую погоду для указанного города
     */
    suspend fun getCurrentWeather(
        city: String,
        units: String = "metric", // metric = Celsius, imperial = Fahrenheit
        lang: String = "ru"
    ): WeatherResponse? {
        return try {
            val response = client.get("$baseUrl/weather") {
                parameter("q", city)
                parameter("appid", apiKey)
                parameter("units", units)
                parameter("lang", lang)
            }

            response.body<WeatherResponse>()
        } catch (e: Exception) {
            println("Ошибка при получении погоды для $city: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Получает текущую погоду по координатам
     */
    suspend fun getCurrentWeatherByCoordinates(
        lat: Double,
        lon: Double,
        units: String = "metric",
        lang: String = "ru"
    ): WeatherResponse? {
        return try {
            val response = client.get("$baseUrl/weather") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("appid", apiKey)
                parameter("units", units)
                parameter("lang", lang)
            }

            response.body<WeatherResponse>()
        } catch (e: Exception) {
            println("Ошибка при получении погоды по координатам ($lat, $lon): ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Получает прогноз погоды на 5 дней (3-х часовые интервалы)
     */
    suspend fun getForecast(
        city: String,
        units: String = "metric",
        lang: String = "ru"
    ): ForecastResponse? {
        return try {
            val response = client.get("$baseUrl/forecast") {
                parameter("q", city)
                parameter("appid", apiKey)
                parameter("units", units)
                parameter("lang", lang)
            }

            response.body<ForecastResponse>()
        } catch (e: Exception) {
            println("Ошибка при получении прогноза для $city: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun close() {
        client.close()
    }

    /**
     * Ответ API с информацией о текущей погоде
     */
    @Serializable
    data class WeatherResponse(
        val coord: Coordinates? = null,
        val weather: List<Weather>,
        val main: Main,
        val wind: Wind? = null,
        val clouds: Clouds? = null,
        val rain: Rain? = null,
        val snow: Snow? = null,
        val dt: Long,
        val sys: Sys? = null,
        val timezone: Int? = null,
        val id: Long,
        val name: String,
        val cod: Int
    )

    /**
     * Прогноз погоды
     */
    @Serializable
    data class ForecastResponse(
        val cod: String,
        val message: Int? = null,
        val cnt: Int,
        val list: List<ForecastItem>,
        val city: City
    )

    @Serializable
    data class ForecastItem(
        val dt: Long,
        @SerialName("dt_txt")
        val dtTxt: String,
        val main: Main,
        val weather: List<Weather>,
        val wind: Wind? = null,
        val clouds: Clouds? = null,
        val rain: Rain? = null,
        val snow: Snow? = null
    )

    @Serializable
    data class City(
        val id: Long,
        val name: String,
        val coord: Coordinates,
        val country: String,
        val timezone: Int? = null,
        val sunrise: Long? = null,
        val sunset: Long? = null
    )

    @Serializable
    data class Coordinates(
        val lon: Double,
        val lat: Double
    )

    @Serializable
    data class Weather(
        val id: Int,
        val main: String,
        val description: String,
        val icon: String
    )

    @Serializable
    data class Main(
        val temp: Double,
        @SerialName("feels_like")
        val feelsLike: Double,
        @SerialName("temp_min")
        val tempMin: Double,
        @SerialName("temp_max")
        val tempMax: Double,
        val pressure: Int,
        val humidity: Int,
        @SerialName("sea_level")
        val seaLevel: Int? = null,
        @SerialName("grnd_level")
        val grndLevel: Int? = null
    )

    @Serializable
    data class Wind(
        val speed: Double,
        val deg: Int? = null,
        val gust: Double? = null
    )

    @Serializable
    data class Clouds(
        val all: Int
    )

    @Serializable
    data class Rain(
        @SerialName("1h")
        val oneHour: Double? = null,
        @SerialName("3h")
        val threeHours: Double? = null
    )

    @Serializable
    data class Snow(
        @SerialName("1h")
        val oneHour: Double? = null,
        @SerialName("3h")
        val threeHours: Double? = null
    )

    @Serializable
    data class Sys(
        val country: String? = null,
        val sunrise: Long? = null,
        val sunset: Long? = null
    )
}