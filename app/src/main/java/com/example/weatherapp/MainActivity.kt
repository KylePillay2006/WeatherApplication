package com.example.weatherapp

import android.animation.*
import android.os.Bundle
import android.view.View
import android.view.animation.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // 🔑 API CONFIG
    private val CITY = "durban,za"
    private val API = "c9d6dfe99369f8b87100803c5f130b6e"

    // ─── Views ─────────────────────────────
    private lateinit var tvAddress: TextView
    private lateinit var tvUpdatedAt: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvTempMin: TextView
    private lateinit var tvTempMax: TextView
    private lateinit var tvSunrise: TextView
    private lateinit var tvSunset: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvInfo: TextView
    private lateinit var pulseDot: View

    private var currentTemp = 0

    // ───────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        applyInsets()
        bindViews()

        fetchWeather()   // 🔥 REAL API CALL

        runEntryAnimations()
        startPulseDot()
    }

    // ─── Insets ────────────────────────────
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    // ─── Bind Views ────────────────────────
    private fun bindViews() {
        tvAddress   = findViewById(R.id.address)
        tvUpdatedAt = findViewById(R.id.updated_at)
        tvStatus    = findViewById(R.id.status)
        tvTemp      = findViewById(R.id.temp)
        tvTempMin   = findViewById(R.id.temp_min)
        tvTempMax   = findViewById(R.id.temp_max)
        tvSunrise   = findViewById(R.id.sunrise)
        tvSunset    = findViewById(R.id.sunset)
        tvWind      = findViewById(R.id.wind)
        tvPressure  = findViewById(R.id.pressure)
        tvHumidity  = findViewById(R.id.humidity)
        tvInfo      = findViewById(R.id.info)
        pulseDot    = findViewById(R.id.pulseDot)
    }

    // ─── API FETCH ─────────────────────────
    private fun fetchWeather() {
        Thread {
            try {
                val response = URL(
                    "https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API"
                ).readText()

                val json = JSONObject(response)

                val main = json.getJSONObject("main")
                val sys = json.getJSONObject("sys")
                val wind = json.getJSONObject("wind")
                val weather = json.getJSONArray("weather").getJSONObject(0)

                val temp = main.getDouble("temp").toInt()
                val tempMin = main.getDouble("temp_min").toInt()
                val tempMax = main.getDouble("temp_max").toInt()
                val pressure = main.getInt("pressure")
                val humidity = main.getInt("humidity")

                val sunrise = sys.getLong("sunrise")
                val sunset = sys.getLong("sunset")

                val windSpeed = (wind.getDouble("speed") * 3.6).toInt() // m/s → km/h

                val cityName = json.getString("name")
                val country = sys.getString("country")

                val description = weather.getString("description")
                    .replaceFirstChar { it.uppercase() }

                val updatedAt = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
                    .format(Date(json.getLong("dt") * 1000))

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

                val sunriseTime = timeFormat.format(Date(sunrise * 1000))
                val sunsetTime = timeFormat.format(Date(sunset * 1000))

                // 🔥 Update UI
                runOnUiThread {
                    updateWeather(
                        "$cityName, $country",
                        updatedAt,
                        description,
                        temp,
                        tempMin,
                        tempMax,
                        sunriseTime,
                        sunsetTime,
                        windSpeed,
                        pressure,
                        humidity
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // ─── UPDATE UI ─────────────────────────
    fun updateWeather(
        city: String,
        updatedAt: String,
        condition: String,
        temp: Int,
        minT: Int,
        maxT: Int,
        sunrise: String,
        sunset: String,
        windKmh: Int,
        pressureHpa: Int,
        humidityPct: Int
    ) {
        tvAddress.text   = city
        tvUpdatedAt.text = "Updated at: $updatedAt"
        tvStatus.text    = condition.uppercase()

        tvTempMin.text   = "${minT}°C"
        tvTempMax.text   = "${maxT}°C"
        tvSunrise.text   = sunrise
        tvSunset.text    = sunset
        tvWind.text      = "$windKmh km/h"
        tvPressure.text  = "$pressureHpa hPa"
        tvHumidity.text  = "$humidityPct%"

        // 🔥 Animate temperature
        ValueAnimator.ofInt(currentTemp, temp).apply {
            duration = 800
            interpolator = OvershootInterpolator(0.6f)
            addUpdateListener {
                tvTemp.text = "${it.animatedValue}°"
            }
            start()
        }

        currentTemp = temp
    }

    // ─── ANIMATIONS ────────────────────────
    private fun runEntryAnimations() {
        val header = findViewById<View>(R.id.headerSection)
        val overview = tvTemp.parent.parent as View
        val row1 = tvSunrise.parent.parent as View
        val row2 = tvPressure.parent.parent as View

        listOf(header, overview, row1, row2).forEach {
            it.translationY = 60f
            it.alpha = 0f
        }

        animateIn(header, 0)
        animateIn(overview, 150)
        animateIn(row1, 350)
        animateIn(row2, 450)
    }

    private fun animateIn(view: View, delay: Long) {
        val move = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 60f, 0f)
        val fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)

        AnimatorSet().apply {
            playTogether(move, fade)
            duration = 600
            startDelay = delay
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    // ─── Pulse Animation ───────────────────
    private fun startPulseDot() {
        val scaleX = ObjectAnimator.ofFloat(pulseDot, View.SCALE_X, 1f, 2.2f)
        val scaleY = ObjectAnimator.ofFloat(pulseDot, View.SCALE_Y, 1f, 2.2f)
        val alpha = ObjectAnimator.ofFloat(pulseDot, View.ALPHA, 0.8f, 0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1400
            scaleX.repeatCount = ValueAnimator.INFINITE
            scaleY.repeatCount = ValueAnimator.INFINITE
            alpha.repeatCount = ValueAnimator.INFINITE
            start()
        }
    }
}