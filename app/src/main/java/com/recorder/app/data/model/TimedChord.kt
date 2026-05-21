package com.recorder.app.data.model

data class TimedChord(
    val chord: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)
