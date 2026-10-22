package com.example.vitaminwidget

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MidnightRefreshReceiver.scheduleNextMidnight(this)

        setContent {
            val context = LocalContext.current
            // Nothing OS 5 Dark Translucent Theme infused with Device Dynamic Accent
            val dynamicBase = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                darkColorScheme()
            }

            val nothingScheme = dynamicBase.copy(
                background = Color(0xFF0B0C0E),
                surface = Color(0xFF15161A),
                onBackground = Color(0xFFF5F5F7),
                onSurface = Color(0xFFF5F5F7)
            )

            MaterialTheme(colorScheme = nothingScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VitaminCalendarScreen()
                }
            }
        }
    }
}

@Composable
fun VitaminCalendarScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var takenToday by remember { mutableStateOf(VitaminRepository.isTakenToday(context)) }
    var transparencyPercent by remember {
        mutableFloatStateOf(VitaminRepository.getTransparencyPercent(context).toFloat())
    }

    val nothingRed = Color(0xFFD71921)
    val deviceAccent = MaterialTheme.colorScheme.primary
    val glassBorder = Color(0x26FFFFFF)
    val glassSurface = lerp(Color(0xFF15161A), deviceAccent, 0.08f)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                takenToday = VitaminRepository.isTakenToday(context)
                transparencyPercent = VitaminRepository.getTransparencyPercent(context).toFloat()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "VITAMIN // OS 5.0",
            fontFamily = FontFamily.Monospace,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF5F5F7)
        )

        // Status Card (Nothing OS Smoked Glass)
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, glassBorder),
            colors = CardDefaults.cardColors(containerColor = glassSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (takenToday) "● DOSE LOGGED" else "● DOSE PENDING",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (takenToday) deviceAccent else nothingRed
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "2X TAP: SUBMIT  ·  3X TAP: RESET",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF9E9EA6)
                    )
                }
                Button(
                    onClick = {
                        takenToday = !takenToday
                        VitaminRepository.setTakenToday(context, takenToday)
                        scope.launch { VitaminWidget.forceRefresh(context) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (takenToday) Color(0xFF26282E) else nothingRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (takenToday) "RESET" else "LOG NOW",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Nothing OS Dot-Matrix Style Calendar Card
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, glassBorder),
            colors = CardDefaults.cardColors(containerColor = glassSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Text("◀", fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                    Text(
                        text = "${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()} // ${currentMonth.year}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Text("▶", fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E96),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val firstDayOfMonth = currentMonth.atDay(1)
                val emptyLeadingDays = firstDayOfMonth.dayOfWeek.value % 7
                val daysInMonth = currentMonth.lengthOfMonth()
                val today = LocalDate.now()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(236.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(emptyLeadingDays) {
                        Box(modifier = Modifier.size(36.dp))
                    }
                    items(daysInMonth) { index ->
                        val date = currentMonth.atDay(index + 1)
                        val isTaken = if (date == today) takenToday else VitaminRepository.isTakenOn(context, date)
                        val isPastMissed = date.isBefore(today) && !isTaken

                        val cellBg = when {
                            isTaken -> deviceAccent.copy(alpha = 0.24f)
                            isPastMissed -> nothingRed.copy(alpha = 0.22f)
                            else -> Color(0xFF202228)
                        }
                        val cellBorder = when {
                            isTaken -> deviceAccent
                            isPastMissed -> nothingRed.copy(alpha = 0.65f)
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cellBg)
                                .border(1.dp, cellBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${index + 1}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when {
                                        isTaken -> "●"
                                        isPastMissed -> "×"
                                        else -> "·"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = when {
                                        isTaken -> deviceAccent
                                        isPastMissed -> nothingRed
                                        else -> Color(0xFF6C6E76)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "● LOGGED   ·   × MISSED DOSE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF9E9EA6),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Smoked Glass Transparency Slider with Live Preview Bar
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, glassBorder),
            colors = CardDefaults.cardColors(containerColor = glassSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SMOKED GLASS TRANSPARENCY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${transparencyPercent.roundToInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = deviceAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = transparencyPercent,
                    onValueChange = { newValue ->
                        transparencyPercent = newValue
                        VitaminRepository.setTransparencyPercent(context, newValue.roundToInt())
                        scope.launch { VitaminWidget.forceRefresh(context) }
                    },
                    valueRange = 0f..100f
                )
            }
        }
    }
}
