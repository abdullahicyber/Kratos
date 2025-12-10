package com.cs250.kratos.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cs250.kratos.databinding.ItemWorkoutBinding
import com.cs250.kratos.model.Workout
import java.text.SimpleDateFormat
import java.util.Locale

// 1. CHANGE List<Workout> to MutableList<Workout> here
class WorkoutAdapter(
    private var workouts: MutableList<Workout>, // <-- FIX 1
    private val onDeleteClicked: (Workout) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {
    // --- START: New code for smooth updates ---
    /**
     *  This class helps the adapter efficiently calculate the difference
     *  between the old list and the new list.
     */
    private class WorkoutDiffCallback(
        private val oldList: List<Workout>,
        private val newList: List<Workout>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        // Checks if two items are the same entity (e.g., they have the same ID).
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        // Checks if the contents of the item have changed.
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    /**
     *  This function is called by the fragment to provide a new list of workouts.
     *  It uses DiffUtil to calculate changes and animate them smoothly.
     */
    fun updateWorkouts(newWorkouts: List<Workout>) {
        val diffCallback = WorkoutDiffCallback(this.workouts, newWorkouts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // 2. Clear the list and add all new items. This is now valid.
        this.workouts.clear()
        this.workouts.addAll(newWorkouts)
        diffResult.dispatchUpdatesTo(this) // This animates the changes!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(workouts[position])
    }

    override fun getItemCount() = workouts.size



    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: Workout) {
            // Set the workout name
            binding.textViewWorkoutName.text = workout.workoutName

            // 4. UPDATE THE STATS TEXT to include duration AND calories
            val stats = "${workout.durationMinutes} min - ${workout.caloriesBurned} kcal"
            binding.textViewWorkoutStats.text = stats

            // ... (keep your existing code for timestamp and delete button)
            binding.buttonDelete.setOnClickListener {
                onDeleteClicked(workout)
            }
        }
    }
}
