package com.example.hsp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)
        setupUI()
    }

    private fun setupUI() {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggle_button_group)
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.login_toggle_button -> {
                        setContentView(R.layout.login)
                        setupUI()
                    }
                    R.id.signup_toggle_button -> {
                        setContentView(R.layout.signup)
                        setupUI()
                    }
                }
            }
        }

        if (findViewById<Button>(R.id.login) != null) {
            setupLoginScreen()
        } else {
            setupSignUpScreen()
        }
    }

    private fun setupLoginScreen() {
        db = FirebaseFirestore.getInstance()

        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val login = findViewById<Button>(R.id.login)

        login.setOnClickListener {
            val email = username.text.toString()
            val password = password.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                db.collection("Users")
                    .whereEqualTo("Email", email)
                    .whereEqualTo("Password", password)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSignUpScreen() {
        db = FirebaseFirestore.getInstance()

        val firstName = findViewById<EditText>(R.id.first_name)
        val lastName = findViewById<EditText>(R.id.last_name)
        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val signupButton = findViewById<Button>(R.id.signup_button)

        signupButton.setOnClickListener {
            val fName = firstName.text.toString()
            val lName = lastName.text.toString()
            val email = username.text.toString()
            val pass = password.text.toString()

            if (fName.isNotEmpty() && lName.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                val user = hashMapOf(
                    "FirstName" to fName,
                    "LastName" to lName,
                    "Email" to email,
                    "Password" to pass
                )

                db.collection("Users")
                    .add(user)
                    .addOnSuccessListener {                        Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                        setContentView(R.layout.login)
                        setupUI()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
