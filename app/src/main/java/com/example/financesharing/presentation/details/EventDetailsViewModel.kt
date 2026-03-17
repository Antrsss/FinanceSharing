package com.example.financesharing.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesharing.data.firebase.FirebaseProvider
import com.example.financesharing.data.repository.firestore.FirestoreEventRepository
import com.example.financesharing.domain.model.Contribution
import com.example.financesharing.domain.model.GiftEvent
import com.example.financesharing.domain.model.Participant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class EventDetailsUiState(
    val loading: Boolean = true,
    val event: GiftEvent? = null,
    val participants: List<Participant> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
    val errorMessage: String? = null
) {
    val isOwner: Boolean
        get() = event?.ownerUid != null && event.ownerUid == FirebaseProvider.auth.currentUser?.uid
}

class EventDetailsViewModel(
    private val repo: FirestoreEventRepository = FirestoreEventRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailsUiState())
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    fun start(eventId: String) {
        viewModelScope.launch {
            _uiState.value = EventDetailsUiState(loading = true)
            combine(
                repo.observeEvent(eventId),
                repo.observeParticipants(eventId),
                repo.observeContributions(eventId)
            ) { event, participants, contributions ->
                EventDetailsUiState(
                    loading = false,
                    event = event,
                    participants = participants,
                    contributions = contributions,
                    errorMessage = null
                )
            }
                .catch { t ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        errorMessage = t.message ?: "Не удалось загрузить детали"
                    )
                }
                .collect { _uiState.value = it }
        }
    }

    suspend fun addMyContribution(eventId: String, amountMinor: Long) {
        repo.addUserContribution(eventId, amountMinor)
    }

    suspend fun addGuestContribution(eventId: String, guestName: String, amountMinor: Long) {
        repo.addGuestContribution(eventId, guestName, amountMinor)
    }

    suspend fun inviteByUsernameOrEmail(eventId: String, eventTitle: String, query: String): Boolean {
        val toUid = repo.findUserUidByUsernameOrEmail(query) ?: return false
        repo.inviteUser(eventId = eventId, toUid = toUid, eventTitle = eventTitle)
        return true
    }
}

