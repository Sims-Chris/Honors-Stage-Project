package com.example.hsp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {
    private lateinit var bottomNavManager: BottomNavManager
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavManager = BottomNavManager(this, bottomNavigationView)

        findViewById<MaterialButton>(R.id.logout_button).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.workout_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        workoutAdapter = WorkoutAdapter(emptyList())
        recyclerView.adapter = workoutAdapter

        val calendarView = findViewById<CalendarView>(R.id.calendar_view)
        
        val today = Calendar.getInstance()
        fetchWorkoutsForDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            fetchWorkoutsForDate(year, month, dayOfMonth)
        }
    }

    private fun fetchWorkoutsForDate(year: Int, month: Int, dayOfMonth: Int) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateString = dateFormat.format(calendar.time)
        
        val uid = currentUser.uid
        val email = currentUser.email ?: ""

        Log.d("CalendarActivity", "Fetching for Date: $dateString")

        checkDocument(uid, dateString) { found ->
            if (!found && email.isNotEmpty()) {
                checkDocument(email, dateString) { finalFound ->
                    if (!finalFound) showNoPlan()
                }
            } else if (!found) {
                showNoPlan()
            }
        }
    }

    private fun checkDocument(userDocId: String, dateString: String, onResult: (Boolean) -> Unit) {
        db.collection("Users")
            .document(userDocId)
            .collection("Training Plan")
            .document(dateString)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val workouts = mutableListOf<WorkoutSummary>()
                    val data = document.data
                    Log.d("CalendarActivity", "Raw Data: $data")
                    
                    data?.forEach { (key, value) ->
                        if (value is Map<*, *>) {
                            val name = key.toString()
                            
                            // Helper to handle spaces or casing in field names
                            fun getField(map: Map<*, *>, fieldName: String): String {
                                return (map[fieldName] ?: 
                                        map["$fieldName "] ?: 
                                        map[fieldName.lowercase()] ?: 
                                        map["${fieldName.lowercase()} "])?.toString() ?: "0"
                            }

                            val sets = getField(value, "Sets")
                            val reps = getField(value, "Reps")
                            val rest = getField(value, "Rest")
                            
                            val summaryText = "Sets: $sets | Reps: $reps | Rest: ${rest}s"
                            workouts.add(WorkoutSummary(dateString, name, "Training", summaryText))
                        }
                    }
                    
                    if (workouts.isNotEmpty()) {
                        workoutAdapter.updateWorkouts(workouts)
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CalendarActivity", "Error checking path", e)
                onResult(false)
            }
    }

    private fun showNoPlan() {
        workoutAdapter.updateWorkouts(listOf(
            WorkoutSummary("", "No plan for this day", "", "")
        ))
    }

    override fun onResume() {
        super.onResume()
        bottomNavManager.setupBottomNav(R.id.nav_calendar)
    }
}