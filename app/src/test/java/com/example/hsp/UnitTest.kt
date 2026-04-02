package com.example.hsp

import org.junit.Test
import org.junit.Assert.*


class AppLogicTestSuite {

    /**
     * AI TEST:
     * Verifies that the Pullups class returns a score.
     * Input Array: [PerformanceValue, Gender(1.0=M), Weight, Height]
     */
    @Test
    fun testAIPullupScore() {
        val input = doubleArrayOf(10.0, 1.0, 70.0, 175.0)
        val score = BoulderingAI.Pullups.score(input)

        // Check that it actually returns a number
        assertTrue(score >= 0.0)
    }

    /**
     * DATA TEST:
     * Verifies that the TrainingExercise class holds data correctly.
     * This ensures your Firebase data objects are working.
     */
    @Test
    fun testExerciseData() {
        val exercise = TrainingExercise(
            name = "Test",
            weight = "10kg",
            sets = "3",
            reps = "10",
            time = "15",
            rest = "60"
        )

        assertEquals("Test", exercise.name)
        assertFalse(exercise.completed)

        exercise.completed = true
        assertTrue(exercise.completed)
    }

}