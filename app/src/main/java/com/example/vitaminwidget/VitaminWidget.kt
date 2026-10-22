package com.example.vitaminwidget

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import androidx.glance.text.FontFamily
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
                NothingGlassWidgetContent(
                    context = context,
                    isTaken = isTaken,
                    opacity = opacityFraction,
                    tick = tick
                )
            }
        }
    }

    @Composable
    private fun NothingGlassWidgetContent(
        context: Context,
        isTaken: Boolean,
        opacity: Float,
        tick: Long
    ) {
        val size = LocalSize.current
        val isCompact1x1 = size.width < 110.dp && size.height < 110.dp
        val isWide2x1 = size.width >= 110.dp && size.height < 110.dp
        val isTall1x2 = size.width < 110.dp && size.height >= 110.dp

        val deviceAccent = GlanceTheme.colors.primary.getColor(context)
        val nothingSmokedBase = Color(0xFF121316)
        val tintedDarkGlass = lerp(nothingSmokedBase, deviceAccent, 0.14f).copy(alpha = opacity)

        // Frosted inner haze layer that simulates blurred glass diffusion over the wallpaper
        val frostedHazeLayer = Color(0xFF22242A).copy(alpha = (opacity * 0.45f).coerceIn(0f, 1f))

        val nothingCrimson = Color(0xFFD71921)
        val statusAccent = if (isTaken) lerp(deviceAccent, Color.White, 0.25f) else nothingCrimson
        val badgeBg = if (isTaken) {
            deviceAccent.copy(alpha = (0.28f * opacity).coerceAtLeast(0.12f))
        } else {
            nothingCrimson.copy(alpha = (0.28f * opacity).coerceAtLeast(0.12f))
        }

        val primaryText = ColorProvider(Color(0xFFF5F5F7))
        val secondaryText = ColorProvider(Color(0xFF9E9EA6))
        val accentProvider = ColorProvider(statusAccent)

        val clickModifier = GlanceModifier.clickable(actionRunCallback<WidgetTapCallback>())

        // Multi-layered Frosted Glass Shell
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(26.dp)
                .background(ColorProvider(tintedDarkGlass))
                .then(clickModifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(26.dp)
                    .background(ColorProvider(frostedHazeLayer))
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ImageProvider(R.drawable.nothing_glass_border))
                        .padding(if (isCompact1x1) 8.dp else 12.dp)
                        .then(clickModifier),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isCompact1x1 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = clickModifier
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .cornerRadius(14.dp)
                                        .background(ColorProvider(badgeBg))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .then(clickModifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTaken) "[OK]" else "[RX]",
                                        style = TextStyle(
                                            color = accentProvider,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = clickModifier
                                    )
                                }
                                Spacer(GlanceModifier.height(6.dp))
                                Text(
                                    text = if (isTaken) "TAKEN" else "TAKE",
                                    style = TextStyle(
                                        color = primaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    modifier = clickModifier
                                )
                            }
                        }

                        isWide2x1 -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = GlanceModifier.fillMaxWidth().then(clickModifier)
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(40.dp)
                                        .cornerRadius(20.dp)
                                        .background(ColorProvider(badgeBg))
                                        .then(clickModifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTaken) "OK" else "RX",
                                        style = TextStyle(
                                            color = accentProvider,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = clickModifier
                                    )
                                }
                                Spacer(GlanceModifier.width(12.dp))
                                Column(modifier = clickModifier) {
                                    Text(
                                        text = "VITAMIN // 01",
                                        style = TextStyle(
                                            color = secondaryText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = clickModifier
                                    )
                                    Text(
                                        text = if (isTaken) "LOGGED TODAY" else "PENDING DOSE",
                                        style = TextStyle(
                                            color = primaryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = clickModifier
                                    )
                                }
                            }
                        }

                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = GlanceModifier.fillMaxSize().then(clickModifier)
                            ) {
                                Text(
                                    text = "DAILY.VITAMIN",
                                    style = TextStyle(
                                        color = secondaryText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = clickModifier
                                )
                                Spacer(GlanceModifier.height(8.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .cornerRadius(20.dp)
                                        .background(ColorProvider(badgeBg))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .then(clickModifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTaken) "[ TAKEN ]" else "[ NOT TAKEN ]",
                                        style = TextStyle(
                                            color = accentProvider,
                                            fontSize = if (isTall1x2) 10.sp else 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = clickModifier
                                    )
                                }
                                Spacer(GlanceModifier.height(8.dp))
                                Text(
                                    text = if (isTaken) "DOSE COMPLETE" else "2X TAP : LOG",
                                    style = TextStyle(
                                        color = primaryText,
                                        fontSize = if (isTall1x2) 11.sp else 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = clickModifier
                                )
                                if (!isTall1x2) {
                                    Spacer(GlanceModifier.height(4.dp))
                                    Text(
                                        text = "1X CAL  /  3X RST",
                                        style = TextStyle(
                                            color = secondaryText,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
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
        tapCounter.incrementAndGet()

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
                        Toast.makeText(appContext, "DOSE LOGGED // TAKEN", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> if (finalTaps >= 3) {
                    VitaminRepository.setTakenToday(appContext, false)
                    VitaminWidget.forceRefresh(appContext)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(appContext, "STATUS RESET // CLEARED", Toast.LENGTH_SHORT).show()
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
