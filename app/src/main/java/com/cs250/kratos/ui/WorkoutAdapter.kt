package com.cs250.kratos.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cs250.kratos.databinding.ItemWorkoutBinding
import com.cs250.kratos.model.Workout
import java.text.SimpleDateFormat
import java.util.Locale

class WorkoutAdapter(private val workouts: List<Workout>) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        holder.bind(workout)
    }

    override fun getItemCount() = workouts.size

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(workout: Workout) {
            binding.textViewWorkoutName.text = workout.workoutName
            binding.textViewWorkoutStats.text = "${workout.durationMinutes} minutes"

            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            binding.textViewWorkoutDate.text = workout.timestamp?.let { sdf.format(it) } ?: "No date"
        }
    }
}
