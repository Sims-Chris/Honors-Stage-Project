package com.example.hsp

data class Exercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val duration: Long? = null // Duration in seconds
)
