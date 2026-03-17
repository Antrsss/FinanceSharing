package com.example.financesharing.data.repository

import com.example.financesharing.domain.model.GiftEventDomain
import com.example.financesharing.domain.repository.GiftEventRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import kotlin.random.Random

class MockGiftEventRepository : GiftEventRepository {

    override fun getActiveEvents(): Flow<List<GiftEventDomain>> = flow {
        delay(800) // imitate network delay
        emit(sampleEvents().filter { it.currentAmount < it.targetAmount })
    }

    override fun getCompletedEvents(): Flow<List<GiftEventDomain>> = flow {
        delay(800)
        emit(sampleEvents().filter { it.currentAmount >= it.targetAmount })
    }

    private fun sampleEvents(): List<GiftEventDomain> {
        val today = LocalDate.now()
        return listOf(
            GiftEventDomain(
                id = "1",
                title = "Подарок коллеге на день рождения",
                description = "Коллективный подарок на умные часы",
                targetAmount = 30000.0,
                currentAmount = 18500.0,
                deadline = today.plusDays(5),
                participantsCount = 12,
                imageUrl = null
            ),
            GiftEventDomain(
                id = "2",
                title = "Подарок родителям",
                description = "Теплый плед и ужин в ресторане",
                targetAmount = 20000.0,
                currentAmount = 21000.0,
                deadline = today.minusDays(1),
                participantsCount = 8,
                imageUrl = null
            ),
            GiftEventDomain(
                id = "3",
                title = "Совместный подарок другу",
                description = "Билеты на концерт",
                targetAmount = 15000.0,
                currentAmount = 6500.0,
                deadline = today.plusDays(10),
                participantsCount = 5,
                imageUrl = null
            ),
            GiftEventDomain(
                id = "4",
                title = "Подарок учителю",
                description = "Книга и сертификат в магазин",
                targetAmount = 12000.0,
                currentAmount = 12000.0,
                deadline = today,
                participantsCount = 20,
                imageUrl = null
            )
        ).shuffled(Random(today.toEpochDay()))
    }
}


