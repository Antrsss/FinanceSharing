package com.example.financesharing.presentation.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesharing.data.repository.firestore.FirestoreInvitationRepository
import com.example.financesharing.domain.model.Invitation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class InvitationsUiState {
    data object Loading : InvitationsUiState()
    data class Success(val invitations: List<Invitation>) : InvitationsUiState()
    data class Error(val message: String) : InvitationsUiState()
}

class InvitationsViewModel(
    private val repo: FirestoreInvitationRepository = FirestoreInvitationRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<InvitationsUiState>(InvitationsUiState.Loading)
    val uiState: StateFlow<InvitationsUiState> = _uiState.asStateFlow()

    init {
        observe()
    }

    private fun observe() {
        viewModelScope.launch {
            repo.observePendingInvitations()
                .catch { t ->
                    _uiState.value = InvitationsUiState.Error(t.message ?: "Не удалось загрузить приглашения")
                }
                .collect { list ->
                    _uiState.value = InvitationsUiState.Success(list)
                }
        }
    }

    fun respond(invitationId: String, accept: Boolean) {
        viewModelScope.launch {
            runCatching { repo.respondToInvitation(invitationId, accept) }
                .onFailure { t ->
                    _uiState.value = InvitationsUiState.Error(t.message ?: "Не удалось обновить приглашение")
                }
        }
    }
}

