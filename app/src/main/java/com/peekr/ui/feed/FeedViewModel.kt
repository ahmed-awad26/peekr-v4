package com.peekr.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.entities.PostEntity
import com.peekr.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<PostEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val selectedPlatform: String = "all",
    val error: String? = null,
    val unreadCount: Int = 0
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
        observeUnreadCount()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            feedRepository.getAllPosts()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { posts ->
                    _uiState.update { state ->
                        val filtered = if (state.selectedPlatform == "all") posts
                        else posts.filter { it.platformId == state.selectedPlatform }
                        state.copy(posts = filtered, isLoading = false)
                    }
                }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            feedRepository.getUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun selectPlatform(platformId: String) {
        _uiState.update { it.copy(selectedPlatform = platformId) }
        viewModelScope.launch {
            val flow = if (platformId == "all") feedRepository.getAllPosts()
            else feedRepository.getPostsByPlatform(platformId)

            flow.collect { posts ->
                _uiState.update { it.copy(posts = posts) }
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            try {
                feedRepository.syncAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun markAsRead(postId: Long) {
        viewModelScope.launch {
            feedRepository.markAsRead(postId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
