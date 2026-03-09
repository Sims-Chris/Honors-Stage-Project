package com.example.hsp

import java.io.Serializable
import kotlin.math.floor

data class TrainingExercise(
    val name: String,
    var weight: String,
    val sets: String,
    var reps: String,
    val time: String,
    val rest: String
) : Serializable

object TrainingPlanGenerator {

    private val exerciseBank = mapOf(
        "pulling_strength" to listOf(
            TrainingExercise("Weighted pull ups", "60% of max weight", "4", "3", "", "120"),
            TrainingExercise("Assisted one arm pull ups", "add or remove weight as needed", "3", "2", "", "120"),
            TrainingExercise("One arm lock offs", "add or remove weight as needed", "4", "1", "8", "30")
        ),
        "pulling_endurance" to listOf(
            TrainingExercise("Pull ups", "", "8", "DYNAMIC_PULLUP_REP", "", "30"),
            TrainingExercise("French Medies", "", "3", "1", "Max", "60")
        ),
        "finger_strength" to listOf(
            TrainingExercise("Pinch block", "80% of max weight", "2", "4", "", "20"),
            TrainingExercise("Half crimp lift", "80% of max weight", "3", "4", "", "20"),
            TrainingExercise("Max hangs (20 mm)", "100% of max weight", "4", "1", "8", "120"),
            TrainingExercise("One arm hangs (20mm)", "add or remove weight as needed", "2", "1", "8", "30"),
            TrainingExercise("Small edge hangs (12mm)", "", "3", "1", "8", "60")
        ),
        "finger_endurance" to listOf(
            TrainingExercise("Repeaters", "", "2", "10", "7", "3"),
            TrainingExercise("Continuous hang", "bodyweight", "3", "1", "Max", "120")
        ),
        "explosive_power" to listOf(
            TrainingExercise("Campus latches", "", "4", "3", "", "60"),
            TrainingExercise("Campus double dynos", "", "4", "3", "", "90")
        ),
        "antagonist" to listOf(
            TrainingExercise("Push ups", "", "3", "DYNAMIC_PUSHUP_REP", "", "60"),
            TrainingExercise("Overhead Press", "70% of max weight", "3", "8", "", "90")
        )
    )

    fun generatePlanStructured(
        targetGrade: Double,
        sex: Double,
        height: Double,
        weight: Double,
        actuals: Map<String, Double>,
        activeDays: List<String>
    ): Map<String, List<TrainingExercise>> {
        
        val userProfile = doubleArrayOf(targetGrade, sex, height, weight)
        
        val expPullups = BoulderingAI.Pullups.score(userProfile)
        val expWeightedPull = BoulderingAI.WeightedPull.score(userProfile)
        val expMaxHang = BoulderingAI.MaxHang.score(userProfile)
        val expContinuousHang = BoulderingAI.ContinuousHang.score(userProfile)
        val expPowl = BoulderingAI.PowL.score(userProfile)
        val expPowr = BoulderingAI.PowR.score(userProfile)
        val expPushups = BoulderingAI.Pushups.score(userProfile)

        val prescriptions = mutableListOf<String>()

        val pullRepRatio = (actuals["pullups"] ?: 1.0) / expPullups
        val pullWeightRatio = (actuals["weighted_pull"] ?: 1.0) / expWeightedPull
        if (pullWeightRatio > 0.9 && pullRepRatio < 0.8) prescriptions.add("pulling_endurance")
        else if (pullWeightRatio < 0.8 && pullRepRatio > 0.9) prescriptions.add("pulling_strength")
        else if (pullWeightRatio < 0.8 && pullRepRatio < 0.8) prescriptions.add("pulling_strength")

        val hangRatio = (actuals["maxhang"] ?: 1.0) / expMaxHang
        val enduranceRatio = (actuals["continuoushang"] ?: 1.0) / expContinuousHang
        if (hangRatio > 0.9 && enduranceRatio < 0.8) prescriptions.add("finger_endurance")
        else if (hangRatio < 0.8 && enduranceRatio > 0.9) prescriptions.add("finger_strength")
        else if (hangRatio < 0.8 && enduranceRatio < 0.8) prescriptions.add("finger_strength")

        val actualPower = ((actuals["powl"] ?: 1.0) + (actuals["powr"] ?: 1.0)) / 2
        val expectedPower = (expPowl + expPowr) / 2
        if (actualPower < expectedPower * 0.85) prescriptions.add("explosive_power")

        if ((actuals["pushups"] ?: 1.0) < expPushups * 0.8) prescriptions.add("antagonist")

        if (prescriptions.isEmpty()) {
            prescriptions.addAll(listOf("finger_strength", "pulling_strength", "explosive_power", "antagonist"))
        }

        val dynPullups = floor((actuals["pullups"] ?: 1.0) * 0.8).toInt().coerceAtLeast(1)
        val dynPushups = floor((actuals["pushups"] ?: 1.0) * 0.8).toInt().coerceAtLeast(1)
        
        val weight80Finger = floor((actuals["maxhang"] ?: 0.0) * 0.8).toInt()
        val weight100Finger = floor((actuals["maxhang"] ?: 0.0) * 1.0).toInt()
        val weight60Pull = floor((actuals["weighted_pull"] ?: 0.0) * 0.6).toInt()

        val poolOfExercises = mutableListOf<TrainingExercise>()
        for (cat in prescriptions) {
            val available = exerciseBank[cat] ?: emptyList()
            poolOfExercises.addAll(available.shuffled().take(2).map { it.copy() })
        }

        // Safer padding: don't loop infinitely if bank is exhausted
        var attempts = 0
        while (poolOfExercises.size < activeDays.size * 4 && attempts < 100) {
            attempts++
            val extraCat = listOf("finger_strength", "pulling_strength", "antagonist", "finger_endurance").random()
            val extraEx = exerciseBank[extraCat]?.random()?.copy()
            if (extraEx != null && !poolOfExercises.any { it.name == extraEx.name }) {
                poolOfExercises.add(extraEx)
            }
        }
        
        // If we still need more, just allow duplicates but distinct instances
        while (poolOfExercises.size < activeDays.size * 4) {
            val extraCat = exerciseBank.keys.random()
            val extraEx = exerciseBank[extraCat]?.random()?.copy()
            if (extraEx != null) poolOfExercises.add(extraEx)
        }

        val schedule = mutableMapOf<String, MutableList<TrainingExercise>>()
        activeDays.forEach { schedule[it] = mutableListOf() }

        for ((i, exInstance) in poolOfExercises.withIndex()) {
            val day = activeDays[i % activeDays.size]

            if (exInstance.reps == "DYNAMIC_PULLUP_REP") exInstance.reps = dynPullups.toString()
            if (exInstance.reps == "DYNAMIC_PUSHUP_REP") exInstance.reps = dynPushups.toString()

            if (exInstance.weight.contains("80% of max weight") && (exInstance.name == "Pinch block" || exInstance.name == "Half crimp lift")) {
                exInstance.weight = "80% of max weight (${weight80Finger}lbs)"
            } else if (exInstance.weight.contains("100% of max weight")) {
                exInstance.weight = "100% of max weight (${weight100Finger}lbs)"
            } else if (exInstance.weight.contains("60% of max weight")) {
                exInstance.weight = "60% of max weight (${weight60Pull}lbs)"
            }

            schedule[day]?.add(exInstance)
        }

        return schedule
    }

    fun generatePlan(
        targetGrade: Double,
        sex: Double,
        height: Double,
        weight: Double,
        actuals: Map<String, Double>,
        activeDays: List<String>
    ): String {
        val schedule = generatePlanStructured(targetGrade, sex, height, weight, actuals, activeDays)
        val stringBuilder = StringBuilder()
        stringBuilder.append("struct{ Name : weight : sets : reps : time for exercise : rest time\n\n")

        for (day in activeDays) {
            stringBuilder.append("$day[\n")
            val exercisesToday = schedule[day] ?: emptyList()
            for (j in exercisesToday.indices) {
                val ex = exercisesToday[j]
                val comma = if (j < exercisesToday.size - 1) "," else ","
                stringBuilder.append("    ${ex.name} : ${ex.weight} : ${ex.sets} : ${ex.reps} : ${ex.time} : ${ex.rest}$comma\n")
            }
            stringBuilder.append("]\n\n")
        }

        return stringBuilder.toString()
    }
}
