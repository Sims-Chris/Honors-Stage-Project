package com.example.hsp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WorkoutAdapter(private var workouts: List<WorkoutSummary>) :
    RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    class WorkoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.workout_title)
        val intensity: TextView = view.findViewById(R.id.intensity_text)
        val summary: TextView = view.findViewById(R.id.summary_list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_summary, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        holder.title.text = workout.title
        holder.intensity.text = workout.intensity
        holder.summary.text = workout.summary
    }

    override fun getItemCount() = workouts.size

    fun updateWorkouts(newWorkouts: List<WorkoutSummary>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }
}