package com.heartmonitor.wear.network

import com.heartmonitor.wear.pipeline.ResolutionResult
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AlertApiClient(
    private val baseUrl: String,
    private val userId: String,
    private val region: String
) {
    fun sendAlert(result: ResolutionResult): Boolean {
        if (baseUrl.isBlank() || userId.isBlank()) return false

        val payload = """
            {
              "user_id": "${escape(userId)}",
              "region": "${escape(region)}",
              "risk_score": ${"%.4f".format(result.riskScore)},
              "level": ${result.level},
              "rationale": [${result.rationale.joinToString(",") { "\"${escape(it)}\"" }}]
            }
        """.trimIndent()

        val url = URL(baseUrl.trimEnd('/') + "/v1/alerts")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        return try {
            OutputStreamWriter(connection.outputStream).use { it.write(payload) }
            val code = connection.responseCode
            code in 200..299
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun escape(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
    }
}
