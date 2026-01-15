package com.example.hsp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class WorkoutActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var exerciseName: TextView
    private lateinit var timerTextView: TextView
    private lateinit var repCount: TextView
    private lateinit var setCount: TextView

    private lateinit var prevSetButton: ImageButton
    private lateinit var prevRepButton: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextRepButton: ImageButton
    private lateinit var nextSetButton: ImageButton

    private val exercises = WorkoutData.exercises
    private var currentExerciseIndex = 0
    private var currentSet = 1
    private var currentRep = 1

    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = 0
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        auth = FirebaseAuth.getInstance()

        exerciseName = findViewById(R.id.exercise_name)
        timerTextView = findViewById(R.id.timer)
        repCount = findViewById(R.id.rep_count)
        setCount = findViewById(R.id.set_count)

        prevSetButton = findViewById(R.id.prev_set_button)
        prevRepButton = findViewById(R.id.prev_rep_button)
        playPauseButton = findViewById(R.id.play_pause_button)
        nextRepButton = findViewById(R.id.next_rep_button)
        nextSetButton = findViewById(R.id.next_set_button)

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        updateExerciseUI()
    }

    private fun updateExerciseUI() {
        val exercise = exercises[currentExerciseIndex]
        exerciseName.text = exercise.name
        updateRepAndSetCount()
        resetTimer()
    }

    private fun updateRepAndSetCount() {
        val exercise = exercises[currentExerciseIndex]
        repCount.text = "$currentRep/${exercise.reps}"
        setCount.text = "$currentSet/${exercise.sets}"
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.logout_button).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val bottomNavManager = BottomNavManager(this, bottomNavigationView)
        bottomNavManager.setupBottomNav(R.id.nav_home)

        prevSetButton.setOnClickListener { changeSet(-1) }
        nextSetButton.setOnClickListener { changeSet(1) }
        prevRepButton.setOnClickListener { changeRep(-1) }
        nextRepButton.setOnClickListener { changeRep(1) }
        playPauseButton.setOnClickListener { toggleTimer() }
    }

    private fun changeSet(direction: Int) {
        val exercise = exercises[currentExerciseIndex]
        val nextSet = currentSet + direction
        if (nextSet in 1..exercise.sets) {
            currentSet = nextSet
            currentRep = 1 // Reset reps when changing sets
            updateRepAndSetCount()
            resetTimer()
        } else if (nextSet > exercise.sets) {
            // Move to the next exercise if available
            if (currentExerciseIndex < exercises.size - 1) {
                currentExerciseIndex++
                currentSet = 1
                currentRep = 1
                updateExerciseUI()
            }
        }
    }

    private fun changeRep(direction: Int) {
        val exercise = exercises[currentExerciseIndex]
        val nextRep = currentRep + direction
        if (nextRep in 1..exercise.reps) {
            currentRep = nextRep
            updateRepAndSetCount()
            resetTimer()
        } else if (nextRep > exercise.reps) {
            changeSet(1) // Move to the next set if reps are done
        }
    }

    private fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (timeRemainingInMillis <= 0) return // Don't start a finished timer

        countDownTimer = object : CountDownTimer(timeRemainingInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                timeRemainingInMillis = 0
                isTimerRunning = false
                updateTimerText()
                playPauseButton.setImageResource(R.drawable.play)
            }
        }.start()

        isTimerRunning = true
        playPauseButton.setImageResource(R.drawable.ic_pause)
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        playPauseButton.setImageResource(R.drawable.play)
    }

    private fun resetTimer() {
        pauseTimer()
        val exercise = exercises[currentExerciseIndex]
        timeRemainingInMillis = (exercise.duration ?: 0) * 1000
        playPauseButton.isEnabled = timeRemainingInMillis > 0
        updateTimerText()
    }

    private fun updateTimerText() {
        val seconds = timeRemainingInMillis / 1000
        if (timeRemainingInMillis > 0) {
            timerTextView.text = "0:${String.format("%02d", seconds)}"
        } else {
             val exercise = exercises[currentExerciseIndex]
             if (exercise.duration != null) {
                timerTextView.text = "0:${String.format("%02d", exercise.duration)}"
             } else {
                timerTextView.text = "--:--"
             }
        }
    }
}
