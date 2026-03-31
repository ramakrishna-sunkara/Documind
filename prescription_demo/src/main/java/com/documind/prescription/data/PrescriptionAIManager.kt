package com.documind.prescription.data

import android.util.Log
import com.documind.prescription.domain.ResponseSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class PrescriptionAIManager(
    private val llmWrapper: LocalLLMWrapper,
    private val prescriptionRepository: PrescriptionRepository
) {
    
    companion object {
        private const val TAG = "PrescriptionAIManager"
        
        private const val SYSTEM_PROMPT = """You are a helpful medical AI assistant helping patients understand their prescriptions.

IMPORTANT RULES:
- Answer ONLY based on the prescription information provided
- Be concise and easy to understand
- If asked about drug interactions, provide general safety information
- Always remind patients to consult their doctor for medical advice
- Never diagnose conditions or recommend changing medications
- If information is not in the prescriptions, say so clearly"""
    }
    
    data class AIResponse(
        val content: String,
        val source: ResponseSource
    )
    
    suspend fun generateResponse(userQuery: String): AIResponse = withContext(Dispatchers.Default) {
        val prescriptionContext = prescriptionRepository.buildPrescriptionContext()
        
        if (llmWrapper.isReady()) {
            Log.d(TAG, "Attempting offline AI response")
            val offlineResult = tryOfflineResponse(prescriptionContext, userQuery)
            
            if (offlineResult.isSuccess) {
                val response = offlineResult.getOrNull()
                if (!response.isNullOrBlank() && response.length > 10) {
                    // Check if LLM indicates it doesn't know the answer
                    if (isLlmUncertainResponse(response)) {
                        Log.d(TAG, "Offline AI uncertain, falling back to online")
                        val mockResponse = getMockOnlineResponse(prescriptionContext, userQuery)
                        return@withContext AIResponse(mockResponse, ResponseSource.ONLINE)
                    }
                    
                    Log.d(TAG, "Offline AI response successful")
                    return@withContext AIResponse(response, ResponseSource.OFFLINE)
                }
            }
            Log.d(TAG, "Offline AI failed or returned empty, falling back to online")
        } else {
            Log.d(TAG, "Offline AI not ready, using online")
        }
        
        val mockResponse = getMockOnlineResponse(prescriptionContext, userQuery)
        return@withContext AIResponse(mockResponse, ResponseSource.ONLINE)
    }
    
    private fun isLlmUncertainResponse(response: String): Boolean {
        val uncertainPhrases = listOf(
            "i don't know",
            "i do not know",
            "i'm not sure",
            "i am not sure",
            "cannot answer",
            "can't answer",
            "unable to answer",
            "not found in document",
            "not found in the document",
            "no information",
            "don't have information",
            "do not have information",
            "cannot provide",
            "can't provide",
            "unable to provide",
            "not available",
            "beyond my knowledge",
            "outside my scope",
            "consult a doctor",
            "consult your doctor",
            "seek medical advice",
            "i cannot help",
            "i can't help",
            "not able to help",
            "insufficient information",
            "not enough information",
            "unclear",
            "unknown"
        )
        
        val responseLower = response.lowercase()
        return uncertainPhrases.any { phrase -> responseLower.contains(phrase) }
    }
    
    private suspend fun tryOfflineResponse(context: String, query: String): Result<String> {
        return try {
            llmWrapper.generateResponse(context, query)
        } catch (e: Exception) {
            Log.e(TAG, "Offline generation failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun getMockOnlineResponse(context: String, query: String): String {
        delay(1500)
        
        val queryLower = query.lowercase()
        
        return when {
            // Summary
            queryLower.contains("summary") || queryLower.contains("summarize") || 
            queryLower.contains("all my") || queryLower.contains("list") -> {
                generateSummaryResponse(context)
            }
            // Schedule
            queryLower.contains("today") || queryLower.contains("schedule") || 
            queryLower.contains("when") || queryLower.contains("time") -> {
                generateScheduleResponse(context)
            }
            // Alcohol
            queryLower.contains("alcohol") || queryLower.contains("drink") || 
            queryLower.contains("beer") || queryLower.contains("wine") -> {
                generateAlcoholWarningResponse(context)
            }
            // Drug interactions
            queryLower.contains("interaction") || queryLower.contains("conflict") || 
            queryLower.contains("together") || queryLower.contains("combine") -> {
                generateInteractionResponse(context)
            }
            // Side effects
            queryLower.contains("side effect") || queryLower.contains("reaction") || 
            queryLower.contains("symptoms") -> {
                generateSideEffectsResponse(context)
            }
            // Missed dose
            queryLower.contains("miss") || queryLower.contains("forgot") || 
            queryLower.contains("skip") || queryLower.contains("didn't take") -> {
                generateMissedDoseResponse(context)
            }
            // Food instructions
            queryLower.contains("food") || queryLower.contains("meal") || 
            queryLower.contains("eat") || queryLower.contains("empty stomach") -> {
                generateFoodInstructionsResponse(context)
            }
            // Stop taking / discontinue
            queryLower.contains("stop") || queryLower.contains("discontinue") || 
            queryLower.contains("quit") || queryLower.contains("finish early") -> {
                generateStopMedicationResponse()
            }
            // Overdose / too much
            queryLower.contains("overdose") || queryLower.contains("too much") || 
            queryLower.contains("double dose") || queryLower.contains("took extra") -> {
                generateOverdoseResponse()
            }
            // Pregnancy / breastfeeding
            queryLower.contains("pregnant") || queryLower.contains("pregnancy") || 
            queryLower.contains("breastfeed") || queryLower.contains("nursing") -> {
                generatePregnancyResponse()
            }
            // Driving / drowsiness
            queryLower.contains("drive") || queryLower.contains("driving") || 
            queryLower.contains("drowsy") || queryLower.contains("sleepy") -> {
                generateDrivingResponse()
            }
            // Storage
            queryLower.contains("store") || queryLower.contains("storage") || 
            queryLower.contains("refrigerat") || queryLower.contains("keep") -> {
                generateStorageResponse()
            }
            // Pain relief / OTC
            queryLower.contains("painkiller") || queryLower.contains("ibuprofen") || 
            queryLower.contains("aspirin") || queryLower.contains("tylenol") || 
            queryLower.contains("over the counter") || queryLower.contains("otc") -> {
                generateOTCResponse()
            }
            // Cost / generic
            queryLower.contains("generic") || queryLower.contains("cheaper") || 
            queryLower.contains("cost") || queryLower.contains("expensive") -> {
                generateGenericResponse()
            }
            // Refill
            queryLower.contains("refill") || queryLower.contains("run out") || 
            queryLower.contains("pharmacy") || queryLower.contains("prescription expire") -> {
                generateRefillResponse()
            }
            // How does it work
            queryLower.contains("how does") || queryLower.contains("how do") || 
            queryLower.contains("what does") || queryLower.contains("mechanism") -> {
                generateHowItWorksResponse(context, query)
            }
            // Feeling better / still sick
            queryLower.contains("feeling better") || queryLower.contains("still sick") || 
            queryLower.contains("not working") || queryLower.contains("doesn't help") -> {
                generateEffectivenessResponse()
            }
            else -> {
                generateGeneralResponse(context, query)
            }
        }
    }
    
    private fun generateStopMedicationResponse(): String {
        return buildString {
            appendLine("**Important: Do Not Stop Without Consulting Your Doctor**")
            appendLine()
            appendLine("Stopping medication abruptly can be dangerous:")
            appendLine()
            appendLine("• Some medications require gradual tapering")
            appendLine("• Stopping antibiotics early can cause resistance")
            appendLine("• Blood pressure/diabetes medications need careful management")
            appendLine()
            appendLine("**What to do:**")
            appendLine("1. Call your doctor before making any changes")
            appendLine("2. Explain why you want to stop (side effects, cost, etc.)")
            appendLine("3. Follow their guidance for safe discontinuation")
            appendLine()
            appendLine("Never stop prescribed medications on your own.")
        }
    }
    
    private fun generateOverdoseResponse(): String {
        return buildString {
            appendLine("**If you took too much medication:**")
            appendLine()
            appendLine("**Immediate steps:**")
            appendLine("1. Don't panic - note what you took and when")
            appendLine("2. Call Poison Control: **1-800-222-1222** (US)")
            appendLine("3. Or call your doctor immediately")
            appendLine("4. If severe symptoms, call 911")
            appendLine()
            appendLine("**Warning signs to watch for:**")
            appendLine("• Difficulty breathing")
            appendLine("• Severe dizziness or confusion")
            appendLine("• Rapid heartbeat")
            appendLine("• Vomiting")
            appendLine()
            appendLine("**Prevention:** Use a pill organizer and set reminders to avoid accidental double doses.")
        }
    }
    
    private fun generatePregnancyResponse(): String {
        return buildString {
            appendLine("**Pregnancy & Breastfeeding - Consult Your Doctor**")
            appendLine()
            appendLine("Many medications require special consideration during pregnancy and breastfeeding.")
            appendLine()
            appendLine("**Important:**")
            appendLine("• Do NOT stop medications without medical advice")
            appendLine("• Some conditions require continued treatment")
            appendLine("• Your doctor can suggest safer alternatives if needed")
            appendLine()
            appendLine("**Action required:**")
            appendLine("Contact your healthcare provider immediately to discuss your medications. They will weigh the benefits vs risks for your specific situation.")
        }
    }
    
    private fun generateDrivingResponse(): String {
        return buildString {
            appendLine("**Driving & Medication Safety**")
            appendLine()
            val prescriptions = prescriptionRepository.getPrescriptions()
            val drowsyMeds = prescriptions.filter { rx ->
                rx.warnings.any { it.lowercase().contains("drowsy") || it.lowercase().contains("dizz") }
            }
            
            if (drowsyMeds.isNotEmpty()) {
                appendLine("**Warning:** These medications may affect your ability to drive:")
                appendLine()
                drowsyMeds.forEach { rx ->
                    appendLine("• **${rx.medicineName}** - May cause drowsiness/dizziness")
                }
                appendLine()
            }
            
            appendLine("**General guidelines:**")
            appendLine("• Wait to see how medication affects you before driving")
            appendLine("• Avoid driving if you feel drowsy or dizzy")
            appendLine("• Don't combine with alcohol")
            appendLine("• Be extra cautious when starting new medication")
            appendLine()
            appendLine("If you're unsure, don't drive. Ask your doctor or pharmacist.")
        }
    }
    
    private fun generateStorageResponse(): String {
        return buildString {
            appendLine("**Medication Storage Guidelines**")
            appendLine()
            appendLine("**General rules:**")
            appendLine("• Store at room temperature (68-77°F / 20-25°C)")
            appendLine("• Keep away from moisture (not in bathroom)")
            appendLine("• Protect from direct sunlight")
            appendLine("• Keep out of reach of children")
            appendLine()
            appendLine("**Special storage:**")
            appendLine("• Some medications need refrigeration - check the label")
            appendLine("• Insulin and certain liquids often need refrigeration")
            appendLine()
            appendLine("**Check expiration dates:**")
            appendLine("• Don't use expired medications")
            appendLine("• Dispose of expired meds at pharmacy take-back programs")
        }
    }
    
    private fun generateOTCResponse(): String {
        return buildString {
            appendLine("**Over-the-Counter Medications - Caution Advised**")
            appendLine()
            appendLine("Before taking any OTC pain relievers or medications with your prescriptions:")
            appendLine()
            appendLine("**Potential concerns:**")
            appendLine("• **Ibuprofen/Aspirin** - Can interact with blood pressure meds, blood thinners")
            appendLine("• **Acetaminophen (Tylenol)** - Generally safer, but watch total daily dose")
            appendLine("• **Antacids** - Can affect absorption of other medications")
            appendLine()
            appendLine("**Recommendation:**")
            appendLine("Always tell your pharmacist about ALL your prescriptions before buying OTC medications. They can check for interactions.")
            appendLine()
            appendLine("When in doubt, call your doctor or pharmacist first.")
        }
    }
    
    private fun generateGenericResponse(): String {
        return buildString {
            appendLine("**Generic vs Brand Name Medications**")
            appendLine()
            appendLine("**Good news:** Generic medications are equally effective!")
            appendLine()
            appendLine("• FDA requires generics to have same active ingredient")
            appendLine("• Same strength, dosage, and safety")
            appendLine("• Can cost 80-85% less than brand names")
            appendLine()
            appendLine("**To get generics:**")
            appendLine("1. Ask your doctor to prescribe generic when available")
            appendLine("2. Ask your pharmacist about generic options")
            appendLine("3. Check if your insurance prefers generics")
            appendLine()
            appendLine("Most of your prescriptions likely have generic alternatives available.")
        }
    }
    
    private fun generateRefillResponse(): String {
        return buildString {
            appendLine("**Prescription Refills**")
            appendLine()
            appendLine("**Plan ahead:**")
            appendLine("• Request refills 5-7 days before running out")
            appendLine("• Most pharmacies offer auto-refill programs")
            appendLine("• Set reminders to check your supply weekly")
            appendLine()
            appendLine("**Refill options:**")
            appendLine("• Call your pharmacy directly")
            appendLine("• Use pharmacy's mobile app")
            appendLine("• Many pharmacies allow online refills")
            appendLine()
            appendLine("**If prescription expired:**")
            appendLine("• Contact your doctor for a new prescription")
            appendLine("• Some medications may require a new appointment")
            appendLine()
            appendLine("Don't wait until you run out completely!")
        }
    }
    
    private fun generateHowItWorksResponse(context: String, query: String): String {
        val prescriptions = prescriptionRepository.getPrescriptions()
        
        return buildString {
            appendLine("**How Your Medications Work**")
            appendLine()
            prescriptions.forEach { rx ->
                appendLine("**${rx.medicineName}:**")
                when {
                    rx.medicineName.contains("Metformin", ignoreCase = true) -> 
                        appendLine("Helps control blood sugar by reducing glucose production in liver and improving insulin sensitivity.")
                    rx.medicineName.contains("Lisinopril", ignoreCase = true) -> 
                        appendLine("Relaxes blood vessels to lower blood pressure and reduce strain on heart.")
                    rx.medicineName.contains("Atorvastatin", ignoreCase = true) -> 
                        appendLine("Blocks cholesterol production in liver to lower LDL ('bad') cholesterol levels.")
                    rx.medicineName.contains("Omeprazole", ignoreCase = true) -> 
                        appendLine("Reduces stomach acid production to treat heartburn and protect stomach lining.")
                    rx.medicineName.contains("Amoxicillin", ignoreCase = true) -> 
                        appendLine("Antibiotic that kills bacteria by preventing them from building cell walls.")
                    rx.medicineName.contains("Vitamin D", ignoreCase = true) -> 
                        appendLine("Essential vitamin that helps body absorb calcium for bone health and immune function.")
                    else -> 
                        appendLine("Works as prescribed to manage your condition. Ask your pharmacist for detailed mechanism.")
                }
                appendLine()
            }
            appendLine("For more detailed information, consult your pharmacist or doctor.")
        }
    }
    
    private fun generateEffectivenessResponse(): String {
        return buildString {
            appendLine("**If Your Medication Doesn't Seem to Be Working**")
            appendLine()
            appendLine("**First, consider:**")
            appendLine("• Some medications take days or weeks to show full effect")
            appendLine("• Are you taking it correctly? (timing, with/without food)")
            appendLine("• Have you missed any doses?")
            appendLine()
            appendLine("**Contact your doctor if:**")
            appendLine("• No improvement after expected timeframe")
            appendLine("• Symptoms are getting worse")
            appendLine("• You're experiencing concerning side effects")
            appendLine()
            appendLine("**Don't:**")
            appendLine("• Increase the dose on your own")
            appendLine("• Stop taking it without consulting your doctor")
            appendLine("• Combine with other medications without approval")
            appendLine()
            appendLine("Your doctor may need to adjust dosage or try a different medication.")
        }
    }
    
    private fun generateSummaryResponse(context: String): String {
        val prescriptions = prescriptionRepository.getPrescriptions()
        if (prescriptions.isEmpty()) {
            return "No prescriptions are currently loaded."
        }
        
        return buildString {
            appendLine("Here's a summary of your prescriptions:")
            appendLine()
            prescriptions.forEach { rx ->
                appendLine("• **${rx.medicineName}** (${rx.dosage})")
                appendLine("  Take ${rx.frequency.lowercase()} for ${rx.duration}")
                if (rx.instructions.isNotBlank()) {
                    appendLine("  ${rx.instructions}")
                }
                appendLine()
            }
            appendLine("Please follow your doctor's instructions and take all medications as prescribed.")
        }
    }
    
    private fun generateScheduleResponse(context: String): String {
        val prescriptions = prescriptionRepository.getPrescriptions()
        if (prescriptions.isEmpty()) {
            return "No prescriptions are currently loaded."
        }
        
        return buildString {
            appendLine("Here's your daily medication schedule:")
            appendLine()
            appendLine("**Morning:**")
            prescriptions.filter { 
                it.frequency.lowercase().contains("morning") || 
                it.frequency.lowercase().contains("twice") ||
                it.frequency.lowercase().contains("daily")
            }.forEach {
                appendLine("• ${it.medicineName} ${it.dosage}")
            }
            appendLine()
            appendLine("**Evening:**")
            prescriptions.filter { 
                it.frequency.lowercase().contains("evening") || 
                it.frequency.lowercase().contains("twice") ||
                it.frequency.lowercase().contains("night")
            }.forEach {
                appendLine("• ${it.medicineName} ${it.dosage}")
            }
            appendLine()
            appendLine("Remember to take medications at consistent times each day.")
        }
    }
    
    private fun generateAlcoholWarningResponse(context: String): String {
        val prescriptions = prescriptionRepository.getPrescriptions()
        val alcoholWarnings = prescriptions.filter { rx ->
            rx.warnings.any { it.lowercase().contains("alcohol") }
        }
        
        return buildString {
            if (alcoholWarnings.isNotEmpty()) {
                appendLine("⚠️ **Important Alcohol Warning**")
                appendLine()
                appendLine("The following medications should NOT be taken with alcohol:")
                appendLine()
                alcoholWarnings.forEach { rx ->
                    appendLine("• **${rx.medicineName}**: ${rx.warnings.find { it.lowercase().contains("alcohol") }}")
                }
                appendLine()
                appendLine("Mixing alcohol with these medications can cause serious side effects including dizziness, drowsiness, and liver damage.")
            } else {
                appendLine("While none of your prescriptions have specific alcohol warnings, it's generally advisable to limit alcohol consumption when taking any medication.")
            }
            appendLine()
            appendLine("Please consult your doctor or pharmacist for personalized advice.")
        }
    }
    
    private fun generateInteractionResponse(context: String): String {
        val prescriptions = prescriptionRepository.getPrescriptions()
        
        return buildString {
            appendLine("**Drug Interaction Information**")
            appendLine()
            if (prescriptions.size > 1) {
                appendLine("You are currently taking ${prescriptions.size} medications. Here are some general safety tips:")
                appendLine()
                appendLine("• Take medications at different times if possible")
                appendLine("• Report any unusual symptoms to your doctor")
                appendLine("• Don't start new medications without consulting your doctor")
                appendLine()
            }
            appendLine("For specific drug interaction information, please consult your pharmacist or doctor, as they can review your complete medication list.")
        }
    }
    
    private fun generateSideEffectsResponse(context: String): String {
        val prescriptions = prescriptionRepository.getPrescriptions()
        
        return buildString {
            appendLine("**Common Side Effects to Watch For:**")
            appendLine()
            prescriptions.forEach { rx ->
                appendLine("**${rx.medicineName}:**")
                rx.warnings.forEach { warning ->
                    if (!warning.lowercase().contains("alcohol")) {
                        appendLine("• $warning")
                    }
                }
                appendLine()
            }
            appendLine("If you experience severe side effects, contact your doctor immediately.")
        }
    }
    
    private fun generateMissedDoseResponse(context: String): String {
        return buildString {
            appendLine("**What to Do If You Miss a Dose:**")
            appendLine()
            appendLine("• If you remember within a few hours, take it as soon as possible")
            appendLine("• If it's almost time for your next dose, skip the missed dose")
            appendLine("• Never take a double dose to make up for a missed one")
            appendLine("• Set reminders to help you remember")
            appendLine()
            appendLine("For specific guidance about your medications, please consult your doctor or pharmacist.")
        }
    }
    
    private fun generateFoodInstructionsResponse(context: String): String {
        val prescriptions = prescriptionRepository.getPrescriptions()
        
        return buildString {
            appendLine("**Food & Medication Instructions:**")
            appendLine()
            prescriptions.forEach { rx ->
                appendLine("• **${rx.medicineName}**: ${rx.instructions}")
            }
            appendLine()
            appendLine("Following these instructions helps your body absorb the medication properly.")
        }
    }
    
    private fun generateGeneralResponse(context: String, query: String): String {
        return buildString {
            appendLine("Based on your prescription information, I'll do my best to help.")
            appendLine()
            appendLine("Your current medications include:")
            prescriptionRepository.getPrescriptions().forEach { rx ->
                appendLine("• ${rx.medicineName} (${rx.dosage})")
            }
            appendLine()
            appendLine("For specific medical questions about \"$query\", please consult your healthcare provider who can give you personalized advice based on your complete medical history.")
        }
    }
    
    fun isOfflineReady(): Boolean = llmWrapper.isReady()
}
