package org.llschall.continuous.light.request

import java.awt.Color

enum class Status(val color: Color) {

    QUEUED(Color.orange),
    IN_PROGRESS(Color.yellow),
    COMPLETED(Color.green),
    UNKNOWN(Color.red);

    fun fromString(value: String): Status {
        return when (value.lowercase()) {
            "queued" -> QUEUED
            "in_progress" -> IN_PROGRESS
            "completed" -> COMPLETED
            else -> UNKNOWN
        }
    }
}