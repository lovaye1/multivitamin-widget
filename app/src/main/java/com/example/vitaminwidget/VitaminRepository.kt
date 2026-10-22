package com.example.vitaminwidget

import android.content.Context
import java.time.LocalDate

object VitaminRepository {
    private const val PREFS_NAME = "vitamin_prefs"
    private const val KEY_TAKEN_DATES = "taken_dates"
    private const val KEY_ALPHA_PERCENT = "alpha_percent"
    private const val KEY_TAP_COUNT = "tap_count"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun todayString(): String = LocalDate.now().toString()

    fun isTakenOn(context: Context, date: LocalDate): Boolean {
        val dates = prefs(context).getStringSet(KEY_TAKEN_DATES, emptySet()) ?: emptySet()
        return dates.contains(date.toString())
    }

    fun isTakenToday(context: Context): Boolean = isTakenOn(context, LocalDate.now())

    fun setTakenToday(context: Context, taken: Boolean) {
        val current = prefs(context).getStringSet(KEY_TAKEN_DATES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val today = todayString()
        if (taken) current.add(today) else current.remove(today)
        prefs(context).edit().putStringSet(KEY_TAKEN_DATES, current).apply()
    }

    fun getTransparencyPercent(context: Context): Int {
        return prefs(context).getInt(KEY_ALPHA_PERCENT, 15)
    }

    fun setTransparencyPercent(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_ALPHA_PERCENT, percent.coerceIn(0, 100)).apply()
    }

    fun incrementTapCount(context: Context): Int {
        val next = prefs(context).getInt(KEY_TAP_COUNT, 0) + 1
        prefs(context).edit().putInt(KEY_TAP_COUNT, next).apply()
        return next
    }

    fun resetTapCount(context: Context) {
        prefs(context).edit().putInt(KEY_TAP_COUNT, 0).apply()
    }
}
