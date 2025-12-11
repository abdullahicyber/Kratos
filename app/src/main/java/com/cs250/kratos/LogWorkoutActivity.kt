package com.cs250.kratos

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cs250.kratos.databinding.ActivityLogWorkoutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


/**
 * LogWorkoutActivity
 *
 * This activity handles the user interface for recording a new workout.
 * It allows the user to input a workout name and duration, calculates an
 * estimated calorie burn based on a standard formula, and saves the data
 * to Firebase Firestore.
 */
class LogWorkoutActivity : AppCompatActivity() {

    // ViewBinding instance to access UI elements defined in activity_log_workout.xml
    private lateinit var binding: ActivityLogWorkoutBinding
    // Firebase instances for Database (Firestore) and Authentication
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Called when the activity is first created.
     * Sets up the view binding and the click listener for the save button.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSaveWorkout.setOnClickListener {
            saveWorkout()
        }
    }

    /**
     * saveWorkout
     *
     * Validates user input, performs a calorie calculation, shows a loading state,
     * and writes the workout data to the "workouts" collection in Firestore.
     */
    private fun saveWorkout() {
        // 1. Retrieve and trim inputs from the EditText fields
        val workoutName = binding.editTextWorkoutName.text.toString().trim()
        val durationStr = binding.editTextDuration.text.toString().trim()

        // 2. Validate: Check if fields are empty
        if (workoutName.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Validate: Check if duration is a valid positive number
        val durationMinutes = durationStr.toIntOrNull()
        if (durationMinutes == null || durationMinutes <= 0) {
            Toast.makeText(this, "Please enter a valid positive duration", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Authentication Check: Ensure user is logged in before saving
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to save a workout", Toast.LENGTH_SHORT).show()
            return
        }

        // --- 2. START: CALORIE CALCULATION ---
        // We use averages for a simple estimation. This can be made more advanced later.
        val metValue = 5.0 // A generic MET value for moderate exercise.
        val averageUserWeightKg = 75.0 // An average user weight in kilograms.

        // Formula: (MET * 3.5 * UserWeightKG / 200) * DurationInMinutes
        val caloriesPerMinute = (metValue * 3.5 * averageUserWeightKg) / 200
        val totalCalories = (caloriesPerMinute * durationMinutes * 2).toInt()
        // --- END: CALORIE CALCULATION ---

        // Show progress indicator and disable the save button to prevent double-taps
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveWorkout.isEnabled = false

        // Create the workout data object, including the calculated calories
        val workout = hashMapOf(
            "userId" to currentUser.uid,
            "workoutName" to workoutName,
            "durationMinutes" to durationMinutes,
            "caloriesBurned" to totalCalories, // <-- 3. ADD THE CALCULATED CALORIES
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Save the workout to Firestore
        db.collection("workouts")
            .add(workout)
            .addOnSuccessListener {
                // Give a brief delay for a better user experience, then finish
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(this, "Workout saved successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close this activity and go back
                }, 500)
            }
            .addOnFailureListener { e ->
                // If saving fails, re-enable the UI and show an error
                binding.progressBar.visibility = View.GONE
                binding.btnSaveWorkout.isEnabled = true
                Toast.makeText(this, "Error saving workout: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}

