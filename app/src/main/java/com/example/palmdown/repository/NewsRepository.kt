package com.example.palmdown.repository

import com.example.palmdown.model.News
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    suspend fun saveNewsIfNotExists(news: News): Boolean {
        return try {
            val collection = getUserNewsCollection() ?: return false

            val existing = collection
                .whereEqualTo("url", news.url)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) return false

            val docRef = collection.document()

            val payload = hashMapOf(
                "id" to docRef.id,
                "title" to news.title,
                "content" to news.content,
                "url" to news.url,
                "date" to news.date,
                "country" to news.country,
                "fetchedAt" to news.fetchedAt,
                "keywords" to news.keywords,
                "creator" to news.creator,
                "categories" to news.categories,
                "imageUrl" to news.imageUrl,
                "videoUrl" to news.videoUrl,
                "sourceId" to news.sourceId,
                "sourceName" to news.sourceName,
                "sourceIcon" to news.sourceIcon
            )

            docRef.set(payload).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllNews(): List<News> {
        return try {
            val collection = getUserNewsCollection() ?: return emptyList()
            val snapshot = collection
                .orderBy("fetchedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(News::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}