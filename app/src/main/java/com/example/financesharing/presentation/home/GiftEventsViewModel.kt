package com.example.financesharing.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.financesharing.data.repository.FirestoreGiftEventRepository
import com.example.financesharing.domain.model.GiftEventDomain
import com.example.financesharing.domain.usecase.GetGiftEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class GiftEventsUiState {
    data object Loading : GiftEventsUiState()
    data class Success(
        val active: List<GiftEventDomain>,
        val completed: List<GiftEventDomain>
    ) : GiftEventsUiState()

    data class Error(val message: String) : GiftEventsUiState()
}

class GiftEventsViewModel(
    private val useCase: GetGiftEventsUseCase
) : ViewModel() {

    private val _uiState: MutableStateFlow<GiftEventsUiState> =
        MutableStateFlow(GiftEventsUiState.Loading)
    val uiState: StateFlow<GiftEventsUiState> = _uiState.asStateFlow()

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            combine(
                useCase.getActive(),
                useCase.getCompleted()
            ) { active, completed ->
                GiftEventsUiState.Success(
                    active = active,
                    completed = completed
                )
            }
                .catch { throwable ->
                    _uiState.value = GiftEventsUiState.Error(
                        throwable.message ?: "Не удалось загрузить сборы"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
}

class GiftEventsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GiftEventsViewModel::class.java)) {
            val repository = FirestoreGiftEventRepository()
            val useCase = GetGiftEventsUseCase(repository)
            return GiftEventsViewModel(useCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}


