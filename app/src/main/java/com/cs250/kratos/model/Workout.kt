package com.cs250.kratos.model

import com.google.firebase.firestore.DocumentId // <-- 1. IMPORT THIS
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Workout(
    @DocumentId val id: String = "", // <-- 2. ADD THIS LINE
    val userId: String = "",
    val workoutName: String = "",
    val durationMinutes: Int = 0,
    @ServerTimestamp val timestamp: Date? = null
)
