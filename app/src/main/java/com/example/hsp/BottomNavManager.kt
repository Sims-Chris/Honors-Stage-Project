package com.example.hsp

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavManager(private val activity: Activity, private val bottomNavigationView: BottomNavigationView) {

    fun setupBottomNav(selectedItemId: Int) {
        // Set the correct icon as selected without triggering the listener
        bottomNavigationView.setOnItemSelectedListener(null)
        bottomNavigationView.selectedItemId = selectedItemId

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val targetActivityClass = when (menuItem.itemId) {
                R.id.nav_home -> HomeActivity::class.java
                R.id.nav_calendar -> CalendarActivity::class.java
                R.id.nav_route -> RouteActivity::class.java
                R.id.nav_profile -> ProfileActivity::class.java
                else -> null
            }

            if (targetActivityClass != null && activity::class.java != targetActivityClass) {
                val intent = Intent(activity, targetActivityClass)
                // Re-use the existing activity instance if it's in the backstack
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                
                // Add a simple crossfade animation for a smoother feel
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                true
            } else {
                // If we are already on the target activity, return true but do nothing
                true
            }
        }
    }
}