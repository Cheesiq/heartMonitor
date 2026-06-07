package com.heartmonitor.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.heartmonitor.wear.monitoring.AppState
import com.heartmonitor.wear.monitoring.MonitoringService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EmergencyScreen(
                    onStart = { startService(Intent(this, MonitoringService::class.java)) },
                    onStop = { stopService(Intent(this, MonitoringService::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun EmergencyScreen(
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val state by AppState.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "HeartMonitor MVP")
        Text(text = "State: ${state.status}", modifier = Modifier.padding(top = 6.dp))
        Text(text = "Risk: ${"%.2f".format(state.riskScore)}")
        Text(text = "Level: ${state.level}")
        Text(text = "Alerts: ${state.sentAlerts}", modifier = Modifier.padding(bottom = 8.dp))

        if (!state.running) {
            Button(onClick = onStart) {
                Text("Start")
            }
        } else {
            Button(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}
