package com.example.financesharing.domain.repository

import com.example.financesharing.domain.model.GiftEventDomain
import kotlinx.coroutines.flow.Flow

interface GiftEventRepository {
    fun getActiveEvents(): Flow<List<GiftEventDomain>>
    fun getCompletedEvents(): Flow<List<GiftEventDomain>>
}


