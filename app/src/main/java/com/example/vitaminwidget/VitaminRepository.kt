package com.example.vitaminwidget

import android.content.Context
import java.time.LocalDate

object VitaminRepository {
    private const val PREFS_NAME = "vitamin_prefs"
    private const val KEY_TAKEN_DATES = "taken_dates"
    private const val KEY_ALPHA_PERCENT = "alpha_percent"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun todayString(): String = LocalDate.now().toString()

    fun isTakenOn(context: Context, date: LocalDate): Boolean {
        val dates = prefs(context).getStringSet(KEY_TAKEN_DATES, emptySet()) ?: emptySet()
        return dates.contains(date.toString())
    }

    fun isTakenToday(context: Context): Boolean = isTakenOn(context, LocalDate.now())

    fun setTakenOn(context: Context, date: LocalDate, taken: Boolean) {
        val current = prefs(context).getStringSet(KEY_TAKEN_DATES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val dateStr = date.toString()
        if (taken) current.add(dateStr) else current.remove(dateStr)
        prefs(context).edit().putStringSet(KEY_TAKEN_DATES, current).commit()
    }

    fun toggleDate(context: Context, date: LocalDate): Boolean {
        val newState = !isTakenOn(context, date)
        setTakenOn(context, date, newState)
        return newState
    }

    fun setTakenToday(context: Context, taken: Boolean) {
        setTakenOn(context, LocalDate.now(), taken)
    }

    fun getTransparencyPercent(context: Context): Int {
        return prefs(context).getInt(KEY_ALPHA_PERCENT, 22)
    }

    fun setTransparencyPercent(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_ALPHA_PERCENT, percent.coerceIn(0, 100)).commit()
    }
}
