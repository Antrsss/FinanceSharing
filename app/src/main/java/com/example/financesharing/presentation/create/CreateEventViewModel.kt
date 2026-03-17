package com.example.financesharing.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesharing.data.repository.firestore.FirestoreEventRepository
import com.example.financesharing.domain.model.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class CreateEventUiState {
    data object Idle : CreateEventUiState()
    data object Loading : CreateEventUiState()
    data class Error(val message: String) : CreateEventUiState()
    data class Created(val eventId: String) : CreateEventUiState()
}

class CreateEventViewModel(
    private val repo: FirestoreEventRepository = FirestoreEventRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<CreateEventUiState>(CreateEventUiState.Idle)
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    fun create(
        title: String,
        description: String,
        currency: Currency,
        targetAmountMinor: Long,
        deadline: LocalDate
    ) {
        viewModelScope.launch {
            _uiState.value = CreateEventUiState.Loading
            try {
                val id = repo.createEvent(
                    title = title,
                    description = description,
                    currency = currency,
                    targetAmountMinor = targetAmountMinor,
                    deadline = deadline
                )
                _uiState.value = CreateEventUiState.Created(id)
            } catch (t: Throwable) {
                _uiState.value = CreateEventUiState.Error(t.message ?: "Не удалось создать сбор")
            }
        }
    }

    fun consumeCreated() {
        if (_uiState.value is CreateEventUiState.Created) {
            _uiState.value = CreateEventUiState.Idle
        }
    }
}

