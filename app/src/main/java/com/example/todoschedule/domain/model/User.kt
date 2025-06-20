package com.example.todoschedule.domain.model

import com.example.todoschedule.core.constants.AppConstants.EMPTY_UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

/** 用户领域模型 */
data class User(
    val id: UUID,
    val username: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val createdAt: Instant = Clock.System.now(),
    val passwordHash: String? = null,
    val lastOpen: Instant = Clock.System.now(),
    val token: String? = null
)
