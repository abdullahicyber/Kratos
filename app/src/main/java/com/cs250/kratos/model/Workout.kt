package com.cs250.kratos.model

import com.google.firebase.firestore.DocumentId // <-- 1. IMPORT THIS
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
/**
 * Represents a single workout session logged by a user.
 *
 * This data class is optimized for Firestore. It uses special annotations
 * to automatically handle the document ID and server-side timestamps during
 * serialization and deserialization.
 *
 * @property id The unique Firestore document ID.
 * Annotated with @DocumentId, meaning this value is not stored as a field inside the document
 * data, but is automatically populated by the Firestore SDK using the document's key.
 * @property userId The ID of the user who performed the workout.
 * @property workoutName The name or type of workout (e.g., "Morning Run", "Leg Day").
 * @property durationMinutes The total duration of the workout in minutes.
 * @property caloriesBurned The estimated number of calories burned.
 * @property timestamp The time the workout was recorded.
 * Annotated with @ServerTimestamp. When writing to Firestore, this token tells the server
 * to insert the current server time (resolving client clock issues).
 * When reading, it is converted back to a java.util.Date.
 */
data class Workout(
    @DocumentId val id: String = "", // <-- 2. ADD THIS LINE
    val userId: String = "",
    val workoutName: String = "",
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    @ServerTimestamp val timestamp: Date? = null
)
