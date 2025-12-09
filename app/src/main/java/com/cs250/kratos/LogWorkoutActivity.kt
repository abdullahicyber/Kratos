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

class LogWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogWorkoutBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSaveWorkout.setOnClickListener {
            saveWorkout()
        }
    }

    private fun saveWorkout() {
        val workoutName = binding.editTextWorkoutName.text.toString().trim()
        val durationStr = binding.editTextDuration.text.toString().trim()

        if (workoutName.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val durationMinutes = durationStr.toIntOrNull()
        if (durationMinutes == null) {
            Toast.makeText(this, "Please enter a valid duration", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to save a workout", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress and disable button
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveWorkout.isEnabled = false

        val workout = hashMapOf(
            "userId" to currentUser.uid,
            "workoutName" to workoutName,
            "durationMinutes" to durationMinutes,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("workouts")
            .add(workout)
            .addOnSuccessListener {
                // Wait for half a second before finishing
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(this, "Workout saved successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity and return
                }, 500)
            }
            .addOnFailureListener { e ->
                // Hide progress and re-enable button on failure
                binding.progressBar.visibility = View.GONE
                binding.btnSaveWorkout.isEnabled = true
                Toast.makeText(this, "Error saving workout: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
