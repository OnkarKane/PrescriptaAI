package com.example.prescriptaai.fragments.models
import com.google.firebase.Timestamp // <-- Firebase Timestamp
// No Parcelable needed here as we're not passing this object directly between activities yet
data class Patient(
    val patientName: String? = null,
    val patientPhoneNumber: String? = null,
    val doctorId: String? = null, // To filter patients by doctor
    val lastUpdated: Timestamp? = null
)