package com.example.hsp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    }

    private fun fetchTodayWorkout() {
        val currentUser = auth.currentUser ?: return
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        val uid = currentUser.uid
        val email = currentUser.email ?: ""

        // Try UID first, then Email as fallback (matching CalendarActivity logic)
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
                    val details = StringBuilder()
                    var exercisesFound = false

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
                            
                            details.append("• $name")
                            val stats = mutableListOf<String>()
                            if (isValid(sets)) stats.add("Sets: $sets")
                            if (isValid(reps)) stats.add("Reps: $reps")
                            if (isValid(time)) stats.add("Time: ${time}s")
                            if (isValid(weight) && !weight!!.contains("add or remove weight")) stats.add("Weight: $weight")
                            
                            if (stats.isNotEmpty()) details.append(": ${stats.joinToString(" | ")}")
                            details.append("\n")
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

                                details.append("• $name")
                                val stats = mutableListOf<String>()
                                if (isValid(sets)) stats.add("Sets: $sets")
                                if (isValid(reps)) stats.add("Reps: $reps")
                                if (isValid(time)) stats.add("Time: ${time}s")
                                if (isValid(weight) && !weight.contains("add or remove weight")) stats.add("Weight: $weight")

                                if (stats.isNotEmpty()) details.append(": ${stats.joinToString(" | ")}")
                                details.append("\n")
                            }
                        }
                    }

                    if (exercisesFound) {
                        workoutTitle.text = "Today's Plan"
                        workoutSubtitle.text = "Scheduled for $dateString"
                        workoutDetails.text = details.toString().trim()
                        startWorkoutButton.visibility = View.VISIBLE
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
    }
}