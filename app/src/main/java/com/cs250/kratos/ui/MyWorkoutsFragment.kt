package com.cs250.kratos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cs250.kratos.LogWorkoutActivity
import com.cs250.kratos.databinding.FragmentMyWorkoutsBinding
import com.cs250.kratos.model.Workout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
/**
 * Displays the user's workout history and summary statistics.
 *
 * Key Features:
 * 1. Real-time list of workouts (fetches from Firestore).
 * 2. Dashboard cards showing weekly stats (Total Workouts, Calories, Streak).
 * 3. Optimistic UI updates when deleting items (removes them instantly from the screen).
 */
class MyWorkoutsFragment : Fragment() {

    private var _binding: FragmentMyWorkoutsBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var workoutAdapter: WorkoutAdapter
    // Local list copy to support Optimistic UI updates
    private val workoutList = mutableListOf<Workout>()
    // Reference to the Firestore listener so we can detach it when the user leaves the screen
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
// Navigate to the "Add Workout" screen
        binding.btnStartWorkout.setOnClickListener {
            val intent = Intent(requireContext(), LogWorkoutActivity::class.java)
            startActivity(intent)
        }
    }
    // Start listening for data changes only when the screen is visible to save data/battery
    override fun onStart() {
        super.onStart()
        fetchWorkouts()
    }
    // Stop listening when the screen is hidden (e.g., user goes to Home screen)
    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }

    private fun setupRecyclerView() {
        // Initialize adapter with the delete callback
        workoutAdapter = WorkoutAdapter(workoutList) { workout ->
            deleteWorkout(workout)
        }
        binding.recyclerViewWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
    }
    /**
     * Deletes a workout using the "Optimistic UI" pattern.
     * * Strategy:
     * 1. Remove the item from the screen IMMEDIATELY (user feels zero lag).
     * 2. Recalculate stats (calories/streak) immediately based on the new list.
     * 3. Send the delete request to the server in the background.
     * 4. If the server fails, "Rollback" by adding the item back to the screen.
     */
    private fun deleteWorkout(workout: Workout) {
        if (workout.id.isEmpty()) {
            Toast.makeText(context, "Cannot delete, workout ID is missing.", Toast.LENGTH_SHORT).show();
            return
        }

        // --- OPTIMISTIC UI: UPDATE THE SCREEN INSTANTLY ---

        val position = workoutList.indexOfFirst { it.id == workout.id }

        if (position != -1) {
            // Remove the item from the local list
            workoutList.removeAt(position)
            // Notify the adapter to animate the removal
            workoutAdapter.notifyItemRemoved(position)

            // --- THE FIX IS HERE ---
            // After removing the item from the local list,
            // immediately re-calculate and update the summary cards.
            updateSummaryCards(workoutList)

            Toast.makeText(context, "Workout deleted.", Toast.LENGTH_SHORT).show()
        }

        // --- Now, delete the item from Firestore in the background ---
        db.collection("workouts").document(workout.id)
            .delete()
            .addOnSuccessListener {
                // Success! UI is already correct.
            }
            .addOnFailureListener { e ->
                // ERROR! The delete failed on the server. We must add the item back.
                Toast.makeText(context, "Delete failed on server. Restoring item.", Toast.LENGTH_SHORT).show()
                if (position != -1) {
                    // Add the item back to the list
                    workoutList.add(position, workout)
                    // Notify the adapter to animate the item coming back
                    workoutAdapter.notifyItemInserted(position)

                    // --- AND FIX THE CARDS AGAIN ON FAILURE ---
                    // Re-calculate the cards with the restored list.
                    updateSummaryCards(workoutList)
                }
            }
    }



    private fun fetchWorkouts() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.textViewEmpty.isVisible = true
            binding.recyclerViewWorkouts.isVisible = false
            return
        }
// Listen for real-time updates (adds, edits, deletes)
        listenerRegistration = db.collection("workouts")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)// Newest first
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val workouts = snapshots.toObjects(Workout::class.java)
                    workoutAdapter.updateWorkouts(workouts) // Use the DiffUtil update method
// Recalculate the top dashboard stats whenever data changes
                    updateSummaryCards(workouts)
// Toggle empty state view
                    binding.textViewEmpty.isVisible = workouts.isEmpty()
                    binding.recyclerViewWorkouts.isVisible = workouts.isNotEmpty()
                }
            }
    }
    /**
     * Calculates and updates the dashboard statistics based on the provided list.
     */
    private fun updateSummaryCards(workouts: List<Workout>) {
        // --- Get all workouts from the last 7 days ---
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val oneWeekAgo = calendar.time
        val weeklyWorkouts = workouts.filter { it.timestamp != null && it.timestamp.after(oneWeekAgo) }

        // --- Update UI Cards ---
        // Update workouts this week count
        binding.textViewWorkoutsWeek.text = weeklyWorkouts.size.toString()

        // (NEW) Update calories this week count
        val totalCaloriesThisWeek = weeklyWorkouts.sumOf { it.caloriesBurned }
        binding.textViewCaloriesWeek.text = totalCaloriesThisWeek.toString()

        // Update active streak
        val activeStreak = calculateActiveStreak(workouts)
        binding.textViewActiveStreak.text = "$activeStreak Days"
    }


    private fun calculateActiveStreak(workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val today = calendar.time

        // Check if the most recent workout was today or yesterday
        val mostRecentWorkoutDate = workouts.first().timestamp ?: return 0
        val diff = today.time - mostRecentWorkoutDate.time
        if (TimeUnit.MILLISECONDS.toDays(diff) > 1) {
            return 0
        }

        var streak = 1
        var lastWorkoutDate = getStartOfDay(mostRecentWorkoutDate)

        for (i in 1 until workouts.size) {
            val currentWorkoutDate = workouts[i].timestamp ?: continue
            val startOfCurrentWorkoutDay = getStartOfDay(currentWorkoutDate)

            val dayDifference = TimeUnit.MILLISECONDS.toDays(lastWorkoutDate.time - startOfCurrentWorkoutDay.time)

            if (dayDifference == 1L) {
                streak++
                lastWorkoutDate = startOfCurrentWorkoutDay
            } else if (dayDifference > 1L) {
                break // Gap in dates, streak is broken
            }
        }
        return streak
    }
    /**
     * Helper to strip time information (hours, mins, secs) from a Date object.
     * Useful for comparing "Day 1" vs "Day 2" regardless of what time the workout happened.
     */
    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
