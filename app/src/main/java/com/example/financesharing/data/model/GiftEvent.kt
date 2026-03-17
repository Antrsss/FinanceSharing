package com.example.financesharing.data.model

import java.time.LocalDate

data class GiftEvent(
    val id: String,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: LocalDate,
    val participantsCount: Int,
    val imageUrl: String?
)


