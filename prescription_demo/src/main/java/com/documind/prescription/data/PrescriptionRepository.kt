package com.documind.prescription.data

import android.content.Context
import android.util.Log
import com.documind.prescription.domain.Prescription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrescriptionRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "PrescriptionRepository"
        private const val PRESCRIPTIONS_FILE = "prescriptions.json"
    }
    
    private val gson = Gson()
    private var cachedPrescriptions: List<Prescription>? = null
    
    suspend fun loadPrescriptions(): Result<List<Prescription>> = withContext(Dispatchers.IO) {
        try {
            cachedPrescriptions?.let { 
                return@withContext Result.success(it) 
            }
            
            val jsonString = context.assets
                .open(PRESCRIPTIONS_FILE)
                .bufferedReader()
                .use { it.readText() }
            
            val listType = object : TypeToken<List<Prescription>>() {}.type
            val prescriptions: List<Prescription> = gson.fromJson(jsonString, listType)
            
            cachedPrescriptions = prescriptions
            Log.d(TAG, "Loaded ${prescriptions.size} prescriptions from assets")
            
            Result.success(prescriptions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load prescriptions: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun loadPrescriptionsFromJson(json: String): Result<List<Prescription>> = 
        withContext(Dispatchers.IO) {
            try {
                val listType = object : TypeToken<List<Prescription>>() {}.type
                val prescriptions: List<Prescription> = gson.fromJson(json, listType)
                cachedPrescriptions = prescriptions
                Log.d(TAG, "Loaded ${prescriptions.size} prescriptions from JSON string")
                Result.success(prescriptions)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse prescriptions JSON: ${e.message}", e)
                Result.failure(e)
            }
        }
    
    fun getPrescriptions(): List<Prescription> = cachedPrescriptions ?: emptyList()
    
    fun getPrescriptionById(id: String): Prescription? = 
        cachedPrescriptions?.find { it.id == id }
    
    fun getTodayPrescriptions(): List<Prescription> {
        return cachedPrescriptions ?: emptyList()
    }
    
    fun buildPrescriptionContext(): String {
        val prescriptions = cachedPrescriptions ?: return "No prescriptions loaded."
        
        if (prescriptions.isEmpty()) {
            return "No prescriptions available."
        }
        
        return buildString {
            appendLine("=== PATIENT PRESCRIPTIONS ===")
            appendLine()
            prescriptions.forEachIndexed { index, prescription ->
                appendLine("--- Prescription ${index + 1} ---")
                append(prescription.toContextString())
                appendLine()
            }
            appendLine("=== END OF PRESCRIPTIONS ===")
        }
    }
    
    fun clearCache() {
        cachedPrescriptions = null
    }
}
