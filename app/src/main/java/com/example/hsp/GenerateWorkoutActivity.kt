package com.example.hsp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable
import java.util.*

class GenerateWorkoutActivity : AppCompatActivity() {

    private lateinit var bottomNavManager: BottomNavManager
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isMetric = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_workout)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavManager = BottomNavManager(this, bottomNavigationView)

        setupUI()
    }

    private fun kgToLb(kg: Double): Double = kg * 2.20462262185
    private fun cmToInch(cm: Double): Double = cm * 0.3937007874

    private fun setupUI() {
        val unitToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.unit_toggle_group)
        val heightEdit = findViewById<EditText>(R.id.height)
        val weightEdit = findViewById<EditText>(R.id.weight)
        val weightedPullEdit = findViewById<EditText>(R.id.weighted_pull)
        val maxHangEdit = findViewById<EditText>(R.id.maxhang)
        val powlEdit = findViewById<EditText>(R.id.powl)
        val powrEdit = findViewById<EditText>(R.id.powr)

        unitToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isMetric = checkedId == R.id.btn_metric
                if (isMetric) {
                    heightEdit.hint = "Height (cm)"
                    weightEdit.hint = "Weight (kg)"
                    weightedPullEdit.hint = "Max Weighted Pull Up (Total kg)"
                    maxHangEdit.hint = "Max Hang 20mm (kg added)"
                    powlEdit.hint = "Power Left (cm)"
                    powrEdit.hint = "Power Right (cm)"
                } else {
                    heightEdit.hint = "Height (inches)"
                    weightEdit.hint = "Weight (lbs)"
                    weightedPullEdit.hint = "Max Weighted Pull (Total lbs)"
                    maxHangEdit.hint = "Max Hang 20mm (lbs added)"
                    powlEdit.hint = "Power Left (inches)"
                    powrEdit.hint = "Power Right (inches)"
                }
            }
        }

        val generateBtn = findViewById<Button>(R.id.btn_generate)

        generateBtn.setOnClickListener {
            if (validateInputs()) {
                startGeneration()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavManager.setupBottomNav(0)
    }

    private fun validateInputs(): Boolean {
        val editTexts = listOf(
            R.id.target_grade, R.id.sex, R.id.height, R.id.weight,
            R.id.pullups, R.id.weighted_pull, R.id.maxhang,
            R.id.continuoushang, R.id.powl, R.id.powr, R.id.pushups,
            R.id.num_weeks
        )

        for (id in editTexts) {
            if (findViewById<EditText>(id).text.isNullOrBlank()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        val checkBoxes = listOf(
            R.id.check_monday, R.id.check_tuesday, R.id.check_wednesday,
            R.id.check_thursday, R.id.check_friday, R.id.check_saturday, R.id.check_sunday
        )

        if (checkBoxes.none { findViewById<CheckBox>(it).isChecked }) {
            Toast.makeText(this, "Please select at least one training day", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun startGeneration() {
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
        val numWeeksEdit = findViewById<EditText>(R.id.num_weeks)

        val checkBoxes = mapOf(
            "Monday" to findViewById<CheckBox>(R.id.check_monday),
            "Tuesday" to findViewById<CheckBox>(R.id.check_tuesday),
            "Wednesday" to findViewById<CheckBox>(R.id.check_wednesday),
            "Thursday" to findViewById<CheckBox>(R.id.check_thursday),
            "Friday" to findViewById<CheckBox>(R.id.check_friday),
            "Saturday" to findViewById<CheckBox>(R.id.check_saturday),
            "Sunday" to findViewById<CheckBox>(R.id.check_sunday)
        )

        try {
            val targetGrade = targetGradeEdit.text.toString().toDoubleOrNull() ?: 0.0
            val sex = sexEdit.text.toString().toDoubleOrNull() ?: 0.0
            var height = heightEdit.text.toString().toDoubleOrNull() ?: 0.0
            var weight = weightEdit.text.toString().toDoubleOrNull() ?: 0.0
            var wPull = weightedPullEdit.text.toString().toDoubleOrNull() ?: 0.0
            var mHang = maxHangEdit.text.toString().toDoubleOrNull() ?: 0.0
            var pL = powlEdit.text.toString().toDoubleOrNull() ?: 0.0
            var pR = powrEdit.text.toString().toDoubleOrNull() ?: 0.0

            if (isMetric) {
                height = cmToInch(height)
                weight = kgToLb(weight)
                wPull = kgToLb(wPull)
                mHang = kgToLb(mHang)
                pL = cmToInch(pL)
                pR = cmToInch(pR)
            }

            val actuals = mapOf(
                "pullups" to (pullupsEdit.text.toString().toDoubleOrNull() ?: 0.0),
                "weighted_pull" to wPull,
                "maxhang" to mHang,
                "continuoushang" to (continuousHangEdit.text.toString().toDoubleOrNull() ?: 0.0),
                "powl" to pL,
                "powr" to pR,
                "pushups" to (pushupsEdit.text.toString().toDoubleOrNull() ?: 0.0)
            )

            val activeDays = checkBoxes.filter { it.value.isChecked }.keys.toList()
            val numWeeks = numWeeksEdit.text.toString().toIntOrNull() ?: 4

            val weeklyPlan = TrainingPlanGenerator.generatePlanStructured(targetGrade, sex, height, weight, actuals, activeDays)
            
            // Convert to a specific HashMap of ArrayLists to guarantee Serializability
            val serializablePlan = HashMap<String, ArrayList<TrainingExercise>>()
            weeklyPlan.forEach { (day, exercises) ->
                serializablePlan[day] = ArrayList(exercises)
            }

            val intent = Intent(this, PlanPreviewActivity::class.java).apply {
                putExtra("weeklyPlan", serializablePlan)
                putExtra("numWeeks", numWeeks)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Log.e("GenerateWorkout", "Error during generation", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}