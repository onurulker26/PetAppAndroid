package com.tamer.petapp.utils

import android.content.Context
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

object AnalyticsHelper {
    private const val TAG = "AnalyticsHelper"
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var crashlytics: FirebaseCrashlytics
    
    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        crashlytics = FirebaseCrashlytics.getInstance()
    }
    
    // User Events
    fun logUserLogin(method: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }
    
    fun logUserRegistration(method: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }
    
    // Pet Events
    fun logPetAdded(petType: String) {
        firebaseAnalytics.logEvent("pet_added") {
            param("pet_type", petType)
            param("timestamp", System.currentTimeMillis())
        }
    }
    
    fun logVaccinationAdded(petType: String, vaccineName: String) {
        firebaseAnalytics.logEvent("vaccination_added") {
            param("pet_type", petType)
            param("vaccine_name", vaccineName)
            param("timestamp", System.currentTimeMillis())
        }
    }
    
    fun logTreatmentAdded(petType: String, treatmentType: String) {
        firebaseAnalytics.logEvent("treatment_added") {
            param("pet_type", petType)
            param("treatment_type", treatmentType)
            param("timestamp", System.currentTimeMillis())
        }
    }
    
    // Feature Usage
    fun logFeatureUsed(featureName: String, params: Map<String, Any> = emptyMap()) {
        firebaseAnalytics.logEvent("feature_used") {
            param("feature_name", featureName)
            params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Number -> param(key, value.toDouble())
                    is Boolean -> param(key, if (value) 1L else 0L)
                }
            }
        }
    }
    
    // Error Tracking
    fun logError(error: Throwable, context: String = "", additionalData: Map<String, String> = emptyMap()) {
        try {
            // Crashlytics'e gönder
            crashlytics.setCustomKey("context", context)
            additionalData.forEach { (key, value) ->
                crashlytics.setCustomKey(key, value)
            }
            crashlytics.recordException(error)
            
            // Analytics'e de gönder
            firebaseAnalytics.logEvent("app_error") {
                param("error_type", error.javaClass.simpleName)
                param("error_message", error.message ?: "Unknown error")
                param("context", context)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Analytics hatası: ${e.message}")
        }
    }
    
    // Performance Tracking
    fun logPerformanceEvent(eventName: String, duration: Long, success: Boolean = true) {
        firebaseAnalytics.logEvent("performance_$eventName") {
            param("duration_ms", duration)
            param("success", if (success) 1L else 0L)
            param("timestamp", System.currentTimeMillis())
        }
    }
    
    // User Properties
    fun setUserProperty(property: String, value: String) {
        firebaseAnalytics.setUserProperty(property, value)
    }
    
    // Database Usage Stats
    fun logDatabaseUsage(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val stats = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "user_id" to userId,
            "session_id" to UUID.randomUUID().toString()
        )
        
        firestore.collection("app_usage_stats")
            .add(stats)
            .addOnFailureListener { e ->
                Log.e(TAG, "Database stats gönderim hatası: ${e.message}")
            }
    }
}

// Extension function for easier parameter setting
private inline fun FirebaseAnalytics.logEvent(name: String, block: ParameterBuilder.() -> Unit) {
    val builder = ParameterBuilder()
    builder.block()
    logEvent(name, builder.bundle)
}

class ParameterBuilder {
    val bundle = android.os.Bundle()
    
    fun param(key: String, value: String) {
        bundle.putString(key, value)
    }
    
    fun param(key: String, value: Long) {
        bundle.putLong(key, value)
    }
    
    fun param(key: String, value: Double) {
        bundle.putDouble(key, value)
    }
} 