package com.example.prescriptaai.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prescriptaai.R
import com.example.prescriptaai.fragments.models.Patient
import java.text.SimpleDateFormat
import java.util.*

class PatientsAdapter(private val patients: MutableList<Patient>) :
    RecyclerView.Adapter<PatientsAdapter.PatientViewHolder>() {

    // You can add a click listener here later
    var onItemClick: ((Patient) -> Unit)? = null

    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        val tvPatientPhoneNumber: TextView = itemView.findViewById(R.id.tvPatientPhoneNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]
        holder.tvPatientName.text = patient.patientName ?: "Unknown Patient"

        val phoneNumberText = patient.patientPhoneNumber ?: "N/A"

        // Format last updated time for display if available
        val formattedLastUpdated = patient.lastUpdated?.toDate()?.let { date -> // <-- SIMPLIFIED
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
        } ?: "Never updated"

        holder.tvPatientPhoneNumber.text = "$phoneNumberText (Last visit: $formattedLastUpdated)"

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(patient)
        }
    }

    override fun getItemCount(): Int = patients.size

    // Helper function to update data in the adapter
    fun updateData(newPatients: List<Patient>) {
        patients.clear()
        patients.addAll(newPatients)
        notifyDataSetChanged()
    }
}