package com.example.prescriptaai.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prescriptaai.R
import com.example.prescriptaai.databinding.ActivityConfirmPrescriptionBinding
import com.example.prescriptaai.fragments.models.Prescription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import android.util.Log // Keep this import for debugging

class ConfirmPrescriptionActivity : AppCompatActivity() {

    private var currentPrescription: Prescription? = null
    private lateinit var binding: ActivityConfirmPrescriptionBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmPrescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get the JSON string passed from ScanFragment
        val jsonResponse = intent.getStringExtra("GEMINI_RESPONSE")

        if (jsonResponse != null) {
            val gson = Gson()
            try {
                // Clean the JSON string by removing markdown fences
                val cleanedJsonResponse = jsonResponse
                    .trim() // Trim all leading/trailing whitespace
                    .removePrefix("```json") // Remove the code block identifier
                    .removeSuffix("```") // Remove the closing code block fences
                    .trim() // Trim again in case of newlines introduced by prefix/suffix removal

                Log.d("ConfirmActivity", "Cleaned JSON: '$cleanedJsonResponse'") // For debugging, note the quotes!
                currentPrescription = gson.fromJson(cleanedJsonResponse, Prescription::class.java) // Assign to member variable
                currentPrescription?.let {
                    populateUi(it) // Pass the non-null prescription to populateUi
                } ?: run { // Handle if parsing somehow results in null
                    Toast.makeText(this, "Error: Parsed prescription is empty.", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("ConfirmActivity", "Error parsing JSON: ${e.message}", e) // Log the specific error
                Toast.makeText(this, "Error parsing scanned data.", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Error: No data received.", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnSaveRecord.setOnClickListener {
            savePrescriptionToFirestore()
        }
    }

    private fun populateUi(prescription: Prescription) {
        binding.etPatientName.setText(prescription.patientName)
        binding.etDiagnosis.setText(prescription.diagnosis)
        // Phone number is intentionally left blank for the doctor to fill in

        // Dynamically create and add views for each medication
        binding.medicationsContainer.removeAllViews() // Clear previous views if any
        prescription.medications?.forEach { medication ->
            val medicationView = LayoutInflater.from(this).inflate(R.layout.item_medication, binding.medicationsContainer, false)

            val medicationNameTextView = medicationView.findViewById<TextView>(R.id.tvMedicationName)
            val medicationDetailsTextView = medicationView.findViewById<TextView>(R.id.tvMedicationDetails)

            medicationNameTextView.text = medication.name ?: "N/A"

            // UPDATED UI FORMATTING
            medicationDetailsTextView.text = buildString {
                append("Dosage: ${medication.dosage ?: "N/A"}")
                append("\nFrequency: ${medication.frequency ?: "N/A"}")
                append("\nDuration: ${medication.duration ?: "N/A"}")
            }

            binding.medicationsContainer.addView(medicationView)
        }
    }

    private fun savePrescriptionToFirestore() {
        val patientPhoneNumber = binding.etPhoneNumber.text.toString().trim()
        val patientName = binding.etPatientName.text.toString().trim()
        val currentDoctorId = auth.currentUser?.uid

        if (patientPhoneNumber.isEmpty()) {
            binding.etPhoneNumber.error = "Phone number is required"
            return
        }
        if (currentDoctorId == null) {
            Toast.makeText(this, "Error: Doctor not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val prescriptionToSave = currentPrescription ?: run {
            Toast.makeText(this, "No prescription data to save.", Toast.LENGTH_SHORT).show()
            return // Exit if no data
        }

        // Convert List<Medication> to List<Map<String, Any>> for Firestore
        val medicationsForFirestore = prescriptionToSave.medications?.map { med ->
            hashMapOf(
                "name" to (med.name ?: ""),
                "dosage" to (med.dosage ?: ""),
                "frequency" to (med.frequency ?: ""),
                "duration" to (med.duration ?: "")
            )
        } ?: emptyList()

        val prescriptionRecord = hashMapOf(
            "patientName" to patientName,
            "patientPhoneNumber" to patientPhoneNumber,
            "age" to (prescriptionToSave.age ?: ""),
            "diagnosis" to binding.etDiagnosis.text.toString().trim(),
            "prescriptionDate" to FieldValue.serverTimestamp(), // Use server time
            "doctorId" to currentDoctorId,
            "medications" to medicationsForFirestore // This specific prescription's medications
        )

        // --- CORRECTED: Define patientRecord for the main patient document (NO medications here) ---
        val patientRecord = hashMapOf(
            "patientName" to patientName,
            "patientPhoneNumber" to patientPhoneNumber,
            "lastUpdated" to FieldValue.serverTimestamp(),
            "doctorId" to currentDoctorId // This links patient to doctor
        )

        val patientDocRef = firestore.collection("patients").document(patientPhoneNumber)

        firestore.runTransaction { transaction ->
            // Create/update the main patient document (without medications from THIS prescription)
            transaction.set(patientDocRef, patientRecord)

            // Add the new prescription to the 'prescriptions' sub-collection
            val newPrescriptionRef = patientDocRef.collection("prescriptions").document()
            transaction.set(newPrescriptionRef, prescriptionRecord)

            val doctorDocRef = firestore.collection("doctors").document(currentDoctorId)
            transaction.update(doctorDocRef, "prescriptionCount", FieldValue.increment(1))

            null // Return null for a successful transaction
        }.addOnSuccessListener {
            Toast.makeText(this, "Record saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error saving record: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}