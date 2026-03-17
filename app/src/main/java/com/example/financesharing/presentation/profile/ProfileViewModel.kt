package com.example.financesharing.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesharing.data.repository.firestore.FirestoreUserRepository
import com.example.financesharing.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repo: FirestoreUserRepository = FirestoreUserRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observe()
    }

    private fun observe() {
        viewModelScope.launch {
            repo.observeMyProfile()
                .catch { t ->
                    _uiState.value = ProfileUiState.Error(t.message ?: "Не удалось загрузить профиль")
                }
                .collect { profile ->
                    if (profile == null) {
                        _uiState.value = ProfileUiState.Error("Профиль не найден")
                    } else {
                        _uiState.value = ProfileUiState.Success(profile)
                    }
                }
        }
    }

    fun setAvatar(avatarId: String) {
        viewModelScope.launch {
            runCatching { repo.setAvatar(avatarId) }
                .onFailure { t ->
                    _uiState.value = ProfileUiState.Error(t.message ?: "Не удалось обновить аватар")
                }
        }
    }

    fun topUp(amountMinor: Long) {
        viewModelScope.launch {
            runCatching { repo.topUpWallet(amountMinor) }
                .onFailure { t ->
                    _uiState.value = ProfileUiState.Error(t.message ?: "Не удалось пополнить кошелёк")
                }
        }
    }
}

