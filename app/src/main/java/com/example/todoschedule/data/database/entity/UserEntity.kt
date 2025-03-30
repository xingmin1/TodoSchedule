package com.example.todoschedule.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** 用户实体类 */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val createdAt: Instant = Clock.System.now(),
    val passwordHash: String? = null,
    val lastOpen: Instant = Clock.System.now(),
    val token: String? = null
)
