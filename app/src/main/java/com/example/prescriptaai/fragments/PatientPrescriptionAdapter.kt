package com.example.prescriptaai.fragments

import android.util.TypedValue // <-- NEW IMPORT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prescriptaai.R
import com.example.prescriptaai.fragments.models.Prescription
fun Int.dpToPx(view: View): Int {
    return (this * view.resources.displayMetrics.density).toInt()
}

class PatientPrescriptionAdapter(private val prescriptions: MutableList<Prescription>) :
    RecyclerView.Adapter<PatientPrescriptionAdapter.PrescriptionViewHolder>() {

    var onItemClick: ((Prescription) -> Unit)? = null

    class PrescriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPrescriptionDate: TextView = itemView.findViewById(R.id.tvPrescriptionDate)
        val tvPrescriptionDiagnosis: TextView = itemView.findViewById(R.id.tvPrescriptionDiagnosis)
        val medicationsDetailContainer: LinearLayout = itemView.findViewById(R.id.medicationsDetailContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrescriptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_patient_prescription, parent, false)
        return PrescriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrescriptionViewHolder, position: Int) {
        val prescription = prescriptions[position]

        holder.tvPrescriptionDate.text = "Prescribed On: ${prescription.prescriptionDate ?: "N/A"}"
        holder.tvPrescriptionDiagnosis.text = "Diagnosis: ${prescription.diagnosis ?: "N/A"}"

        holder.medicationsDetailContainer.removeAllViews()

        prescription.medications?.forEach { medication ->
            val medicationTextView = TextView(holder.itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Using the corrected dpToPx extension function
                    setMargins(0, 4.dpToPx(holder.itemView), 0, 0)
                }
                text = buildString {
                    append("• ${medication.name ?: "N/A"}")
                    append("\n  Dosage: ${medication.dosage ?: "N/A"}")
                    append("\n  Frequency: ${medication.frequency ?: "N/A"}")
                    append("\n  Duration: ${medication.duration ?: "N/A"}")
                }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            }
            holder.medicationsDetailContainer.addView(medicationTextView)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(prescription)
        }
    }

    override fun getItemCount(): Int = prescriptions.size

    fun updateData(newPrescriptions: List<Prescription>) {
        prescriptions.clear()
        prescriptions.addAll(newPrescriptions)
        notifyDataSetChanged()
    }
}