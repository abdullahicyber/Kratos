package com.cs250.kratos.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Workout(
    val userId: String = "",
    val workoutName: String = "",
    val durationMinutes: Int = 0,
    @ServerTimestamp val timestamp: Date? = null
)
