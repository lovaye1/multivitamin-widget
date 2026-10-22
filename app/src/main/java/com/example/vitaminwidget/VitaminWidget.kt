package com.example.vitaminwidget

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

class VitaminWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        private val REFRESH_TICK_KEY = longPreferencesKey("refresh_tick")

        suspend fun forceRefresh(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(VitaminWidget::class.java)
            val now = System.currentTimeMillis()
            glanceIds.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[REFRESH_TICK_KEY] = now
                    }
                }
                VitaminWidget().update(context, id)
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val tick = prefs[REFRESH_TICK_KEY] ?: 0L

            val isTaken = VitaminRepository.isTakenToday(context)
            val transparencyPercent = VitaminRepository.getTransparencyPercent(context)
            val opacityFraction = (100 - transparencyPercent) / 100f

            GlanceTheme {
                WidgetContent(
                    context = context,
                    isTaken = isTaken,
                    opacity = opacityFraction,
                    tick = tick
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        isTaken: Boolean,
        opacity: Float,
        tick: Long
    ) {
        val size = LocalSize.current
        val isCompact1x1 = size.width < 110.dp && size.height < 110.dp
        val isWide2x1 = size.width >= 110.dp && size.height < 110.dp
        val isTall1x2 = size.width < 110.dp && size.height >= 110.dp

        val baseContainerColor = if (isTaken) {
            GlanceTheme.colors.primaryContainer.getColor(context)
        } else {
            GlanceTheme.colors.errorContainer.getColor(context)
        }
        val bgColorWithAlpha = baseContainerColor.copy(alpha = opacity)

        val contentColor = if (isTaken) {
            GlanceTheme.colors.onPrimaryContainer
        } else {
            GlanceTheme.colors.onErrorContainer
        }

        // Uses actionRunCallback so taps are never blocked by Android's BroadcastReceiver queue
        val clickModifier = GlanceModifier.clickable(actionRunCallback<WidgetTapCallback>())

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(ColorProvider(bgColorWithAlpha))
                .then(clickModifier)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompact1x1 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = clickModifier
                    ) {
                        Text(
                            text = if (isTaken) "💊✅" else "💊❗",
                            style = TextStyle(fontSize = 22.sp),
                            modifier = clickModifier
                        )
                        Text(
                            text = if (isTaken) "Done" else "Take!",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = clickModifier
                        )
                    }
                }
                isWide2x1 -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = clickModifier
                    ) {
                        Text(
                            text = if (isTaken) "✅" else "💊",
                            style = TextStyle(fontSize = 26.sp),
                            modifier = clickModifier
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Column(modifier = clickModifier) {
                            Text(
                                text = "Multivitamin",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = clickModifier
                            )
                            Text(
                                text = if (isTaken) "Taken today" else "Not taken yet",
                                style = TextStyle(color = contentColor, fontSize = 12.sp),
                                modifier = clickModifier
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = clickModifier
                    ) {
                        Text(
                            text = if (isTaken) "💊 ✅" else "💊 ⏰",
                            style = TextStyle(fontSize = if (isTall1x2) 26.sp else 34.sp),
                            modifier = clickModifier
                        )
                        Spacer(GlanceModifier.height(6.dp))
                        Text(
                            text = "Daily Vitamin",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = if (isTall1x2) 13.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = clickModifier
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = if (isTaken) "Taken Today!" else "Tap 2x to Submit",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = clickModifier
                        )
                        if (!isTall1x2) {
                            Spacer(GlanceModifier.height(4.dp))
                            Text(
                                text = "1x: Calendar • 3x: Reset",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = clickModifier
                            )
                        }
                    }
                }
            }
        }
    }
}

class WidgetTapCallback : ActionCallback {
    companion object {
        private val tapCounter = AtomicInteger(0)
        private var debounceJob: Job? = null
        private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        private const val MULTI_TAP_WINDOW_MS = 650L
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val currentTap = tapCounter.incrementAndGet()

        // Immediately cancel any previous pending tap timer and return right away so Android doesn't block tap #2/#3
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(MULTI_TAP_WINDOW_MS)

            val finalTaps = tapCounter.getAndSet(0)
            when (finalTaps) {
                1 -> {
                    val openIntent = Intent(appContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    appContext.startActivity(openIntent)
                }
                2 -> {
                    VitaminRepository.setTakenToday(appContext, true)
                    VitaminWidget.forceRefresh(appContext)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(appContext, "Multivitamin marked as TAKEN! 💊", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> if (finalTaps >= 3) {
                    VitaminRepository.setTakenToday(appContext, false)
                    VitaminWidget.forceRefresh(appContext)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(appContext, "Today's status RESET ↺", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

class VitaminWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VitaminWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MidnightRefreshReceiver.scheduleNextMidnight(context)
    }
}
