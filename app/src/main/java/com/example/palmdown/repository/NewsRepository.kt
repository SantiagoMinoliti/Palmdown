package com.example.palmdown.repository

import com.example.palmdown.model.News
import com.google.firebase.firestore.FirebaseFirestore

object NewsRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun getNews(
        onSuccess: (List<News>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore
            .collection("news")
            .get()
            .addOnSuccessListener { snapshot ->
                val newsList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(News::class.java)?.copy(
                        id = doc.id
                    )
                }
                onSuccess(newsList)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun postNews(
        news: News,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore
            .collection("news")
            .add(news)
            .addOnSuccessListener { _ ->
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}