package com.cs250.kratos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // <-- IMPORT TOAST
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

class MyWorkoutsFragment : Fragment() {

    private var _binding: FragmentMyWorkoutsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var workoutAdapter: WorkoutAdapter
    private val workoutList = mutableListOf<Workout>()
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

        binding.btnStartWorkout.setOnClickListener {
            val intent = Intent(requireContext(), LogWorkoutActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        fetchWorkouts()
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter(workoutList) { workout ->
            deleteWorkout(workout)
        }
        binding.recyclerViewWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
    }

    private fun deleteWorkout(workout: Workout) {
        if (workout.id.isEmpty()) {
            Toast.makeText(context, "Cannot delete, workout ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- OPTIMISTIC UI: UPDATE THE SCREEN INSTANTLY ---

        // 1. Find the position of the item in the local list.
        val position = workoutList.indexOfFirst { it.id == workout.id }

        // 2. Remove it from the local list and notify the adapter of the exact removal.
        //    This triggers the nice "item removed" animation.
        if (position != -1) {
            workoutList.removeAt(position)
            workoutAdapter.notifyItemRemoved(position)
            Toast.makeText(context, "Workout deleted.", Toast.LENGTH_SHORT).show()
        }

        // --- Now, delete from Firestore in the background ---
        db.collection("workouts").document(workout.id)
            .delete()
            .addOnSuccessListener {
                // Success! The UI is already correct. Nothing more to do.
            }
            .addOnFailureListener { e ->
                // Uh oh, the delete failed. We need to add the item back to the UI.
                Toast.makeText(context, "Delete failed on server. Restoring item.", Toast.LENGTH_SHORT).show()
                if (position != -1) {
                    // Add the item back to where it was and notify the adapter.
                    workoutList.add(position, workout)
                    workoutAdapter.notifyItemInserted(position)
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

        listenerRegistration = db.collection("workouts")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val workouts = snapshots.toObjects(Workout::class.java)
                    // This is a more reliable way to update the adapter's data
                    workoutAdapter.updateWorkouts(workouts)

                    updateSummaryCards(workouts)

                    binding.textViewEmpty.isVisible = workouts.isEmpty()
                    binding.recyclerViewWorkouts.isVisible = workouts.isNotEmpty()
                }
            }
    }

    private fun updateSummaryCards(workouts: List<Workout>) {
        // Calculate workouts this week
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val oneWeekAgo = calendar.time

        val workoutsThisWeek = workouts.count { it.timestamp != null && it.timestamp.after(oneWeekAgo) }
        binding.textViewWorkoutsWeek.text = workoutsThisWeek.toString()

        // Calculate active streak
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
