package com.example.financesharing.domain.usecase

import com.example.financesharing.domain.model.GiftEventDomain
import com.example.financesharing.domain.repository.GiftEventRepository
import kotlinx.coroutines.flow.Flow

data class GiftEventsResult(
    val active: List<GiftEventDomain>,
    val completed: List<GiftEventDomain>
)

class GetGiftEventsUseCase(
    private val repository: GiftEventRepository
) {
    fun getActive(): Flow<List<GiftEventDomain>> = repository.getActiveEvents()
    fun getCompleted(): Flow<List<GiftEventDomain>> = repository.getCompletedEvents()
}


