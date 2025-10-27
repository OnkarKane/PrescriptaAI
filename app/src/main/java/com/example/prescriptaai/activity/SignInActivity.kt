package com.example.prescriptaai.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prescriptaai.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.tvGoToSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, HomeActivity::class.java).apply {
                            // --- NEW: Clear activity stack ---
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            // --- END NEW ---
                        }
                        startActivity(intent)
                        finish() // Keep finish() for good measure, though flags are usually sufficient
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    currentUser.reload().await()
                    val refreshedUser = firebaseAuth.currentUser
                    if (refreshedUser != null) { // Removed isEmailVerified check for simplicity based on previous discussions
                        val intent = Intent(this@SignInActivity, HomeActivity::class.java).apply {
                            // --- NEW: Clear activity stack ---
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            // --- END NEW ---
                        }
                        startActivity(intent)
                        finish() // Keep finish()
                    } else {
                        firebaseAuth.signOut()
                        Toast.makeText(this@SignInActivity, "Session invalid. Please sign in again.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    firebaseAuth.signOut()
                    Toast.makeText(this@SignInActivity, "Session expired or invalid. Please sign in again.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}