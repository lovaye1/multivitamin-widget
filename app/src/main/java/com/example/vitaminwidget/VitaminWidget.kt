package com.example.vitaminwidget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class VitaminWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isTaken = VitaminRepository.isTakenToday(context)
        val transparencyPercent = VitaminRepository.getTransparencyPercent(context)
        val opacityFraction = (100 - transparencyPercent) / 100f

        provideContent {
            GlanceTheme {
                WidgetContent(
                    context = context,
                    isTaken = isTaken,
                    opacity = opacityFraction
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        isTaken: Boolean,
        opacity: Float
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

        val tapIntent = Intent(context, WidgetTapReceiver::class.java).apply {
            action = WidgetTapReceiver.ACTION_WIDGET_TAP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(ColorProvider(bgColorWithAlpha))
                .clickable(actionSendBroadcast(tapIntent))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompact1x1 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isTaken) "💊✅" else "💊❗",
                            style = TextStyle(fontSize = 22.sp)
                        )
                        Text(
                            text = if (isTaken) "Done" else "Take!",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                isWide2x1 -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isTaken) "✅" else "💊",
                            style = TextStyle(fontSize = 26.sp)
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Column {
                            Text(
                                text = "Multivitamin",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = if (isTaken) "Taken today" else "Not taken yet",
                                style = TextStyle(color = contentColor, fontSize = 12.sp)
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTaken) "💊 ✅" else "💊 ⏰",
                            style = TextStyle(fontSize = if (isTall1x2) 26.sp else 34.sp)
                        )
                        Spacer(GlanceModifier.height(6.dp))
                        Text(
                            text = "Daily Vitamin",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = if (isTall1x2) 13.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = if (isTaken) "Taken Today!" else "Tap 2x to Submit",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        if (!isTall1x2) {
                            Spacer(GlanceModifier.height(4.dp))
                            Text(
                                text = "1x: Calendar • 3x: Reset",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
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
