package com.documind.prescription.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class QuickAction(
    val title: String,
    val prompt: String,
    val icon: ImageVector
) {
    SUMMARY(
        title = "Summary",
        prompt = "Please provide a brief summary of all my prescriptions, including the medicine names, dosages, and key instructions.",
        icon = Icons.Default.Summarize
    ),
    
    TODAY_MEDICATIONS(
        title = "Today's Meds",
        prompt = "What medications should I take today? Please list them with their dosage and timing.",
        icon = Icons.Default.CalendarToday
    ),
    
    DRUG_INTERACTIONS(
        title = "Interactions",
        prompt = "Are there any potential drug interactions or conflicts between my current medications that I should be aware of?",
        icon = Icons.Default.Warning
    ),
    
    DOSAGE_SCHEDULE(
        title = "Schedule",
        prompt = "Create a daily medication schedule for me, organizing when I should take each medicine throughout the day.",
        icon = Icons.Default.Schedule
    ),
    
    SIDE_EFFECTS(
        title = "Side Effects",
        prompt = "What are the common side effects of my current medications that I should watch out for?",
        icon = Icons.Default.LocalPharmacy
    )
}
