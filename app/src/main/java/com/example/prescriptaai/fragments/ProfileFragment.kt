package com.example.prescriptaai.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.prescriptaai.activity.SignInActivity
import com.example.prescriptaai.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Fetch and display the doctor's info
        fetchDoctorProfile()

        // Set up the logout button listener
        binding.btnLogout.setOnClickListener {
            // Sign out from Firebase
            auth.signOut()

            val intent = Intent(activity, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish() // Close the HomeActivity
        }
    }

    private fun fetchDoctorProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val docRef = firestore.collection("doctors").document(userId)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Doctor"
                        val hospital = document.getString("hospital") ?: "Hospital"

                        // Set the text on the UI elements
                        binding.tvDoctorNameProfile.text = "Dr. $name"
                        binding.tvHospitalNameProfile.text = hospital
                    } else {
                        Log.d("ProfileFragment", "No such document for user: $userId")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileFragment", "Error getting document", exception)
                    Toast.makeText(activity, "Error fetching profile.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}