package com.example.prescriptaai.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.prescriptaai.databinding.FragmentHomeBinding

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // ListenerRegistrations for real-time updates
    private var patientsListener: ListenerRegistration? = null
    private var prescriptionsCountListener: ListenerRegistration? = null //

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Fetch and display the doctor's name on creation
        fetchDoctorProfile()
    }

    override fun onResume() {
        super.onResume()
        // Fetch counts when the fragment becomes visible
        fetchPatientCounts()
        fetchPrescriptionCounts()
    }

    override fun onPause() {
        super.onPause()
        // Detach listeners when fragment is no longer visible to prevent memory leaks
        patientsListener?.remove()
        prescriptionsCountListener?.remove()
    }

    private fun fetchDoctorProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val docRef = firestore.collection("doctors").document(userId)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        if (name != null) {
                            binding.tvDoctorName.text = "Dr. $name"
                        } else {
                            binding.tvDoctorName.text = "Dr. [Name Missing]" // Fallback
                        }
                    } else {
                        Log.d("HomeFragment", "No such document for user: $userId")
                        binding.tvDoctorName.text = "Doctor" // Fallback text
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("HomeFragment", "Error getting document", exception)
                    Toast.makeText(activity, "Error fetching profile.", Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.tvDoctorName.text = "Doctor" // No user logged in
        }
    }

    private fun fetchPatientCounts() {
        val currentDoctorId = auth.currentUser?.uid
        if (currentDoctorId == null) {
            if (isAdded) binding.tvPatientCount.text = "0" // Safely update UI
            return
        }

        patientsListener = firestore.collection("patients")
            .whereEqualTo("doctorId", currentDoctorId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen for patient count failed.", e)
                    if (isAdded) {
                        binding.tvPatientCount.text = "Error"
                        // Only show toast if fragment is still active
                        if (isAdded && requireContext() != null) {
                            Toast.makeText(requireContext(), "Error loading patient count.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    if (isAdded) binding.tvPatientCount.text = snapshots.size().toString()
                } else {
                    if (isAdded) binding.tvPatientCount.text = "0"
                }
            }
    }

    private fun fetchPrescriptionCounts() {
        val currentDoctorId = auth.currentUser?.uid
        if (currentDoctorId == null) {
            if (isAdded) binding.tvPrescriptionCount.text = "0" // Safely update UI
            return
        }

        val doctorDocRef = firestore.collection("doctors").document(currentDoctorId)
        prescriptionsCountListener = doctorDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen for doctor prescription count failed.", e)
                if (isAdded) { // <-- NEW: Check if fragment is attached
                    binding.tvPrescriptionCount.text = "Error"
                    // Only show toast if fragment is still active
                    if (isAdded && requireContext() != null) {
                        Toast.makeText(requireContext(), "Error loading prescription count.", Toast.LENGTH_SHORT).show()
                    }
                }
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val count = snapshot.getLong("prescriptionCount") ?: 0
                if (isAdded) binding.tvPrescriptionCount.text = count.toString()
            } else {
                if (isAdded) binding.tvPrescriptionCount.text = "0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners to prevent memory leaks
        patientsListener?.remove()
        prescriptionsCountListener?.remove()
        _binding = null
    }
}