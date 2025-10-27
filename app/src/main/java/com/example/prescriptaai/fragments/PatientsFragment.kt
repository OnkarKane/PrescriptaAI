package com.example.prescriptaai.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prescriptaai.activity.PatientDetailActivity // <-- NEW IMPORT
import com.example.prescriptaai.databinding.FragmentPatientsBinding
import com.example.prescriptaai.fragments.models.Patient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PatientsFragment : Fragment() {

    private var _binding: FragmentPatientsBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var patientsAdapter: PatientsAdapter
    private val patientsList = mutableListOf<Patient>()
    private var patientsFirestoreListener: ListenerRegistration? = null // <-- NEW: To store the listener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        fetchPatients()
    }

    private fun setupRecyclerView() {
        patientsAdapter = PatientsAdapter(patientsList)
        binding.recyclerViewPatients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = patientsAdapter
        }

        patientsAdapter.onItemClick = { patient ->
            if (patient.patientPhoneNumber != null) {
                val intent = android.content.Intent(requireContext(), PatientDetailActivity::class.java).apply {
                    putExtra("PATIENT_PHONE_NUMBER", patient.patientPhoneNumber)
                }
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Error: Patient phone number missing.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchPatients() // Start/re-attach listener when fragment is active
    }

    override fun onPause() {
        super.onPause()
        patientsFirestoreListener?.remove() // <-- NEW: Detach listener when fragment is paused
    }

    private fun fetchPatients() {
        val currentDoctorId = auth.currentUser?.uid
        if (currentDoctorId == null) {
            if (isAdded) binding.tvNoPatients.visibility = View.VISIBLE // Safely update UI
            if (isAdded && requireContext() != null) {
                Toast.makeText(requireContext(), "Error: Doctor not logged in.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        patientsFirestoreListener = firestore.collection("patients") // <-- Assign to the listener variable
            .whereEqualTo("doctorId", currentDoctorId)
            .orderBy("lastUpdated", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("PatientsFragment", "Listen for Query failed.", e)
                    if (isAdded) { // <-- NEW: Check if fragment is attached
                        // Only show toast if fragment is still active
                        if (isAdded && requireContext() != null) {
                            Toast.makeText(requireContext(), "Error loading patients.", Toast.LENGTH_SHORT).show()
                        }
                        binding.tvNoPatients.visibility = View.VISIBLE
                    }
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val fetchedPatients = mutableListOf<Patient>()
                    for (doc in snapshots.documents) {
                        val patient = doc.toObject(Patient::class.java)
                        val patientWithId = patient?.copy(patientPhoneNumber = doc.id)
                        patientWithId?.let { fetchedPatients.add(it) }
                    }
                    if (isAdded) { // <-- NEW: Check if fragment is attached before UI updates
                        patientsAdapter.updateData(fetchedPatients)
                        binding.recyclerViewPatients.visibility = View.VISIBLE
                        binding.tvNoPatients.visibility = View.GONE
                    }
                } else {
                    Log.d("PatientsFragment", "No patients for doctor $currentDoctorId")
                    if (isAdded) { // <-- NEW: Check if fragment is attached before UI updates
                        binding.recyclerViewPatients.visibility = View.GONE
                        binding.tvNoPatients.visibility = View.VISIBLE
                    }
                }
            }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}