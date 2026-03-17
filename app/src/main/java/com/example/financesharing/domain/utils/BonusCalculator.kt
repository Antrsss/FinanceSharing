package com.example.financesharing.domain.utils

object BonusCalculator {
    // 1 bonus per each N major currency units (e.g. 1 bonus per 100.00)
    const val BONUS_STEP_MAJOR: Long = 100

    fun calculateBonusPoints(spentTotalMinor: Long): Long {
        val stepMinor = BONUS_STEP_MAJOR * 100L
        if (stepMinor <= 0L) return 0L
        return spentTotalMinor / stepMinor
    }
}

