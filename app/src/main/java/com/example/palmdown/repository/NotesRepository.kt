package com.example.palmdown.repository

import com.example.palmdown.model.Notes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotesRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appId = "palmdown"

    private fun getUserNotesCollection() = auth.currentUser?.uid?.let { userId ->
        firestore.collection("artifacts")
            .document(appId)
            .collection("users")
            .document(userId)
            .collection("notes")
    }

    suspend fun saveNote(note: Notes): Boolean {
        return try {
            val collection = getUserNotesCollection() ?: return false
            val docRef = if (note.id.isEmpty()) collection.document() else collection.document(note.id)
            val finalNote = note.copy(id = docRef.id)
            docRef.set(finalNote).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllNotes(): List<Notes> {
        return try {
            val collection = getUserNotesCollection() ?: return emptyList()
            val snapshot = collection.orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING).get().await()
            snapshot.toObjects(Notes::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Metodo per eliminare la nota tramite ID
    suspend fun deleteNote(noteId: String): Boolean {
        return try {
            getUserNotesCollection()?.document(noteId)?.delete()?.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}