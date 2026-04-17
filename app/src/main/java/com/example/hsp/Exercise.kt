package com.example.hsp

data class Exercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val duration: Long? = null, //Seconds
    val rest: Long? = null,      // Seconds
    var completed: Boolean = false
)
