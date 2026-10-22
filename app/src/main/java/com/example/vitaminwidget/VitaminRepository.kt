package com.example.vitaminwidget

import android.content.Context
import java.time.LocalDate

object VitaminRepository {
    private const val PREFS_NAME = "vitamin_prefs"
    private const val KEY_TAKEN_DATES = "taken_dates"
    private const val KEY_ALPHA_PERCENT = "alpha_percent"
    private const val KEY_TAP_COUNT = "tap_count"
    private const val KEY_LAST_TAP_TIME = "last_tap_time"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun todayString(): String = LocalDate.now().toString()

    fun isTakenOn(context: Context, date: LocalDate): Boolean {
        val dates = prefs(context).getStringSet(KEY_TAKEN_DATES, emptySet()) ?: emptySet()
        return dates.contains(date.toString())
    }

    fun isTakenToday(context: Context): Boolean = isTakenOn(context, LocalDate.now())

    // Uses commit() instead of apply() so changes are guaranteed written before widget redraws
    fun setTakenToday(context: Context, taken: Boolean) {
        val current = prefs(context).getStringSet(KEY_TAKEN_DATES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val today = todayString()
        if (taken) current.add(today) else current.remove(today)
        prefs(context).edit().putStringSet(KEY_TAKEN_DATES, current).commit()
    }

    fun getTransparencyPercent(context: Context): Int {
        return prefs(context).getInt(KEY_ALPHA_PERCENT, 15)
    }

    fun setTransparencyPercent(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_ALPHA_PERCENT, percent.coerceIn(0, 100)).commit()
    }

    // Synchronously records rapid taps and returns (tapCount, tapTimestamp)
    @Synchronized
    fun recordTap(context: Context, windowMs: Long): Pair<Int, Long> {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        val lastTime = p.getLong(KEY_LAST_TAP_TIME, 0L)
        val prevCount = p.getInt(KEY_TAP_COUNT, 0)

        val newCount = if (now - lastTime <= windowMs) prevCount + 1 else 1
        p.edit()
            .putInt(KEY_TAP_COUNT, newCount)
            .putLong(KEY_LAST_TAP_TIME, now)
            .commit()
        return Pair(newCount, now)
    }

    @Synchronized
    fun getLastTapTime(context: Context): Long {
        return prefs(context).getLong(KEY_LAST_TAP_TIME, 0L)
    }

    @Synchronized
    fun clearTaps(context: Context) {
        prefs(context).edit()
            .putInt(KEY_TAP_COUNT, 0)
            .putLong(KEY_LAST_TAP_TIME, 0L)
            .commit()
    }
}
