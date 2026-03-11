package com.example.hsp

import com.google.firebase.firestore.FieldPath
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {
    private lateinit var bottomNavManager: BottomNavManager
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private lateinit var totalWorkoutsVal: TextView
    private lateinit var streakVal: TextView
    private lateinit var rateVal: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var historyRecycler: RecyclerView
    private lateinit var badgeNewbie: ImageView
    private lateinit var badgeStreak: ImageView
    private lateinit var badgePro: ImageView

    private val logList = mutableListOf<LogEntry>()
    private lateinit var logAdapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews()
        setupRecyclerView()
        setupFilters()
        setupAchievementClicks()
        
        fetchStats("all_time")
        fetchHistory()
    }

    private fun initViews() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavManager = BottomNavManager(this, bottomNavigationView)

        totalWorkoutsVal = findViewById(R.id.total_workouts_val)
        streakVal = findViewById(R.id.streak_val)
        rateVal = findViewById(R.id.rate_val)
        progressBar = findViewById(R.id.weekly_progress_bar)
        historyRecycler = findViewById(R.id.recent_history_recycler)
        badgeNewbie = findViewById(R.id.badge_newbie)
        badgeStreak = findViewById(R.id.badge_streak)
        badgePro = findViewById(R.id.badge_pro)
    }

    private fun setupRecyclerView() {
        logAdapter = LogAdapter(logList)
        historyRecycler.layoutManager = LinearLayoutManager(this)
        historyRecycler.adapter = logAdapter
    }

    private fun setupFilters() {
        findViewById<MaterialButtonToggleGroup>(R.id.filter_toggle_group).addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_week -> fetchStats("week")
                    R.id.btn_month -> fetchStats("month")
                    R.id.btn_all_time -> fetchStats("all_time")
                }
            }
        }
    }

    private fun setupAchievementClicks() {
        val achievements = mapOf(
            R.id.badge_newbie_container to "First Step: Complete your first workout to unlock.",
            R.id.badge_streak_container to "On Fire: Reach a 5-day workout streak to unlock.",
            R.id.badge_pro_container to "Elite: Complete 50 total workouts to unlock."
        )

        achievements.forEach { (id, description) ->
            findViewById<View>(id)?.setOnClickListener {
                Toast.makeText(this, description, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchStats(filter: String) {
        val currentUser = auth.currentUser ?: return
        
        db.collection("Users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val completed = document.getLong("TotalCompleted") ?: 0
                    val planned = document.getLong("TotalPlanned") ?: 0
                    
                    totalWorkoutsVal.text = completed.toString()
                    
                    if (planned > 0) {
                        val rate = (completed.toFloat() / planned.toFloat() * 100).toInt()
                        rateVal.text = "$rate%"
                    }

                    // Achievements Logic
                    if (completed >= 1) badgeNewbie.alpha = 1.0f
                    if (completed >= 50) badgePro.alpha = 1.0f
                }
            }

        // Fetch Streak (Simplified for Stats)
        calculateStreak(currentUser.uid)
        
        // Weekly Progress (Hardcoded goal of 4 for demo, usually from user profile)
        updateWeeklyProgress(currentUser.uid)
    }

    private fun calculateStreak(uid: String) {
        // Reusing basic logic from HomeActivity
        db.collection("Users").document(uid).collection("Training Plan")
            .get()
            .addOnSuccessListener { documents ->
                var streak = 0
                // Simplified streak calc for stats screen
                // In a real app, this would be a shared utility
                streakVal.text = "5" // Placeholder for visual demo
                badgeStreak.alpha = 1.0f
            }
    }

    private fun updateWeeklyProgress(uid: String) {
        // Count workouts completed in the last 7 days
        progressBar.progress = 75 // Placeholder 3/4 days
    }

    private fun fetchHistory() {
        val currentUser = auth.currentUser ?: return
        db.collection("Users").document(currentUser.uid).collection("Training Plan")
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                logList.clear()
                for (doc in documents) {
                    val date = doc.id
                    val exercises = doc.get("exercises") as? List<Map<String, Any>>
                    val completed = exercises?.all { it["Completed"] as? Boolean == true } ?: false
                    
                    if (exercises != null) {
                        logList.add(LogEntry(
                            title = if (completed) "Workout Completed" else "Partial Workout",
                            date = date,
                            status = if (completed) "Done" else "Pending",
                            isCompleted = completed
                        ))
                    }
                }
                logAdapter.notifyDataSetChanged()
            }
    }

    override fun onResume() {
        super.onResume()
        bottomNavManager.setupBottomNav(R.id.nav_stats)
    }

    data class LogEntry(
        val title: String,
        val date: String,
        val status: String,
        val isCompleted: Boolean
    )

    class StatsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.log_title)
        val date: TextView = view.findViewById(R.id.log_date)
        val status: TextView = view.findViewById(R.id.log_status)
        val icon: ImageView = view.findViewById(R.id.log_icon)
    }

    inner class LogAdapter(private val items: List<LogEntry>) : RecyclerView.Adapter<StatsViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_log, parent, false)
            return StatsViewHolder(view)
        }

        override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.date.text = item.date
            holder.status.text = item.status
            
            if (item.isCompleted) {
                holder.icon.setImageResource(R.drawable.check)
                holder.status.setTextColor(getColor(R.color.buttonColour))
            } else {
                holder.icon.setImageResource(R.drawable.cross)
                holder.status.setTextColor(getColor(android.R.color.holo_red_light))
            }
        }

        override fun getItemCount() = items.size
    }
}
