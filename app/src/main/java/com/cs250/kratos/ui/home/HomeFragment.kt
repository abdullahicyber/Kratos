package com.cs250.kratos.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cs250.kratos.databinding.FragmentHomeBinding
import com.cs250.kratos.model.Workout
import com.cs250.kratos.ui.WorkoutAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var workoutAdapter: WorkoutAdapter
    private val workoutList = mutableListOf<Workout>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadWorkouts()
    }

    // Refresh data when you return to this screen
    override fun onResume() {
        super.onResume()
        loadWorkouts()
    }

    private fun setupRecyclerView() {
        // 7. PASS THE DELETE FUNCTION TO THE ADAPTER'S CONSTRUCTOR
        workoutAdapter = WorkoutAdapter(workoutList) { workout ->
            deleteWorkout(workout)
        }
        binding.recyclerViewWorkouts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutAdapter
        }
    }

    private fun loadWorkouts() {
        val currentUser = auth.currentUser ?: return
        binding.progressBar.visibility = View.VISIBLE

        db.collection("workouts")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                workoutList.clear()
                workoutList.addAll(documents.toObjects(Workout::class.java))
                workoutAdapter.notifyDataSetChanged() // Use notifyDataSetChanged for simplicity
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to load workouts.", Toast.LENGTH_SHORT).show()
            }
    }

    // 8. ADD THIS FUNCTION TO HANDLE DELETION
    private fun deleteWorkout(workout: Workout) {
        // Use the 'id' field that we added in Step 1
        db.collection("workouts").document(workout.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Workout deleted successfully", Toast.LENGTH_SHORT).show()
                // Reload the list from Firestore to ensure UI is consistent
                loadWorkouts()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting workout: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
