package com.example.prescriptaai.fragments

import com.example.prescriptaai.fragments.models.Prescription

// This data class holds a Prescription object along with its Firestore document ID.
// It's used by PatientPrescriptionAdapter to pass both pieces of info when an item is clicked.
data class PrescriptionWithId(
    val prescription: Prescription, // The actual prescription data
    val documentId: String          // The unique ID of this document in Firestore
)