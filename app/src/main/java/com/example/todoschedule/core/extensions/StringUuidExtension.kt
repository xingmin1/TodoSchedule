package com.example.todoschedule.core.extensions

import com.example.todoschedule.core.constants.AppConstants
import java.util.UUID

fun String.toUuidOrNull(): UUID? {
    return try {
        UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun String.toUuid(): UUID {
    return UUID.fromString(this)
}

fun UUID.valid(): Boolean {
    return this != AppConstants.EMPTY_UUID
}

fun List<UUID>.toStringList(): List<String> {
    return this.map { it.toString() }
}