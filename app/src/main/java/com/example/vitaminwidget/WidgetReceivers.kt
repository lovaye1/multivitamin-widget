package com.example.vitaminwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WidgetTapReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_WIDGET_TAP = "com.example.vitaminwidget.ACTION_WIDGET_TAP"
        private var debounceJob: Job? = null
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_TAP) return

        val currentTaps = VitaminRepository.incrementTapCount(context)
        val pendingResult = goAsync()

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(350L)
            VitaminRepository.resetTapCount(context)

            when (currentTaps) {
                1 -> {
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(openIntent)
                }
                2 -> {
                    VitaminRepository.setTakenToday(context, true)
                    VitaminWidget().updateAll(context)
                    Toast.makeText(context, "Multivitamin marked as TAKEN! 💊", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    VitaminRepository.setTakenToday(context, false)
                    VitaminWidget().updateAll(context)
                    Toast.makeText(context, "Today's status RESET ↺", Toast.LENGTH_SHORT).show()
                }
            }
            pendingResult.finish()
        }
    }
}

class MidnightRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            VitaminWidget().updateAll(context)
            scheduleNextMidnight(context)
            pendingResult.finish()
        }
    }

    companion object {
        fun scheduleNextMidnight(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MidnightRefreshReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextMidnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
            val triggerMillis = nextMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }
}
