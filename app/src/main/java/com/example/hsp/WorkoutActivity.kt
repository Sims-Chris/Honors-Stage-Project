package com.example.hsp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class WorkoutActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var exerciseName: TextView
    private lateinit var timerTextView: TextView
    private lateinit var repCount: TextView
    private lateinit var setCount: TextView

    private lateinit var prevSetButton: ImageButton
    private lateinit var prevRepButton: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextRepButton: ImageButton
    private lateinit var nextSetButton: ImageButton

    private var exercises: MutableList<Exercise> = mutableListOf()
    private var currentExerciseIndex = 0
    private var currentSet = 1
    private var currentRep = 1

    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = 0
    private var isTimerRunning = false
    private var isResting = false
    private var activeUserDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        exerciseName = findViewById(R.id.exercise_name)
        timerTextView = findViewById(R.id.timer)
        repCount = findViewById(R.id.rep_count)
        setCount = findViewById(R.id.set_count)

        prevSetButton = findViewById(R.id.prev_set_button)
        prevRepButton = findViewById(R.id.prev_rep_button)
        playPauseButton = findViewById(R.id.play_pause_button)
        nextRepButton = findViewById(R.id.next_rep_button)
        nextSetButton = findViewById(R.id.next_set_button)

        setupClickListeners()
        fetchTodayWorkout()
    }

    private fun fetchTodayWorkout() {
        val currentUser = auth.currentUser ?: return
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        val uid = currentUser.uid
        val email = currentUser.email ?: ""

        checkDocument(uid, today) { found ->
            if (!found && email.isNotEmpty()) {
                checkDocument(email, today) { finalFound ->
                    if (!finalFound) handleNoWorkout()
                }
            } else if (!found) {
                handleNoWorkout()
            }
        }
    }

    private fun checkDocument(userDocId: String, dateString: String, onResult: (Boolean) -> Unit) {
        db.collection("Users").document(userDocId)
            .collection("Training Plan").document(dateString)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    activeUserDocId = userDocId
                    exercises.clear()
                    
                    val exercisesList = document.get("exercises") as? List<Map<String, Any>>
                    if (exercisesList != null && exercisesList.isNotEmpty()) {
                        exercisesList.forEach { data ->
                            val name = data["Name"] as? String ?: "Exercise"
                            val setsStr = data["Sets"]?.toString() ?: "1"
                            val repsStr = data["Reps"]?.toString() ?: "1"
                            val timeStr = data["Time"]?.toString() ?: "0"
                            val restStr = data["Rest"]?.toString() ?: "0"
                            val completed = data["Completed"] as? Boolean ?: false
                            
                            val sets = setsStr.toIntOrNull() ?: 1
                            val reps = repsStr.toIntOrNull() ?: 1
                            val duration = timeStr.toLongOrNull() ?: 0L
                            val rest = restStr.toLongOrNull() ?: 0L
                            
                            exercises.add(Exercise(name, sets, reps, if (duration > 0) duration else null, if (rest > 0) rest else null, completed))
                        }
                    } else {
                        val data = document.data
                        data?.forEach { (key, value) ->
                            if (value is Map<*, *>) {
                                val name = key.toString()
                                
                                fun getField(map: Map<*, *>, fieldName: String): String {
                                    return (map[fieldName] ?: map["$fieldName "] ?: map[fieldName.lowercase()] ?: map["${fieldName.lowercase()} "])?.toString() ?: "0"
                                }

                                val setsStr = getField(value, "Sets")
                                val repsStr = getField(value, "Reps")
                                val timeStr = getField(value, "Time")
                                val restStr = getField(value, "Rest")
                                val completed = (value["Completed"] ?: value["completed"]) as? Boolean ?: false

                                val sets = setsStr.toIntOrNull() ?: 1
                                val reps = repsStr.toIntOrNull() ?: 1
                                val duration = timeStr.toLongOrNull() ?: 0L
                                val rest = restStr.toLongOrNull() ?: 0L

                                exercises.add(Exercise(name, sets, reps, if (duration > 0) duration else null, if (rest > 0) rest else null, completed))
                            }
                        }
                    }

                    if (exercises.isNotEmpty()) {
                        val allDone = exercises.all { it.completed }
                        if (allDone) {
                            Toast.makeText(this@WorkoutActivity, "You have already completed today's workout!", Toast.LENGTH_SHORT).show()
                            finish()
                            onResult(true)
                            return@addOnSuccessListener
                        }

                        // Find first uncompleted exercise
                        currentExerciseIndex = exercises.indexOfFirst { !it.completed }.coerceAtLeast(0)
                        updateExerciseUI()
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener {
                Log.e("WorkoutActivity", "Error fetching workout", it)
                onResult(false)
            }
    }

    private fun handleNoWorkout() {
        Toast.makeText(this, "No workout scheduled for today", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateExerciseUI() {
        if (exercises.isEmpty()) return
        
        val exercise = exercises[currentExerciseIndex]
        if (isResting) {
            exerciseName.text = "Resting..."
        } else {
            exerciseName.text = if (exercise.completed) "${exercise.name} (Done)" else exercise.name
        }
        updateRepAndSetCount()
        resetTimer()
    }

    private fun updateRepAndSetCount() {
        if (exercises.isEmpty()) return
        
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
        if (exercises.isEmpty()) return
        
        val exercise = exercises[currentExerciseIndex]
        val nextSet = currentSet + direction
        
        isResting = false // Reset resting state on manual change
        
        if (nextSet in 1..exercise.sets) {
            currentSet = nextSet
            currentRep = 1
            updateExerciseUI()
        } else if (nextSet > exercise.sets) {
            markExerciseCompleted(currentExerciseIndex)
            
            if (currentExerciseIndex < exercises.size - 1) {
                currentExerciseIndex++
                currentSet = 1
                currentRep = 1
                updateExerciseUI()
            } else {
                Toast.makeText(this, "Workout Complete!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun incrementExerciseStats() {
        val currentUser = auth.currentUser ?: return
        val docId = activeUserDocId ?: currentUser.uid
        val userRef = db.collection("Users").document(docId)
        
        val updates = hashMapOf<String, Any>(
            "TotalCompleted" to FieldValue.increment(1),
            "Incomplete" to FieldValue.increment(-1)
        )
        
        userRef.update(updates)
            .addOnFailureListener { e ->
                Log.e("WorkoutActivity", "Error updating workout stats", e)
            }
    }

    private fun markExerciseCompleted(index: Int) {
        if (index !in exercises.indices) return
        val exercise = exercises[index]
        if (exercise.completed) return
        
        exercise.completed = true
        incrementExerciseStats()
        
        val currentUser = auth.currentUser ?: return
        val docId = activeUserDocId ?: currentUser.uid
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        val docRef = db.collection("Users").document(docId)
            .collection("Training Plan").document(today)
            
        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val exercisesList = document.get("exercises") as? MutableList<Map<String, Any>>
                if (exercisesList != null && index < exercisesList.size) {
                    val updatedList = exercisesList.toMutableList()
                    val updatedExercise = updatedList[index].toMutableMap()
                    updatedExercise["Completed"] = true
                    updatedList[index] = updatedExercise
                    docRef.update("exercises", updatedList)
                }
            }
        }
    }

    private fun changeRep(direction: Int) {
        if (exercises.isEmpty()) return
        
        val exercise = exercises[currentExerciseIndex]
        val nextRep = currentRep + direction
        
        isResting = false // Reset resting state on manual change
        
        if (nextRep in 1..exercise.reps) {
            currentRep = nextRep
            updateRepAndSetCount()
            resetTimer()
        } else if (nextRep > exercise.reps) {
            changeSet(1)
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
        if (timeRemainingInMillis <= 0) return

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
                handleTimerFinished()
            }
        }.start()

        isTimerRunning = true
        playPauseButton.setImageResource(R.drawable.ic_pause)
    }

    private fun handleTimerFinished() {
        if (exercises.isEmpty()) return
        val exercise = exercises[currentExerciseIndex]

        if (!isResting) {
            if (currentRep < exercise.reps) {
                changeRep(1)
            } else {
                if (currentSet < exercise.sets && exercise.rest != null && exercise.rest > 0) {
                    isResting = true
                    updateExerciseUI()
                } else {
                    changeSet(1)
                }
            }
        } else {
            isResting = false
            changeSet(1)
        }
        // User must click Play to start the timer for the next segment
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        playPauseButton.setImageResource(R.drawable.play)
    }

    private fun resetTimer() {
        pauseTimer()
        if (exercises.isEmpty()) return
        
        val exercise = exercises[currentExerciseIndex]
        val duration = if (isResting) exercise.rest else exercise.duration
        
        timeRemainingInMillis = (duration ?: 0) * 1000
        playPauseButton.isEnabled = timeRemainingInMillis > 0
        updateTimerText()
    }

    private fun updateTimerText() {
        val seconds = timeRemainingInMillis / 1000
        val min = seconds / 60
        val sec = seconds % 60
        
        if (timeRemainingInMillis > 0) {
            timerTextView.text = String.format("%d:%02d", min, sec)
        } else {
             if (exercises.isEmpty()) {
                timerTextView.text = "--:--"
                return
             }
             val exercise = exercises[currentExerciseIndex]
             val duration = if (isResting) exercise.rest else exercise.duration
             if (duration != null && duration > 0) {
                timerTextView.text = String.format("%d:%02d", duration / 60, duration % 60)
             } else {
                timerTextView.text = "--:--"
             }
        }
    }
}
