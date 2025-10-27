package com.example.prescriptaai.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prescriptaai.databinding.ActivityPatientDetailBinding
import com.example.prescriptaai.fragments.PatientPrescriptionAdapter
import com.example.prescriptaai.fragments.models.Medication
import com.example.prescriptaai.fragments.models.Prescription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

const val EXTRA_PATIENT_PHONE_NUMBER = "PATIENT_PHONE_NUMBER"

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var prescriptionsAdapter: PatientPrescriptionAdapter
    private val prescriptionList = mutableListOf<Prescription>() // <-- CRITICAL: Now only Prescription objects

    private var patientPhoneNumber: String? = null
    private var prescriptionsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        patientPhoneNumber = intent.getStringExtra(EXTRA_PATIENT_PHONE_NUMBER)

        if (patientPhoneNumber == null) {
            Toast.makeText(this, "Error: Patient phone number not provided.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        supportActionBar?.title = "Patient Details"

        setupRecyclerView()
        fetchPatientDetails(patientPhoneNumber!!)
        fetchPatientPrescriptions(patientPhoneNumber!!)
    }

    override fun onStop() {
        super.onStop()
        prescriptionsListener?.remove()
    }

    override fun onDestroy() {
        super.onDestroy()
        prescriptionsListener?.remove()
    }

    private fun setupRecyclerView() {
        prescriptionsAdapter = PatientPrescriptionAdapter(prescriptionList) // <-- Pass only Prescription list
        binding.recyclerViewPrescriptions.apply {
            layoutManager = LinearLayoutManager(this@PatientDetailActivity)
            adapter = prescriptionsAdapter
        }

        // --- NEW: Simplified onItemClick as we are not launching a new Activity for viewing ---
        // This will just show a toast for now, or you could add a dialog to view more details.
        prescriptionsAdapter.onItemClick = { prescription ->
            Toast.makeText(this, "Viewing prescription for ${prescription.prescriptionDate}", Toast.LENGTH_SHORT).show()
            // If you need more detailed view, you can open a DialogFragment here
        }
        // --- END NEW ---
    }

    private fun fetchPatientDetails(phoneNumber: String) {
        firestore.collection("patients").document(phoneNumber).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("patientName") ?: "Unknown"
                    binding.tvPatientDetailName.text = name
                    binding.tvPatientDetailPhoneNumber.text = phoneNumber
                } else {
                    Toast.makeText(this, "Patient details not found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PatientDetail", "Error fetching patient details: ${e.message}", e)
                Toast.makeText(this, "Error loading patient details.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun fetchPatientPrescriptions(phoneNumber: String) {
        prescriptionsListener?.remove()

        prescriptionsListener = firestore.collection("patients").document(phoneNumber)
            .collection("prescriptions")
            .orderBy("prescriptionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("PatientDetail", "Listen for prescriptions failed.", e)
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "Error loading prescriptions.", Toast.LENGTH_SHORT).show()
                        binding.tvNoPrescriptions.visibility = View.VISIBLE
                    }
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val fetchedPrescriptions = mutableListOf<Prescription>()
                    for (doc in snapshots.documents) {
                        try {
                            val diagnosis = doc.getString("diagnosis")
                            val patientName = doc.getString("patientName")
                            val patientPhoneNumberFromDb = doc.getString("patientPhoneNumber")
                            val age = doc.getString("age")

                            val prescriptionDateTimestamp = doc.getTimestamp("prescriptionDate")?.toDate()
                            val prescriptionDate = prescriptionDateTimestamp?.let {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                            }

                            val medicationsRaw = doc.get("medications") as? List<Map<String, Any>>
                            val medications = medicationsRaw?.map { medMap ->
                                Medication(
                                    name = medMap["name"] as? String,
                                    dosage = medMap["dosage"] as? String,
                                    frequency = medMap["frequency"] as? String,
                                    duration = medMap["duration"] as? String
                                )
                            }

                            val prescription = Prescription(
                                patientName = patientName,
                                patientPhoneNumber = patientPhoneNumberFromDb,
                                age = age,
                                diagnosis = diagnosis,
                                medications = medications,
                                prescriptionDate = prescriptionDate
                            )
                            fetchedPrescriptions.add(prescription) // Add just the Prescription object
                        } catch (ex: Exception) {
                            Log.e("PatientDetail", "Error mapping prescription document: ${ex.message}", ex)
                        }
                    }
                    if (!isFinishing && !isDestroyed) {
                        prescriptionsAdapter.updateData(fetchedPrescriptions) // Pass list of Prescription
                        binding.recyclerViewPrescriptions.visibility = View.VISIBLE
                        binding.tvNoPrescriptions.visibility = View.GONE
                    }
                } else {
                    Log.d("PatientDetail", "No prescriptions for patient $phoneNumber")
                    if (!isFinishing && !isDestroyed) {
                        binding.recyclerViewPrescriptions.visibility = View.GONE
                        binding.tvNoPrescriptions.visibility = View.VISIBLE
                    }
                }
            }
    }
}