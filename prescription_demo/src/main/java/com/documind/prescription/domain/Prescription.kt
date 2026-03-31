package com.documind.prescription.domain

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class Prescription(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("medicineName")
    val medicineName: String,
    
    @SerializedName("dosage")
    val dosage: String,
    
    @SerializedName("frequency")
    val frequency: String,
    
    @SerializedName("duration")
    val duration: String,
    
    @SerializedName("prescribedDate")
    val prescribedDate: String,
    
    @SerializedName("doctorName")
    val doctorName: String,
    
    @SerializedName("instructions")
    val instructions: String,
    
    @SerializedName("warnings")
    val warnings: List<String> = emptyList()
) {
    fun toContextString(): String {
        return buildString {
            appendLine("Medicine: $medicineName")
            appendLine("Dosage: $dosage")
            appendLine("Frequency: $frequency")
            appendLine("Duration: $duration")
            appendLine("Prescribed: $prescribedDate by $doctorName")
            appendLine("Instructions: $instructions")
            if (warnings.isNotEmpty()) {
                appendLine("Warnings: ${warnings.joinToString(", ")}")
            }
        }
    }
}
