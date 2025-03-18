package com.example.secondsclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.example.secondsclock.ui.theme.SecondsClockTheme
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    enum class DisplayMode { MILLISECONDS, SECONDS, NORMAL_TIME }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecondsClockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClockPreviewScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ClockPreviewScreen(modifier: Modifier = Modifier) {
    var displayMode by remember { mutableStateOf(0) } // 0: Seconds, 1: Milliseconds, 2: HH:mm:ss
    var currentTime by remember { mutableStateOf(getFormattedTime(displayMode)) }

    // Update clock every frame
    LaunchedEffect(displayMode) {
        while (true) {
            currentTime = getFormattedTime(displayMode)
            delay(
                when (displayMode) {
                    1 -> 10L  // Milliseconds update every 10ms
                    0 -> 500L // Seconds update twice per second
                    2 -> 1000L // Normal time update every second
                    else -> 500L
                }
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Seconds Clock Widget", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Widget preview
        Box(
            modifier = Modifier
                .size(200.dp, 100.dp)
                .background(Color(0xFF000000), shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { displayMode = (displayMode + 1) % 3 }) {
                Text("Change Mode")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Long press home screen → Widgets → Add 'Seconds Clock'",
            fontSize = 16.sp
        )
    }
}

fun getFormattedTime(mode: Int): String {
    val now = LocalTime.now()
    val seconds = now.toSecondOfDay()
    val milliseconds = now.toNanoOfDay() / 1_000_000 % 1000

    val displayText = when (mode) {
        1-> "${seconds * 1000 + milliseconds}"
        0 -> "$seconds"
        2 -> now.toString()
        else -> ""

    }
    return displayText;
}
