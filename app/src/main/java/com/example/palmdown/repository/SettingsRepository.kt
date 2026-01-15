package com.example.palmdown.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.palmdown.model.Settings
import kotlinx.coroutines.tasks.await

class SettingsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val appId = "palmdown"

    private fun getUserSettingsDoc() = auth.currentUser?.uid?.let { userId ->
        firestore.collection("artifacts")
            .document(appId)
            .collection("users")
            .document(userId)
            .collection("settings")
            .document("preferences")
    }

    suspend fun saveSettings(settings: Settings): Boolean {
        return try {
            val docRef = getUserSettingsDoc() ?: return false
            docRef.set(settings).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSettings(): Settings? {
        return try {
            val docRef = getUserSettingsDoc() ?: return null
            val snapshot = docRef.get().await()
            snapshot.toObject(Settings::class.java)
        } catch (e: Exception) {
            null
        }
    }
}