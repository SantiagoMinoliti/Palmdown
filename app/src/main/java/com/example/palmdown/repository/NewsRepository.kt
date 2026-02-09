package com.example.palmdown.repository

import android.util.Log
import com.example.palmdown.model.News
import com.example.palmdown.model.NewsFilter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
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

    /**
     * Genera un hash MD5 dall'URL per usarlo come ID del documento.
     */
    private fun generateIdFromUrl(url: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(url.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            url.hashCode().toString().replace("-", "")
        }
    }
    suspend fun saveNewsIfNotExists(news: News): Boolean {
        return try {
            val collection = getUserNewsCollection() ?: return false
            if (news.url.isBlank()) return false

            val deterministicId = generateIdFromUrl(news.url)
            val docRef = collection.document(deterministicId)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) return false

            val payload = hashMapOf(
                "id" to deterministicId,
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
            Log.e("NewsRepo", "Error saving news", e)
            false
        }
    }

    suspend fun updateNews(news: News) {
        try {
            val collection = getUserNewsCollection() ?: return
            val docId = if (news.id.isNotBlank()) news.id else generateIdFromUrl(news.url)
            val updates = mapOf(
                "isArchived" to news.isArchived,
                "isFavorite" to news.isFavorite
            )
            collection.document(docId).update(updates).await()
        } catch (e: Exception) {
            Log.e("NewsRepo", "Error updating news", e)
        }
    }

    suspend fun getAllNews(filter: NewsFilter): List<News> {
        return try {
            val collection = getUserNewsCollection() ?: return emptyList()

            var query: Query = collection.orderBy("fetchedAt", Query.Direction.DESCENDING)

            if (filter.category.isNotBlank() && filter.category.lowercase() != "all") {
                query = query.whereArrayContains("categories", filter.category.lowercase(Locale.getDefault()))
            }

            val snapshot = query.get().await()
            var newsList = snapshot.documents.map { mapDocumentToNews(it) }

            if (filter.query.isNotBlank()) {
                val q = filter.query.lowercase(Locale.getDefault())
                newsList = newsList.filter {
                    it.title.lowercase(Locale.getDefault()).contains(q) ||
                            it.content.lowercase(Locale.getDefault()).contains(q)
                }
            }
            newsList
        } catch (e: Exception) {
            Log.e("NewsRepo", "Error fetching news", e)
            emptyList()
        }
    }

    // --- METODI PER IL COLLEGAMENTO NEWS NELL'EDITOR ---

    suspend fun linkNewsToNote(noteId: String, news: News) {
        val userRoot = getUserRoot() ?: return
        try {
            // Salva una copia della news nella sotto-collezione della nota
            userRoot.collection("notes")
                .document(noteId)
                .collection("linkedNews")
                .document(news.id)
                .set(news) // Salviamo l'intero oggetto News per visualizzarlo offline nell'editor
                .await()
        } catch (e: Exception) {
            Log.e("NewsRepo", "Error linking news to note", e)
        }
    }

    suspend fun unlinkNewsFromNote(noteId: String, newsId: String) {
        val userRoot = getUserRoot() ?: return
        try {
            userRoot.collection("notes")
                .document(noteId)
                .collection("linkedNews")
                .document(newsId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("NewsRepo", "Error unlinking news", e)
        }
    }

    suspend fun getLinkedNewsForNote(noteId: String): List<News> {
        val userRoot = getUserRoot() ?: return emptyList()
        return try {
            val snapshot = userRoot.collection("notes")
                .document(noteId)
                .collection("linkedNews")
                .get()
                .await()

            snapshot.documents.map { mapDocumentToNews(it) }
        } catch (e: Exception) {
            Log.e("NewsRepo", "Error fetching linked news", e)
            emptyList()
        }
    }

    // --- HELPER MAPPING ---

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
            isArchived = doc.getBoolean("isArchived") ?: false,
            isFavorite = doc.getBoolean("isFavorite") ?: false
        )
    }

    suspend fun fetchAllCategories(): List<String> {
        return try {
            val collection = getUserNewsCollection() ?: return emptyList()
            val snapshot = collection.get().await()
            val allNews = snapshot.documents.map { mapDocumentToNews(it) }
            val categoriesSet = mutableSetOf<String>()
            allNews.forEach { news -> news.categories.forEach { if (it.isNotBlank()) categoriesSet.add(it) } }
            categoriesSet.toList().sorted()
        } catch (e: Exception) { emptyList() }
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
