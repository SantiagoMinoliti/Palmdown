package com.example.palmdown.repository

import android.util.Log
import com.example.palmdown.model.News
import com.example.palmdown.model.NewsFilter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Locale

class NewsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val appId = "palmdown"

    private fun getUserRoot() = auth.currentUser?.uid?.let { userId ->
        firestore.collection("artifacts")
            .document(appId)
            .collection("users")
            .document(userId)
    }

    private fun getUserNewsCollection() = getUserRoot()?.collection("news")

    private fun getUserFiltersDocument() = getUserRoot()
        ?.collection("filters")
        ?.document("filterset")

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
                "sourceIcon" to news.sourceIcon,
                "isArchived" to news.isArchived,
                "isFavorite" to news.isFavorite
            )

            docRef.set(payload).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateNews(news: News) {
        try {
            val collection = getUserNewsCollection() ?: return
            if (news.id.isBlank()) return

            val updates = mapOf(
                "isArchived" to news.isArchived,
                "isFavorite" to news.isFavorite
            )

            collection.document(news.id).update(updates).await()
        } catch (e: Exception) {
            Log.e("NewsRepo", "Error updating news", e)
        }
    }

    suspend fun getAllNews(filter: NewsFilter): List<News> {
        return try {
            val collection = getUserNewsCollection() ?: return emptyList()

            var query: Query = collection.orderBy("fetchedAt", Query.Direction.DESCENDING)

            if (filter.category.isNotBlank() && filter.category.lowercase() != "all") {
                query = query.whereArrayContains(
                    "categories",
                    filter.category.lowercase(Locale.getDefault())
                )
            }
            Log.d(
                "NewsRepo",
                "Filtering by category='${filter.category}'"
            )

            val snapshot = query.get().await()

            // Mappatura manuale per garantire la corretta lettura dei campi booleani isFavorite/isArchived
            var news = snapshot.documents.map { doc ->
                mapDocumentToNews(doc)
            }

            if (filter.query.isNotBlank()) {
                val q = filter.query.lowercase(Locale.getDefault())
                news = news.filter {
                    it.title.lowercase(Locale.getDefault()).contains(q) ||
                            it.content.lowercase(Locale.getDefault()).contains(q)
                }
            }

            news
        } catch (e: Exception) {
            Log.e("NewsRepo", "Error fetching news", e)
            emptyList()
        }
    }

    // Helper per mappare manualmente il documento Firestore all'oggetto News
    private fun mapDocumentToNews(doc: DocumentSnapshot): News {
        val keywords = (doc.get("keywords") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val creator = (doc.get("creator") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val categories = (doc.get("categories") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        return News(
            id = doc.id,
            title = doc.getString("title") ?: "",
            content = doc.getString("content") ?: "",
            url = doc.getString("url") ?: "",
            date = doc.getDate("date"),
            country = doc.getString("country") ?: "",
            fetchedAt = doc.getDate("fetchedAt"),
            keywords = keywords,
            creator = creator,
            categories = categories,
            imageUrl = doc.getString("imageUrl") ?: "",
            videoUrl = doc.getString("videoUrl") ?: "",
            sourceId = doc.getString("sourceId") ?: "",
            sourceName = doc.getString("sourceName") ?: "",
            sourceIcon = doc.getString("sourceIcon") ?: "",
            // Legge esplicitamente i campi booleani con i nomi corretti
            isArchived = doc.getBoolean("isArchived") ?: false,
            isFavorite = doc.getBoolean("isFavorite") ?: false
        )
    }

    suspend fun fetchAllCategories(): List<String> {
        return try {
            val collection = getUserNewsCollection() ?: return emptyList()
            val snapshot = collection.get().await()
            // Usiamo anche qui la mappatura manuale per coerenza, o toObjects se ci fidiamo solo delle categorie
            // Per le categorie toObjects va bene perché usa fields standard, ma map è più sicuro
            val allNews = snapshot.documents.map { mapDocumentToNews(it) }

            val categoriesSet = mutableSetOf<String>()
            allNews.forEach { news ->
                news.categories.forEach { cat ->
                    if (cat.isNotBlank()) categoriesSet.add(cat)
                }
            }

            categoriesSet.toList().sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateFilters(filters: NewsFilter) {
        val doc = getUserFiltersDocument() ?: return

        val payload = mapOf(
            "query" to filters.query,
            "category" to filters.category
        )

        doc.set(payload).await()
    }

    suspend fun fetchSavedFilters(): NewsFilter {
        return try {
            val doc = getUserFiltersDocument() ?: return NewsFilter()
            val snapshot = doc.get().await()

            if (!snapshot.exists()) return NewsFilter()

            NewsFilter(
                query = snapshot.getString("query") ?: "",
                category = snapshot.getString("category") ?: "all"
            )
        } catch (e: Exception) {
            NewsFilter()
        }
    }
}