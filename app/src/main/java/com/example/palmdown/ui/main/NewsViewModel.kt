package com.example.palmdown.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.palmdown.model.News
import com.example.palmdown.repository.NewsRepository
import com.example.palmdown.worker.NewsBackgroundWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository = NewsRepository()
) : ViewModel() {

    private val _news = MutableStateFlow<List<News>>(emptyList())
    val news: StateFlow<List<News>> = _news

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadNews() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _news.value = repository.getAllNews()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh(context: Context) {
        _isLoading.value = true

        val request = OneTimeWorkRequestBuilder<NewsBackgroundWorker>().build()

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
                    loadNews()
                }
            }
    }
}
