package com.kevin.glasslauncher.weather

/** An hour counts as wet from this chance (%) of rain or snow... */
const val WET_CHANCE = 50

/** ...or from this much forecast precipitation (mm). */
const val WET_MM = 0.2

/**
 * What the next hours hold, for the card's one-line summary. Hours are 0-23 local; a null [stop]
 * means it does not stop within the forecast. [snow] when every wet hour of the spell is snow.
 */
sealed interface RainOutlook {
    data object Dry : RainOutlook
    data class Now(val stop: Int?, val snow: Boolean) : RainOutlook
    data class Later(val start: Int, val stop: Int?, val snow: Boolean) : RainOutlook
}

fun isWet(hour: HourForecast): Boolean = hour.rainChance >= WET_CHANCE || hour.precipitationMm >= WET_MM

/** The first wet spell in [hours] (the next 12, starting with the current hour). */
fun rainOutlook(hours: List<HourForecast>): RainOutlook {
    val start = hours.indexOfFirst(::isWet)
    if (start < 0) return RainOutlook.Dry
    val end = (start until hours.size).firstOrNull { !isWet(hours[it]) }
    val spell = hours.subList(start, end ?: hours.size)
    val snow = spell.all { it.condition == WeatherCondition.Snow }
    val stop = end?.let { hours[it].hour }
    return if (start == 0) RainOutlook.Now(stop, snow) else RainOutlook.Later(hours[start].hour, stop, snow)
}

/** "3 PM" on the 12-hour clock, "15:00" on the 24-hour one, like the status corner's time. */
fun hourLabel(hour: Int, clock24: Boolean): String = if (clock24) {
    "%02d:00".format(hour)
} else {
    val h = hour % 12
    "%d %s".format(if (h == 0) 12 else h, if (hour < 12) "AM" else "PM")
}
