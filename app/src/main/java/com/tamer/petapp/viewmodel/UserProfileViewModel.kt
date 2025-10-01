package com.tamer.petapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.repository.ForumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class UserProfileViewModel(private val repository: ForumRepository) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // UI states
    sealed class UiState {
        object Loading : UiState()
        data class Success<T>(val data: T) : UiState()
        data class Error(val message: String, val throwable: Throwable? = null) : UiState()
    }

    // State flows
    private val _userPostsState = MutableStateFlow<UiState>(UiState.Loading)
    val userPostsState: StateFlow<UiState> = _userPostsState

    private val _likedPostsState = MutableStateFlow<UiState>(UiState.Loading)
    val likedPostsState: StateFlow<UiState> = _likedPostsState

    // Kullanıcının kendi gönderilerini yükle
    fun loadUserPosts() {
        _userPostsState.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _userPostsState.value = UiState.Error("Kullanıcı oturumu bulunamadı")
                    return@launch
                }
                
                repository.getUserPosts(currentUser.uid)
                    .catch { e ->
                        Log.e("UserProfileViewModel", "Kullanıcı gönderileri yüklenirken hata oluştu", e)
                        _userPostsState.value = UiState.Error(e.message ?: "Gönderiler yüklenirken hata oluştu", e)
                    }
                    .collect { posts ->
                        _userPostsState.value = UiState.Success(posts)
                    }
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Kullanıcı gönderileri yüklenirken beklenmeyen hata", e)
                _userPostsState.value = UiState.Error(e.message ?: "Gönderiler yüklenirken hata oluştu", e)
            }
        }
    }

    // Kullanıcının beğendiği gönderileri yükle
    fun loadLikedPosts() {
        _likedPostsState.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _likedPostsState.value = UiState.Error("Kullanıcı oturumu bulunamadı")
                    return@launch
                }
                
                repository.getLikedPosts(currentUser.uid)
                    .catch { e ->
                        Log.e("UserProfileViewModel", "Beğenilen gönderiler yüklenirken hata oluştu", e)
                        _likedPostsState.value = UiState.Error(e.message ?: "Beğenilen gönderiler yüklenirken hata oluştu", e)
                    }
                    .collect { posts ->
                        _likedPostsState.value = UiState.Success(posts)
                    }
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Beğenilen gönderiler yüklenirken beklenmeyen hata", e)
                _likedPostsState.value = UiState.Error(e.message ?: "Beğenilen gönderiler yüklenirken hata oluştu", e)
            }
        }
    }
}

class UserProfileViewModelFactory(private val repository: ForumRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı")
    }
} 