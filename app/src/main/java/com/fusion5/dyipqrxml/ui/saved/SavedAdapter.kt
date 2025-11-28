package com.fusion5.dyipqrxml.ui.saved

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fusion5.dyipqrxml.data.model.Route
import com.fusion5.dyipqrxml.databinding.ItemSavedTerminalBinding

class SavedAdapter(
    private val onRouteClick: (Route) -> Unit,
    private val onRemoveFavorite: (Route) -> Unit
) : ListAdapter<Route, SavedAdapter.SavedTerminalViewHolder>(SavedTerminalDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedTerminalViewHolder {
        val binding = ItemSavedTerminalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedTerminalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedTerminalViewHolder, position: Int) {
        val route = getItem(position)
        holder.bind(route)
    }

    inner class SavedTerminalViewHolder(
        private val binding: ItemSavedTerminalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(route: Route) {
            binding.textTerminalName.text = route.routeCode
//            binding.textTerminalDescription.text = "${route.startTerminalName} → ${route.endTerminalName}"
            
            // Show fare and travel time info
            val fareInfo = "Fare: ₱${route.fare}"
            val timeInfo = if (route.estimatedTravelTimeInSeconds != null) {
                val minutes = route.estimatedTravelTimeInSeconds / 60
                " • ${minutes} min"
            } else ""
            
            binding.textLocationInfo.text = fareInfo + timeInfo
            binding.textLocationInfo.visibility = View.VISIBLE

            binding.root.setOnClickListener {
                onRouteClick(route)
            }

            binding.buttonRemove.setOnClickListener {
                onRemoveFavorite(route)
            }
        }
    }

    object SavedTerminalDiffCallback : DiffUtil.ItemCallback<Route>() {
        override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
            return oldItem == newItem
        }
    }
}