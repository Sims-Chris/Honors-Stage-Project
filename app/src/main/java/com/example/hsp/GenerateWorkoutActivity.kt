package com.example.hsp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class GenerateWorkoutActivity : AppCompatActivity() {

    private lateinit var bottomNavManager: BottomNavManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_workout)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavManager = BottomNavManager(this, bottomNavigationView)

        setupUI()
    }

    private fun setupUI() {
        val targetGradeEdit = findViewById<EditText>(R.id.target_grade)
        val sexEdit = findViewById<EditText>(R.id.sex)
        val heightEdit = findViewById<EditText>(R.id.height)
        val weightEdit = findViewById<EditText>(R.id.weight)
        
        val pullupsEdit = findViewById<EditText>(R.id.pullups)
        val weightedPullEdit = findViewById<EditText>(R.id.weighted_pull)
        val maxHangEdit = findViewById<EditText>(R.id.maxhang)
        val continuousHangEdit = findViewById<EditText>(R.id.continuoushang)
        val powlEdit = findViewById<EditText>(R.id.powl)
        val powrEdit = findViewById<EditText>(R.id.powr)
        val pushupsEdit = findViewById<EditText>(R.id.pushups)
        
        val generateBtn = findViewById<Button>(R.id.btn_generate)
        val outputText = findViewById<TextView>(R.id.plan_output)

        generateBtn.setOnClickListener {
            try {
                val targetGrade = targetGradeEdit.text.toString().toDouble()
                val sex = sexEdit.text.toString().toDouble()
                val height = heightEdit.text.toString().toDouble()
                val weight = weightEdit.text.toString().toDouble()

                val actuals = mapOf(
                    "pullups" to (pullupsEdit.text.toString().toDoubleOrNull() ?: 0.0),
                    "weighted_pull" to (weightedPullEdit.text.toString().toDoubleOrNull() ?: 0.0),
                    "maxhang" to (maxHangEdit.text.toString().toDoubleOrNull() ?: 0.0),
                    "continuoushang" to (continuousHangEdit.text.toString().toDoubleOrNull() ?: 0.0),
                    "powl" to (powlEdit.text.toString().toDoubleOrNull() ?: 0.0),
                    "powr" to (powrEdit.text.toString().toDoubleOrNull() ?: 0.0),
                    "pushups" to (pushupsEdit.text.toString().toDoubleOrNull() ?: 0.0)
                )

                // For now, using a fixed set of active days. 
                // In a full implementation, these could be picked via CheckBoxes.
                val activeDays = listOf("Monday", "Wednesday", "Friday")

                val plan = TrainingPlanGenerator.generatePlan(
                    targetGrade, sex, height, weight, actuals, activeDays
                )

                outputText.text = plan
                Toast.makeText(this, "Plan Generated!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this, "Please fill in all profile fields correctly", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavManager.setupBottomNav(R.id.nav_home)
    }
}
