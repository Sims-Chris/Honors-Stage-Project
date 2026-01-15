package com.example.hsp

import android.content.Context
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavManager(private val context: Context, private val bottomNavigationView: BottomNavigationView) {

    fun setupBottomNav(selectedItemId: Int) {
        bottomNavigationView.selectedItemId = selectedItemId

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val intent = when (menuItem.itemId) {
                R.id.nav_home -> Intent(context, HomeActivity::class.java)
                R.id.nav_calendar -> Intent(context, CalendarActivity::class.java)
                R.id.nav_log -> Intent(context, LogActivity::class.java)
                R.id.nav_profile -> Intent(context, ProfileActivity::class.java)
                else -> null
            }

            if (intent != null) {
                val destinationClass = intent.component?.className
                val currentClass = context::class.java.name

                if (destinationClass != currentClass) {
                    // This flag brings an existing activity to the front without destroying the stack,
                    // preserving the state of screens like WorkoutActivity.
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    context.startActivity(intent)
                }
            }

            true
        }
    }
}
