package com.example.hsp

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bottomNavManager: BottomNavManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val startWorkoutButton = findViewById<Button>(R.id.start_workout_button)
        startWorkoutButton.setOnClickListener {
            val intent = Intent(this, WorkoutActivity::class.java)
            startActivity(intent)
        }

        val logoutButton = findViewById<Button>(R.id.logout_button)
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val generateWorkoutButton = findViewById<Button>(R.id.generate_workout_button)
        generateWorkoutButton.setOnClickListener {
            startActivity(Intent(this, GenerateWorkoutActivity::class.java))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavManager = BottomNavManager(this, bottomNavigationView)

        fetchTodayWorkout()
        fetchStreak()
        fetchTotalWorkouts()
    }

    private fun fetchTodayWorkout() {
        val currentUser = auth.currentUser ?: return
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        val uid = currentUser.uid
        val email = currentUser.email ?: ""

        // Try UID first, then Email as fallback
        checkDocument(uid, today) { found ->
            if (!found && email.isNotEmpty()) {
                checkDocument(email, today) { finalFound ->
                    if (!finalFound) displayRestDay()
                }
            } else if (!found) {
                displayRestDay()
            }
        }
    }

    private fun fetchStreak() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val email = currentUser.email ?: ""

        calculateStreak(uid) { found ->
            if (!found && email.isNotEmpty()) {
                calculateStreak(email) { }
            }
        }
    }

    private fun fetchTotalWorkouts() {
        val currentUser = auth.currentUser ?: return
        val totalWorkoutsTextView = findViewById<TextView>(R.id.total_workouts_count)
        
        db.collection("Users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val completed = document.getLong("TotalCompleted") ?: 0
                    val planned = document.getLong("TotalPlanned") ?: 0
                    totalWorkoutsTextView.text = "$completed / $planned"
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Error fetching TotalCompleted", e)
            }
    }

    private fun calculateStreak(userDocId: String, onResult: (Boolean) -> Unit) {
        val streakTextView = findViewById<TextView>(R.id.streak_days)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        
        db.collection("Users").document(userDocId).collection("Training Plan")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onResult(false)
                    return@addOnSuccessListener
                }

                val workoutMap = mutableMapOf<String, Boolean>() // date -> isAllCompleted
                
                for (doc in documents) {
                    val date = doc.id
                    val exercisesList = doc.get("exercises") as? List<Map<String, Any>>
                    
                    val hasExercises = if (exercisesList != null) {
                        exercisesList.isNotEmpty()
                    } else {
                        doc.data.any { it.value is Map<*, *> }
                    }

                    if (hasExercises) {
                        val isAllCompleted = if (exercisesList != null) {
                            exercisesList.all { it["Completed"] as? Boolean == true }
                        } else {
                            val data = doc.data
                            var allDone = true
                            data.forEach { (_, value) ->
                                if (value is Map<*, *>) {
                                    if (!((value["Completed"] ?: value["completed"]) as? Boolean ?: false)) {
                                        allDone = false
                                    }
                                }
                            }
                            allDone
                        }
                        workoutMap[date] = isAllCompleted
                    }
                }

                if (workoutMap.isEmpty()) {
                    onResult(false)
                    return@addOnSuccessListener
                }

                // Calculate streak backwards
                var streak = 0
                val calendar = Calendar.getInstance()
                val todayStr = dateFormat.format(calendar.time)
                
                // Rule: Excluding today unless it is completed.
                // If today has exercises:
                //   If completed: increment and move to yesterday.
                //   If NOT completed: don't increment, just move to yesterday (doesn't break yet).
                // If today has NO exercises: move to yesterday.
                
                if (workoutMap.containsKey(todayStr)) {
                    if (workoutMap[todayStr] == true) {
                        streak++
                    }
                }
                
                // Now check from yesterday backwards
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                
                var daysChecked = 0
                while (daysChecked < 365) { // Limit search to 1 year
                    val dateStr = dateFormat.format(calendar.time)
                    if (workoutMap.containsKey(dateStr)) {
                        if (workoutMap[dateStr] == true) {
                            streak++
                        } else {
                            // Break streak on first incomplete workout found in history
                            break
                        }
                    } else {
                        // Days with no exercises are ignored in the counter
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    daysChecked++
                }

                streakTextView.text = "$streak ${if (streak == 1) "Day" else "Days"}"
                onResult(true)
            }
            .addOnFailureListener {
                Log.e("HomeActivity", "Error calculating streak", it)
                onResult(false)
            }
    }

    private fun isValid(value: String?): Boolean {
        return !value.isNullOrBlank() && value != "0" && value != "NaN" && value != "null"
    }

    private fun checkDocument(userDocId: String, dateString: String, onResult: (Boolean) -> Unit) {
        val workoutTitle = findViewById<TextView>(R.id.workout_title)
        val workoutSubtitle = findViewById<TextView>(R.id.workout_subtitle)
        val workoutDetails = findViewById<TextView>(R.id.workout_details)
        val startWorkoutButton = findViewById<Button>(R.id.start_workout_button)

        db.collection("Users").document(userDocId)
            .collection("Training Plan").document(dateString)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val details = SpannableStringBuilder()
                    var exercisesFound = false
                    var allCompleted = true

                    fun appendExerciseWithIcon(name: String, completed: Boolean, stats: List<String>) {
                        val iconRes = if (completed) R.drawable.check else R.drawable.cross
                        val drawable = ContextCompat.getDrawable(this@HomeActivity, iconRes)?.mutate()?.apply {
                            val size = (workoutDetails.textSize * 1.1).toInt()
                            setBounds(0, 0, size, size)
                            setTint(ContextCompat.getColor(this@HomeActivity, R.color.textColour))
                        }

                        if (drawable != null) {
                            details.append("  ")
                            details.setSpan(
                                ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
                                details.length - 2,
                                details.length - 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        } else {
                            details.append(if (completed) "✓ " else "✗ ")
                        }

                        details.append(name)
                        if (stats.isNotEmpty()) {
                            details.append(": ${stats.joinToString(" | ")}")
                        }
                        details.append("\n")
                    }

                    // 1. Try NEW format (list of maps)
                    val exercisesList = document.get("exercises") as? List<Map<String, Any>>
                    if (exercisesList != null && exercisesList.isNotEmpty()) {
                        exercisesFound = true
                        exercisesList.forEach { ex ->
                            val name = ex["Name"] as? String ?: "Unknown Exercise"
                            val sets = ex["Sets"]?.toString()
                            val reps = ex["Reps"]?.toString()
                            val weight = ex["Weight"]?.toString()
                            val time = ex["Time"]?.toString()
                            val completed = ex["Completed"] as? Boolean ?: false
                            
                            if (!completed) allCompleted = false

                            val stats = mutableListOf<String>()
                            if (isValid(sets)) stats.add("Sets: $sets")
                            if (isValid(reps)) stats.add("Reps: $reps")
                            if (isValid(time)) stats.add("Time: ${time}s")
                            if (isValid(weight) && !weight!!.contains("add or remove weight")) stats.add("Weight: $weight")
                            
                            appendExerciseWithIcon(name, completed, stats)
                        }
                    } else {
                        // 2. Try OLD format (top-level fields are exercises)
                        val data = document.data
                        data?.forEach { (key, value) ->
                            if (value is Map<*, *>) {
                                exercisesFound = true
                                val name = key.toString()
                                
                                fun getField(map: Map<*, *>, fieldName: String): String {
                                    return (map[fieldName] ?: map["$fieldName "] ?: map[fieldName.lowercase()] ?: map["${fieldName.lowercase()} "])?.toString() ?: ""
                                }

                                val sets = getField(value, "Sets")
                                val reps = getField(value, "Reps")
                                val weight = getField(value, "Weight")
                                val time = getField(value, "Time")
                                val completed = (value["Completed"] ?: value["completed"]) as? Boolean ?: false

                                if (!completed) allCompleted = false

                                val stats = mutableListOf<String>()
                                if (isValid(sets)) stats.add("Sets: $sets")
                                if (isValid(reps)) stats.add("Reps: $reps")
                                if (isValid(time)) stats.add("Time: ${time}s")
                                if (isValid(weight) && !weight.contains("add or remove weight")) stats.add("Weight: $weight")

                                appendExerciseWithIcon(name, completed, stats)
                            }
                        }
                    }

                    if (exercisesFound) {
                        workoutTitle.text = if (allCompleted) "Today's Plan (Completed)" else "Today's Plan"
                        workoutSubtitle.text = "Scheduled for $dateString"
                        
                        // Remove trailing newline
                        if (details.isNotEmpty() && details.last() == '\n') {
                            details.delete(details.length - 1, details.length)
                        }
                        
                        workoutDetails.text = details
                        startWorkoutButton.visibility = if (allCompleted) View.GONE else View.VISIBLE
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener {
                Log.e("HomeActivity", "Error fetching workout", it)
                onResult(false)
            }
    }

    private fun displayRestDay() {
        findViewById<TextView>(R.id.workout_title).text = "Rest Day"
        findViewById<TextView>(R.id.workout_subtitle).text = "No workout scheduled for today"
        findViewById<TextView>(R.id.workout_details).text = "Take some time to recover or generate a new plan!"
        findViewById<Button>(R.id.start_workout_button).visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        bottomNavManager.setupBottomNav(R.id.nav_home)
        fetchTodayWorkout()
        fetchStreak()
        fetchTotalWorkouts()
    }
}
