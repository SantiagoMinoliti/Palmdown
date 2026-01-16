package com.example.palmdown.repository

import com.example.palmdown.model.Language
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LanguageRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val appId = "palmdown"

    suspend fun getLanguages(): List<Language> {
        return try {
            val snapshot = firestore
                .collection("artifacts")
                .document(appId)
                .collection("languages")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Language::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}