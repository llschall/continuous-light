package org.llschall.continuous.light.request

import java.awt.Color

enum class Status(val color: Color) {

    COMPLETED(Color.green),
    IN_PROGRESS(Color.yellow),
    UNKNOWN(Color.red);

    fun fromString(value: String): Status {
        return when (value.lowercase()) {
            "completed" -> COMPLETED
            "in_progress" -> IN_PROGRESS
            else -> UNKNOWN
        }
    }
}