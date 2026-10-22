package com.example.vitaminwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WidgetTapReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_WIDGET_TAP = "com.example.vitaminwidget.ACTION_WIDGET_TAP"
        private const val MULTI_TAP_WINDOW_MS = 550L
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_TAP) return

        val appContext = context.applicationContext
        val (tapCount, myTapTimestamp) = VitaminRepository.recordTap(appContext, MULTI_TAP_WINDOW_MS)
        val pendingResult = goAsync()

        scope.launch {
            try {
                // Wait for the multi-tap window to finish
                delay(MULTI_TAP_WINDOW_MS)

                // If another tap happened after this one, abort this earlier tap!
                if (VitaminRepository.getLastTapTime(appContext) != myTapTimestamp) {
                    return@launch
                }

                // No newer tap arrived! Clear counter and run the exact action for tapCount
                VitaminRepository.clearTaps(appContext)

                when (tapCount) {
                    1 -> {
                        val openIntent = Intent(appContext, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        appContext.startActivity(openIntent)
                    }
                    2 -> {
                        VitaminRepository.setTakenToday(appContext, true)
                        VitaminWidget.forceRefresh(appContext)
                        Toast.makeText(appContext, "Multivitamin marked as TAKEN! 💊", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        VitaminRepository.setTakenToday(appContext, false)
                        VitaminWidget.forceRefresh(appContext)
                        Toast.makeText(appContext, "Today's status RESET ↺", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class MidnightRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VitaminWidget.forceRefresh(appContext)
                scheduleNextMidnight(appContext)
            } finally {
                pendingResult.finish()
            }
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
