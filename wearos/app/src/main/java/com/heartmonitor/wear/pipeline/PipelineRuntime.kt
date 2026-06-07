package com.heartmonitor.wear.pipeline

class PipelineRuntime {
    private val neuralModel = SimpleNeuralModel()
    private val symbolicEngine = SymbolicEngine()
    private val resolver = TruthResolver()

    fun evaluate(samples: List<SensorSample>): ResolutionResult {
        val neural = neuralModel.infer(samples)
        val rules = symbolicEngine.apply(samples)
        return resolver.resolve(neural, rules)
    }
}

private class SimpleNeuralModel {
    fun infer(samples: List<SensorSample>): List<NeuralHypothesis> {
        val heart = samples.find { it.type == "heart_rate" }?.value ?: 0.0
        val spo2 = samples.find { it.type == "spo2" }?.value ?: 100.0
        val confidence = when {
            heart > 180.0 && spo2 < 88.0 -> 0.78
            heart > 170.0 -> 0.62
            else -> 0.22
        }
        return listOf(NeuralHypothesis(eventType = "cardiac_event", confidence = confidence))
    }
}

private class SymbolicEngine {
    fun apply(samples: List<SensorSample>): List<RuleAssertion> {
        val byType = samples.associateBy { it.type }
        val heart = byType["heart_rate"]?.value ?: 0.0
        val movement = byType["movement"]?.value ?: 1.0
        val spo2 = byType["spo2"]?.value ?: 100.0

        val assertions = mutableListOf<RuleAssertion>()

        if (heart > 180.0 && movement < 0.1) {
            assertions += RuleAssertion(
                ruleId = "R_HR_MOVEMENT",
                weight = 0.22,
                explanation = "Very high HR with low movement"
            )
        }

        if (spo2 < 85.0) {
            assertions += RuleAssertion(
                ruleId = "R_SPO2_LOW",
                weight = 0.25,
                explanation = "Critical SpO2"
            )
        }

        if (movement > 0.8) {
            assertions += RuleAssertion(
                ruleId = "R_ACTIVE_STATE",
                weight = -0.10,
                explanation = "High movement weakens resting-risk hypothesis"
            )
        }

        return assertions
    }
}

private class TruthResolver {
    fun resolve(
        neural: List<NeuralHypothesis>,
        rules: List<RuleAssertion>
    ): ResolutionResult {
        val neuralScore = neural.maxOfOrNull { it.confidence } ?: 0.0
        val symbolicScore = rules.sumOf { it.weight }
        val risk = (neuralScore + symbolicScore).coerceIn(0.0, 1.0)

        val level = when {
            risk >= 0.90 -> 4
            risk >= 0.75 -> 3
            risk >= 0.55 -> 2
            risk >= 0.35 -> 1
            else -> 0
        }

        return ResolutionResult(
            riskScore = risk,
            level = level,
            rationale = rules.map { "${it.ruleId}: ${it.explanation}" }
        )
    }
}
