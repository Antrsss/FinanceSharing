package com.example.financesharing.domain.model

import java.time.LocalDate

data class GiftEvent(
    val id: String,
    val ownerUid: String,
    val title: String,
    val description: String,
    val currency: Currency,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long,
    val expectedParticipantsCount: Int,
    val deadline: LocalDate,
    val createdAtEpochMillis: Long
)

enum class ParticipantStatus { INVITED, ACCEPTED, DECLINED }

sealed class Participant {
    abstract val id: String
    abstract val status: ParticipantStatus

    data class User(
        override val id: String,
        val uid: String,
        val username: String,
        override val status: ParticipantStatus
    ) : Participant()

    data class Guest(
        override val id: String,
        val displayName: String,
        override val status: ParticipantStatus
    ) : Participant()
}

sealed class Contributor {
    data class User(val uid: String, val username: String) : Contributor()
    data class Guest(val displayName: String) : Contributor()
}

data class Contribution(
    val id: String,
    val contributor: Contributor,
    val amountMinor: Long,
    val createdAtEpochMillis: Long
)

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED }

data class Invitation(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val fromUid: String,
    val toUid: String,
    val status: InvitationStatus,
    val createdAtEpochMillis: Long
)

