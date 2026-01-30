package com.example.hsp

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavManager(private val activity: Activity, private val bottomNavigationView: BottomNavigationView) {

    fun setupBottomNav(selectedItemId: Int) {
        bottomNavigationView.selectedItemId = selectedItemId

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            // 1. Determine the destination class
            val targetActivityClass = when (menuItem.itemId) {
                R.id.nav_home -> HomeActivity::class.java
                R.id.nav_calendar -> CalendarActivity::class.java
                R.id.nav_log -> LogActivity::class.java
                R.id.nav_profile -> ProfileActivity::class.java
                else -> null
            }

            // 2. Logic to handle navigation
            targetActivityClass?.let { target ->
                // Compare the activity instance's class to the target
                if (activity::class.java != target) {
                    val intent = Intent(activity, target).apply {
                        // Reorders existing activity to front to preserve state
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }

                    activity.startActivity(intent)

                    // 3. Remove the transition animation for a seamless tab switch
                    activity.overridePendingTransition(0, 0)
                }
            }
            true
        }
    }
}