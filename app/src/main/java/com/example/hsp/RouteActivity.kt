package com.example.hsp

import android.content.Intent
import android.graphics.Path
import android.graphics.PathMeasure
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.hsp.databinding.ActivityRouteBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RouteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRouteBinding
    private lateinit var bottomNavManager: BottomNavManager
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = Firebase.firestore

        // 1. Initialize Navigation Bar using your BottomNavManager
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavManager = BottomNavManager(this, bottomNavigationView)

        // 2. Setup Logout (matching CalendarActivity style)
        findViewById<MaterialButton>(R.id.logout_button).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 3. Wait for image to load to calculate the path coordinates
        binding.routeImage.post {
            getCompletionProgress()
        }
    }

    private fun getCompletionProgress() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val completed = doc.getDouble("Completed") ?: 0.0
                val length = doc.getDouble("Length") ?: 1.0
                val progress = (completed / length).toFloat().coerceIn(0f, 1f)

                drawPathAndPositionClimber(progress)
            }
        }.addOnFailureListener { e ->
            Log.e("RouteActivity", "Error fetching progress", e)
        }
    }

    private fun drawPathAndPositionClimber(progress: Float) {
        val w = binding.routeImage.width.toFloat()
        val h = binding.routeImage.height.toFloat()

        // Define the Route Path (coordinates scaled to image size)
        // These points approximate the red line from bottom to top
        val routePath = Path().apply {
            moveTo(w * 0.78f, h * 0.95f) // Start (Bottom Right)
            lineTo(w * 0.81f, h * 0.85f)
            lineTo(w * 0.75f, h * 0.75f)
            lineTo(w * 0.78f, h * 0.68f)
            lineTo(w * 0.70f, h * 0.65f) // The big horizontal traverse
            lineTo(w * 0.55f, h * 0.65f)
            lineTo(w * 0.45f, h * 0.60f)
            lineTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.32f, h * 0.35f)
            lineTo(w * 0.32f, h * 0.25f)
            lineTo(w * 0.28f, h * 0.15f)
            lineTo(w * 0.28f, h * 0.05f) // Top (End)
        }

        // Use PathMeasure to find the point at 'progress' distance along the path
        val pathMeasure = PathMeasure(routePath, false)
        val pathLength = pathMeasure.length
        val pos = FloatArray(2)

        pathMeasure.getPosTan(pathLength * progress, pos, null)

        // Move the marker to the exact coordinates on the red line
        binding.climberMarker.animate()
            .x(pos[0] - (binding.climberMarker.width / 2))
            .y(pos[1] - (binding.climberMarker.height / 2))
            .setDuration(2000)
            .start()
    }

    override fun onResume() {
        super.onResume()
        // Highlight the Route tab in the navbar
        bottomNavManager.setupBottomNav(R.id.nav_route)
    }
}