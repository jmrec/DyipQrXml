package com.fusion5.dyipqrxml.ui.terminals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fusion5.dyipqrxml.R
import com.fusion5.dyipqrxml.databinding.ItemTerminalBinding

class TerminalsAdapter(
    private val onRouteClick: (com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite) -> Unit,
    private val onFavoriteToggle: (com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite) -> Unit
) : ListAdapter<com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite, TerminalsAdapter.RouteViewHolder>(RouteDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemTerminalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RouteViewHolder(
        private val binding: ItemTerminalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(routeWithFavorite: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite) {
            val route = routeWithFavorite.route
            
            // Set route information
            binding.textTerminalName.text = route.routeCode
            binding.textTerminalDescription.text = "${route.startTerminalName} → ${route.endTerminalName}"
            
            // Show fare and travel time info
            val fareInfo = "Fare: ₱${route.fare}"
            val timeInfo = if (route.estimatedTravelTimeInSeconds != null) {
                val minutes = route.estimatedTravelTimeInSeconds / 60
                " • ${minutes} min"
            } else ""
            
            binding.textLocationInfo.text = fareInfo + timeInfo
            binding.textLocationInfo.visibility = View.VISIBLE

            // Update favorite button
            binding.buttonFavorite.visibility = View.VISIBLE
            if (routeWithFavorite.isFavorite) {
                binding.buttonFavorite.setImageResource(R.drawable.ic_bookmark_added)
                binding.buttonFavorite.contentDescription = itemView.context.getString(R.string.favorite_remove_desc)
            } else {
                binding.buttonFavorite.setImageResource(R.drawable.ic_bookmark_add)
                binding.buttonFavorite.contentDescription = itemView.context.getString(R.string.favorite_add_desc)
            }

            // Set click listeners
            binding.root.setOnClickListener {
                onRouteClick(routeWithFavorite)
            }
            
            binding.buttonFavorite.setOnClickListener {
                onFavoriteToggle(routeWithFavorite)
            }
        }
    }

    object RouteDiffCallback : DiffUtil.ItemCallback<com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite>() {
        override fun areItemsTheSame(oldItem: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite, newItem: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite): Boolean =
            oldItem.route.id == newItem.route.id
            
        override fun areContentsTheSame(oldItem: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite, newItem: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite): Boolean =
            oldItem == newItem
    }
}