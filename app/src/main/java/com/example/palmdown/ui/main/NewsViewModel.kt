package com.example.palmdown.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.palmdown.model.News
import com.example.palmdown.model.NewsFilter
import com.example.palmdown.repository.NewsRepository
import com.example.palmdown.worker.NewsBackgroundWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository = NewsRepository()
) : ViewModel() {

    private val _news = MutableStateFlow<List<News>>(emptyList())
    val news: StateFlow<List<News>> = _news

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _filters = MutableStateFlow(NewsFilter())
    val filters: StateFlow<NewsFilter> = _filters

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            try {
                _filters.value = repository.fetchSavedFilters()
            } catch (_: Exception) {}
            loadCategories()
            loadNews()
        }
    }

    fun updateQuery(query: String) {
        updateFilters(_filters.value.copy(query = query))
    }

    fun updateCategory(category: String) {
        updateFilters(_filters.value.copy(category = category))
    }

    private fun updateFilters(newFilters: NewsFilter) {
        _filters.value = newFilters

        viewModelScope.launch {
            try {
                repository.updateFilters(newFilters)
            } catch (_: Exception) {}

            loadNews()
        }
    }

    fun toggleFavorite(newsId: String) {
        viewModelScope.launch {
            var newsToUpdate: News? = null

            // Aggiorna lo stato locale
            _news.update { currentList ->
                currentList.map {
                    if (it.id == newsId) {
                        val updated = it.copy(isFavorite = !it.isFavorite)
                        newsToUpdate = updated
                        updated
                    } else it
                }
            }

            // Salva su Firebase
            newsToUpdate?.let {
                repository.updateNews(it)
            }
        }
    }

    fun archiveNews(newsId: String) {
        viewModelScope.launch {
            var newsToUpdate: News? = null

            // Aggiorna lo stato locale
            _news.update { currentList ->
                currentList.map {
                    if (it.id == newsId) {
                        val updated = it.copy(isArchived = true)
                        newsToUpdate = updated
                        updated
                    } else it
                }
            }

            // Salva su Firebase
            newsToUpdate?.let {
                repository.updateNews(it)
            }
        }
    }

    fun deleteNews(newsId: String) {
        viewModelScope.launch {
            _news.update { currentList ->
                currentList.filter { it.id != newsId }
            }
            // Nota: Se in futuro vorrai cancellare anche da remoto,
            // dovrai aggiungere un metodo deleteNews nel repository
        }
    }

    private suspend fun loadNews() {
        _isLoading.value = true
        _error.value = null

        try {
            _news.value = repository.getAllNews(_filters.value)
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun loadCategories() {
        try {
            _categories.value = repository.fetchAllCategories()
        } catch (_: Exception) {
            _categories.value = emptyList()
        }
    }

    fun refresh(context: Context, forceRefresh: Boolean = false) {
        _isLoading.value = true

        val request = OneTimeWorkRequestBuilder<NewsBackgroundWorker>()
            .setInputData(workDataOf("force_refresh" to forceRefresh))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "manual_news_refresh",
                ExistingWorkPolicy.REPLACE,
                request
            )

        WorkManager.getInstance(context)
            .getWorkInfoByIdLiveData(request.id)
            .observeForever { info ->
                if (info != null && info.state.isFinished) {
                    viewModelScope.launch {
                        loadCategories()
                        loadNews()
                    }
                }
            }
    }
}