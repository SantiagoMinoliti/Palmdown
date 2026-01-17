package com.example.palmdown.repository

import com.example.palmdown.model.News
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NewsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val appId = "palmdown"

    private fun getUserNewsCollection() = auth.currentUser?.uid?.let { userId ->
        firestore.collection("artifacts")
            .document(appId)
            .collection("users")
            .document(userId)
            .collection("news")
    }

    suspend fun saveNews(news: News): Boolean {
        return try {
            val collection = getUserNewsCollection() ?: return false

            val docRef = if (news.id.isNullOrBlank()) {
                collection.document()
            } else {
                collection.document(news.id)
            }

            val finalNews = news.copy(id = docRef.id)

            docRef.set(finalNews).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllNews(): List<News> {
        return try {
            val collection = getUserNewsCollection() ?: return emptyList()
            val snapshot = collection.get().await()
            snapshot.toObjects(News::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveNewsIfNotExists(news: News): Boolean {
        return try {
            val collection = getUserNewsCollection() ?: return false

            val existing = collection
                .whereEqualTo("url", news.url)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                return false
            }

            val docRef = collection.document()
            val finalNews = news.copy(id = docRef.id)

            docRef.set(finalNews).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
