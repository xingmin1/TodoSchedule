package com.example.todoschedule.core.extensions

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