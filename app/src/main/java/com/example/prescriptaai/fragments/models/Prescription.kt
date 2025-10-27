package com.example.prescriptaai.fragments.models
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// @Parcelize allows us to easily pass this whole object between activities
@Parcelize
data class Prescription(
    val patientName: String?,
    val patientPhoneNumber: String?, // Note: This will be null from Gemini
    val age: String?,
    val diagnosis: String?,
    val medications: List<Medication>?,
    val prescriptionDate: String?
) : Parcelable

@Parcelize
data class Medication(
    val name: String?,
    val dosage: String?,
    val frequency: String?,
    val duration: String?
) : Parcelable