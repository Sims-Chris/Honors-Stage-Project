package com.example.hsp

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatsActivity : AppCompatActivity() {
    private lateinit var bottomNavManager: BottomNavManager
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var isMetric = true

    private lateinit var pbRecycler: RecyclerView
    private val pbList = mutableListOf<PBEntry>()
    private lateinit var pbAdapter: PBAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initPBList()
        initViews()
        fetchPBs()
    }

    private fun initPBList() {
        pbList.clear()
        pbList.add(PBEntry("max_weighted_pull", "Max Weighted Pull Up", "Total weight including bodyweight", 0.0))
        pbList.add(PBEntry("max_hang_20mm", "Max Hang (20mm)", "Extra weight added to bodyweight", 0.0))
        pbList.add(PBEntry("pull_ups_count", "Max Pull Ups", "Maximum consecutive repetitions", 0.0, "reps"))
        pbList.add(PBEntry("push_ups_count", "Max Push Ups", "Maximum consecutive repetitions", 0.0, "reps"))
        pbList.add(PBEntry("power_left", "Power Left", "Explosive movement distance (Left)", 0.0, "cm/in"))
        pbList.add(PBEntry("power_right", "Power Right", "Explosive movement distance (Right)", 0.0, "cm/in"))
    }

    private fun initViews() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavManager = BottomNavManager(this, bottomNavigationView)

        pbRecycler = findViewById(R.id.pb_recycler)
        pbRecycler.layoutManager = LinearLayoutManager(this)
        pbAdapter = PBAdapter(pbList) { entry -> showUpdateDialog(entry) }
        pbRecycler.adapter = pbAdapter

        val unitToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.unit_toggle_group)
        unitToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isMetric = checkedId == R.id.btn_metric
                pbAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchPBs() {
        val currentUser = auth.currentUser ?: return
        db.collection("Users").document(currentUser.uid).collection("Stats").document("PersonalBests")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    pbList.forEach { entry ->
                        entry.value = document.getDouble(entry.id) ?: 0.0
                    }
                    pbAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun showUpdateDialog(entry: PBEntry) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update ${entry.name}")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Enter new PB"
        builder.setView(input)

        builder.setPositiveButton("Update") { _, _ ->
            val newValue = input.text.toString().toDoubleOrNull()
            if (newValue != null) {
                updatePBInDB(entry, newValue)
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updatePBInDB(entry: PBEntry, value: Double) {
        val currentUser = auth.currentUser ?: return
        val updates = mapOf(entry.id to value)
        
        db.collection("Users").document(currentUser.uid).collection("Stats").document("PersonalBests")
            .update(updates)
            .addOnFailureListener {
                // If document doesn't exist, create it
                db.collection("Users").document(currentUser.uid).collection("Stats").document("PersonalBests")
                    .set(updates)
            }
            .addOnSuccessListener {
                entry.value = value
                pbAdapter.notifyDataSetChanged()
                Toast.makeText(this, "PB Updated!", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        bottomNavManager.setupBottomNav(R.id.nav_stats)
    }

    data class PBEntry(
        val id: String,
        val name: String,
        val desc: String,
        var value: Double,
        val unitOverride: String? = null
    )

    class PBViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.exercise_name)
        val desc: TextView = view.findViewById(R.id.exercise_desc)
        val value: TextView = view.findViewById(R.id.pb_value)
        val unit: TextView = view.findViewById(R.id.pb_unit)
    }

    inner class PBAdapter(private val items: List<PBEntry>, private val onClick: (PBEntry) -> Unit) : 
        RecyclerView.Adapter<PBViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PBViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pb, parent, false)
            return PBViewHolder(view)
        }

        override fun onBindViewHolder(holder: PBViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.desc.text = item.desc
            
            // Basic unit conversion for display
            var displayValue = item.value
            var displayUnit = item.unitOverride ?: if (isMetric) "kg" else "lbs"
            
            if (item.unitOverride == "cm/in") {
                displayUnit = if (isMetric) "cm" else "in"
            } else if (item.unitOverride == null && !isMetric) {
                displayValue *= 2.20462 // Simple kg to lbs
            }

            holder.value.text = String.format("%.1f", displayValue)
            holder.unit.text = displayUnit
            
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
