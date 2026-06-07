package com.heartmonitor.wear.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.heartmonitor.wear.MainActivity
import com.heartmonitor.wear.R
import com.heartmonitor.wear.network.AlertApiClient
import com.heartmonitor.wear.network.SettingsStore
import com.heartmonitor.wear.pipeline.PipelineRuntime
import com.heartmonitor.wear.pipeline.SensorSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class MonitoringService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private val runtime = PipelineRuntime()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring active"))

        AppState.update { it.copy(running = true, status = "Monitoring") }

        serviceScope.launch {
            val settings = SettingsStore(applicationContext)
            val alertClient = AlertApiClient(settings.backendUrl(), settings.userId(), settings.region())
            while (isActive) {
                val result = runtime.evaluate(generateWindow())
                val sent = if (result.level >= 2) alertClient.sendAlert(result) else false

                AppState.update {
                    it.copy(
                        riskScore = result.riskScore,
                        level = result.level,
                        status = if (sent) "Alert sent" else "Monitoring",
                        lastRationale = result.rationale,
                        sentAlerts = it.sentAlerts + if (sent) 1 else 0
                    )
                }

                delay(SAMPLE_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        AppState.update { it.copy(running = false, status = "Stopped") }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun generateWindow(): List<SensorSample> {
        val now = System.currentTimeMillis()

        val elevated = Random.nextDouble() > 0.75
        val heartRate = if (elevated) Random.nextDouble(176.0, 192.0) else Random.nextDouble(72.0, 102.0)
        val movement = if (elevated) Random.nextDouble(0.0, 0.12) else Random.nextDouble(0.2, 0.95)
        val spo2 = if (elevated) Random.nextDouble(80.0, 89.0) else Random.nextDouble(94.0, 99.0)

        return listOf(
            SensorSample(now, "heart_rate", heartRate, 0.93),
            SensorSample(now, "movement", movement, 0.95),
            SensorSample(now, "spo2", spo2, 0.91)
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Emergency Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HeartMonitor")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "monitoring"
        private const val NOTIFICATION_ID = 1001
        private const val SAMPLE_INTERVAL_MS = 15000L
    }
}
