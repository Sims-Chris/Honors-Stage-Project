package com.example.hsp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hsp.databinding.ActivityPlanPreviewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PlanPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanPreviewBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var weeklyPlan: Map<String, List<TrainingExercise>>? = null
    private var numWeeks: Int = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        @Suppress("UNCHECKED_CAST")
        weeklyPlan = intent.getSerializableExtra("weeklyPlan") as? Map<String, List<TrainingExercise>>
        numWeeks = intent.getIntExtra("numWeeks", 4)

        if (weeklyPlan == null || weeklyPlan!!.isEmpty()) {
            Log.e("PlanPreview", "No plan data received in intent")
            Toast.makeText(this, "Error: No plan data found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PlanPreview", "Received plan with ${weeklyPlan!!.size} days")
        
        binding.previewTitle.text = "Training Week Preview ($numWeeks Weeks)"
        displayPlan()
        setupButtons()
    }

    private fun displayPlan() {
        val plan = weeklyPlan ?: return
        val context = this
        binding.planContainer.removeAllViews()

        val margin16 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()
        val margin8 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()

        plan.forEach { (day, exercises) ->
            val dayHeader = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, margin16, 0, margin8)
                }
                text = day
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(null, Typeface.BOLD)
                setPadding(margin8, margin8, margin8, margin8)
            }
            binding.planContainer.addView(dayHeader)

            if (exercises.isEmpty()) {
                val emptyText = TextView(context).apply {
                    text = "No exercises assigned"
                    setTextColor(Color.GRAY)
                    setPadding(margin16, 0, 0, 0)
                }
                binding.planContainer.addView(emptyText)
            }

            exercises.forEach { ex ->
                val exerciseCard = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(margin16, margin16, margin16, margin16)
                    setBackgroundResource(R.drawable.rounded_corners)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, margin8)
                    }
                }

                val nameText = TextView(context).apply {
                    text = ex.name
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTypeface(null, Typeface.BOLD)
                }
                exerciseCard.addView(nameText)

                val details = StringBuilder()
                if (isValid(ex.sets)) details.append("Sets: ${ex.sets} | ")
                if (isValid(ex.reps)) details.append("Reps: ${ex.reps} | ")
                if (isValid(ex.time)) details.append("Time: ${ex.time}s | ")
                if (isValid(ex.weight) && !ex.weight.contains("add or remove weight")) {
                    details.append(ex.weight)
                }

                val detailText = TextView(context).apply {
                    text = details.toString().trimEnd(' ', '|')
                    setTextColor(Color.parseColor("#CCCCCC"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setPadding(0, 4, 0, 0)
                }
                exerciseCard.addView(detailText)

                binding.planContainer.addView(exerciseCard)
            }
        }
    }

    private fun isValid(value: String?): Boolean {
        return !value.isNullOrBlank() && value != "0" && value != "NaN" && value != "null" && 
               !value.contains("DYNAMIC")
    }

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm New Plan")
                .setMessage("Accepting this plan will delete ALL your previous training data. Do you want to proceed?")
                .setPositiveButton("Accept & Clear Data") { _, _ ->
                    savePlanToFirestore()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnRetry.setOnClickListener {
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun savePlanToFirestore() {
        val currentUser = auth.currentUser ?: return
        val batch = db.batch()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dayOfWeekMap = mapOf(
            "Sunday" to Calendar.SUNDAY, "Monday" to Calendar.MONDAY, "Tuesday" to Calendar.TUESDAY,
            "Wednesday" to Calendar.WEDNESDAY, "Thursday" to Calendar.THURSDAY, "Friday" to Calendar.FRIDAY,
            "Saturday" to Calendar.SATURDAY
        )

        // Calculate total exercises across all weeks
        var totalExercisesPerWeek = 0
        weeklyPlan?.values?.forEach { exercises ->
            totalExercisesPerWeek += exercises.size
        }
        val totalIncomplete = totalExercisesPerWeek * numWeeks

        deleteAllWorkouts {
            // Update User Metadata
            val userRef = db.collection("Users").document(currentUser.uid)
            val updates = hashMapOf<String, Any>(
                "Incomplete" to totalIncomplete,
                "TotalPlanned" to totalIncomplete,
                "TotalCompleted" to 0 // Reset completed count for a new plan? 
                // Or maybe the user wants TotalCompleted to be lifetime? 
                // Usually it's better to keep lifetime but if they are generating a NEW plan, 
                // maybe they want to start over. However, TotalCompleted usually implies lifetime.
            )
            // If they want "X / Y", reset is probably not wanted if TotalCompleted is lifetime.
            // But they said "same number as Incomplete on generation" for "total workouts made".
            
            batch.update(userRef, updates)

            for (week in 0 until numWeeks) {
                weeklyPlan?.forEach { (dayName, exercises) ->
                    val calendar = Calendar.getInstance()
                    val targetDay = dayOfWeekMap[dayName]!!
                    var daysUntil = targetDay - calendar.get(Calendar.DAY_OF_WEEK)
                    if (daysUntil < 0) daysUntil += 7
                    
                    calendar.add(Calendar.DAY_OF_YEAR, daysUntil + (week * 7))
                    val dateString = dateFormat.format(calendar.time)
                    
                    val exerciseList = exercises.map { ex ->
                        mapOf(
                            "Name" to ex.name,
                            "Weight" to ex.weight,
                            "Sets" to ex.sets,
                            "Reps" to ex.reps,
                            "Time" to ex.time,
                            "Rest" to ex.rest,
                            "Completed" to ex.completed
                        )
                    }
                    val workoutData = mapOf("exercises" to exerciseList)
                    val docRef = db.collection("Users").document(currentUser.uid)
                        .collection("Training Plan").document(dateString)
                    batch.set(docRef, workoutData)
                }
            }

            batch.commit().addOnSuccessListener {
                Toast.makeText(this, "Plan Saved Successfully!", Toast.LENGTH_LONG).show()
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAllWorkouts(onComplete: () -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete()
        db.collection("Users").document(currentUser.uid)
            .collection("Training Plan")
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (doc in documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnCompleteListener { onComplete() }
            }
            .addOnFailureListener { onComplete() }
    }
}
