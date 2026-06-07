package com.heartmonitor.wear.pipeline

data class SensorSample(
    val timestampMs: Long,
    val type: String,
    val value: Double,
    val quality: Double
)

data class NeuralHypothesis(
    val eventType: String,
    val confidence: Double
)

data class RuleAssertion(
    val ruleId: String,
    val weight: Double,
    val explanation: String
)

data class ResolutionResult(
    val riskScore: Double,
    val level: Int,
    val rationale: List<String>
)
