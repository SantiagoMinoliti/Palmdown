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
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository = NewsRepository()
) : ViewModel() {

    /* --------------------
     *  STATE
     * -------------------- */

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

    /* --------------------
     *  INIT
     * -------------------- */

    init {
        viewModelScope.launch {
            // 1. fetch persisted filters (non-blocking fallback already set)
            try {
                _filters.value = repository.fetchSavedFilters()
            } catch (_: Exception) {}

            // 2. load categories (derived only from existing news)
            loadCategories()

            // 3. load news with active filters
            loadNews()
        }
    }

    /* --------------------
     *  FILTER UPDATES (UI -> VM)
     * -------------------- */

    fun updateQuery(query: String) {
        updateFilters(_filters.value.copy(query = query))
    }

    fun updateCategory(category: String) {
        updateFilters(_filters.value.copy(category = category))
    }

    private fun updateFilters(newFilters: NewsFilter) {
        _filters.value = newFilters

        viewModelScope.launch {
            // persist filters, but never block UI
            try {
                repository.updateFilters(newFilters)
            } catch (_: Exception) {}

            loadNews()
        }
    }

    fun getFilters(): NewsFilter = _filters.value

    /* --------------------
     *  LOADERS
     * -------------------- */

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

    /* --------------------
     *  REFRESH
     * -------------------- */

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
