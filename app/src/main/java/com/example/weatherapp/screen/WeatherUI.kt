package com.example.weatherapp.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.weatherapp.data.Weather
import com.example.weatherapp.data.WeatherResponse
import com.example.weatherapp.retrofit.WeatherApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class WeatherUI : ComponentActivity() {
    private val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private val API_KEY = "a336f6dbee70a1ad0aee6af18924ac4b"

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val retrofit =
            Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create())
                .build()

        val weatherApiService = retrofit.create(WeatherApiService::class.java)

        if (isInternetConnected()) {
            val requestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (isGranted) {
                        fetchLocation(weatherApiService)
                    } else {
                        Toast.makeText(applicationContext, "No Internet", Toast.LENGTH_LONG).show()
                    }
                }

            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                fetchLocation(weatherApiService)
            }
        } else {
            Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchLocation(weatherApiService: WeatherApiService) {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val latitude = location?.latitude ?: 0.0
                val longitude = location?.longitude ?: 0.0
                setContent {
                    WeatherDetails(weatherApiService, API_KEY, latitude, longitude,fetchLocation(weatherApiService))
                }
            }
        }
    }
    private fun isInternetConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

}


@Composable
fun WeatherDetails(
    weatherApiService: WeatherApiService,
    API_KEY: String,
    lat: Double,
    long: Double,
    fetchLocation: Unit,
) {
    var isTemp by remember { mutableStateOf(false) }
    var weatherData by remember { mutableStateOf<WeatherResponse?>(null) }

    LaunchedEffect(true) {
        try {
            val response = weatherApiService.getCurrentWeather(lat, long, API_KEY, "metric")
            weatherData = response
            Log.d("WeatherData", response.toString())
        } catch (e: HttpException) {
            e.code()
        } catch (e: Throwable) {
            e.message
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Weather App",
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(20.dp))
        LatLong(lat, long)
        Spacer(modifier = Modifier.size(20.dp))
        TemperatureDisplay(
            isTemp = isTemp,
            celsius = weatherData?.main?.temp,
            fahrenheit = weatherData?.main?.temp?.times(9.0 / 5.0)?.plus(32.0)
        )
        Spacer(modifier = Modifier.size(20.dp))

        Text(
            text = "Humidity: ${weatherData?.main?.humidity} g/kg",
        )
        Spacer(modifier = Modifier.size(20.dp))
        Text(
            text = "Wind Speed: ${weatherData?.wind?.speed} Km/Hr",
        )
        Spacer(modifier = Modifier.size(20.dp))
        WeatherConditions(weatherData?.weather)

        Spacer(modifier = Modifier.size(20.dp))

        ToggleTemperatureUnitButton(isTemp = isTemp, onToggle = { isTemp = !isTemp })
        Spacer(modifier = Modifier.size(20.dp))
        RefreshButton(fetchLocation)
    }
}

@Composable
fun LatLong(lat: Double, long: Double) {
    Text(text = "Latitude & Longitude: $lat , $long", modifier = Modifier.padding(bottom = 16.dp))
}

@Composable
fun TemperatureDisplay(isTemp: Boolean, celsius: Double?, fahrenheit: Double?) {
    val temperature = if (isTemp) {
        "Temperature: ${celsius ?: "-"} °C"
    } else {
        "Temperature: ${fahrenheit ?: "-"} F"
    }

    Text(text = temperature, modifier = Modifier.padding(bottom = 16.dp))
}

@Composable
fun WeatherConditions(conditions: List<Weather>?) {
    conditions?.let {
        for (condition in it) {
            Text(
                text = "Weather Condition: ${condition.description}",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun ToggleTemperatureUnitButton(isTemp: Boolean, onToggle: () -> Unit) {
    Button(
        onClick = { onToggle() }, modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(text = if (!isTemp) "Celsius" else "Fahrenheit")
    }
}

@Composable
fun RefreshButton(fetchLocation: Unit) {
    Button(
        onClick = { fetchLocation }, modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(text = "Refresh")
    }
}



