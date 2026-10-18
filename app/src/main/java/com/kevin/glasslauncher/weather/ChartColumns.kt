package com.kevin.glasslauncher.weather

/** Rain chances below this (%) get a bar but no label, so the chart stays quiet on dry days. */
const val CHANCE_LABEL_MIN = 10

/** One column of the hourly chart, ready to draw: [chance] is the bar height, 0 to 1. */
data class ChartColumn(
    val hourLabel: String,
    val temperature: String,
    val chanceLabel: String?,
    val chance: Float,
    val condition: WeatherCondition,
)

/** One column per forecast hour, at most [FORECAST_HOURS]; hour labels follow the clock setting. */
fun chartColumns(hours: List<HourForecast>, clock24: Boolean): List<ChartColumn> =
    hours.take(FORECAST_HOURS).map { h ->
        ChartColumn(
            hourLabel = hourLabel(h.hour, clock24),
            temperature = temperatureText(h.temperature),
            chanceLabel = if (h.rainChance >= CHANCE_LABEL_MIN) "${h.rainChance}%" else null,
            chance = h.rainChance / 100f,
            condition = h.condition,
        )
    }

/** The day's rain chance label, or null when under [CHANCE_LABEL_MIN]. */
fun dayChanceLabel(day: DayForecast): String? = if (day.rainChance >= CHANCE_LABEL_MIN) "${day.rainChance}%" else null
