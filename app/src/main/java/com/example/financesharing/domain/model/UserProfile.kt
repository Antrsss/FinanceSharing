package com.example.financesharing.domain.model

data class UserProfile(
    val uid: String,
    val username: String,
    val email: String,
    val avatarId: String,
    val baseCurrency: Currency,
    val walletBalanceMinor: Long,
    val spentTotalMinor: Long,
    val bonusPoints: Long
)

