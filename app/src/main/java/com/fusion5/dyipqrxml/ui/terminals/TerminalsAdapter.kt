package com.fusion5.dyipqrxml.ui.terminals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fusion5.dyipqrxml.R
import com.fusion5.dyipqrxml.databinding.ItemTerminalBinding
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

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
            
            // Show fare and travel time
            val fareInfo = "₱${"%.2f".format(route.fare)}"
            
            // Use the database travel time if available, otherwise calculate from route length
            val timeInfo = if (route.estimatedTravelTimeInSeconds != null) {
                val minutes = route.estimatedTravelTimeInSeconds / 60
                " • ${minutes} min"
            } else {
                // Calculate time from route length
                val routeLength = calculateRouteLength(route)
                val estimatedTime = calculateWalkingTime(routeLength)
                " • ${estimatedTime} min"
            }
            
            binding.textLocationInfo.text = "$fareInfo$timeInfo"
            binding.textLocationInfo.visibility = View.VISIBLE
            
            // Show route length and time in same format as HomeFragment
            val routeLength = calculateRouteLength(route)
            val routeLengthText = if (routeLength < 1.0) {
                "${"%.0f".format(routeLength * 1000)}m"
            } else {
                "${"%.1f".format(routeLength)}km"
            }
            
            val estimatedTime = calculateWalkingTime(routeLength)
            binding.textRouteDetails.text = "$routeLengthText / $estimatedTime min"
            binding.textRouteDetails.visibility = View.VISIBLE

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
        
        private fun calculateRouteLength(route: com.fusion5.dyipqrxml.data.model.Route): Double {
            return try {
                route.routeGeoJson?.let { geoJson ->
                    val jsonObject = JSONObject(geoJson)
                    val features = jsonObject.getJSONArray("features")
                    
                    if (features.length() > 0) {
                        val feature = features.getJSONObject(0)
                        val geometry = feature.getJSONObject("geometry")
                        
                        if (geometry.getString("type") == "LineString") {
                            val coordinates = geometry.getJSONArray("coordinates")
                            var totalLength = 0.0
                            
                            // Calculate total length of the route by summing distances between consecutive points
                            for (i in 0 until coordinates.length() - 1) {
                                val coord1 = coordinates.getJSONArray(i)
                                val coord2 = coordinates.getJSONArray(i + 1)
                                val lng1 = coord1.getDouble(0)
                                val lat1 = coord1.getDouble(1)
                                val lng2 = coord2.getDouble(0)
                                val lat2 = coord2.getDouble(1)
                                
                                val point1 = com.google.android.gms.maps.model.LatLng(lat1, lng1)
                                val point2 = com.google.android.gms.maps.model.LatLng(lat2, lng2)
                                
                                totalLength += calculateDistance(point1, point2)
                            }
                            
                            return totalLength
                        }
                    }
                }
                0.0
            } catch (e: Exception) {
                0.0
            }
        }
        
        private fun calculateDistance(point1: com.google.android.gms.maps.model.LatLng, point2: com.google.android.gms.maps.model.LatLng): Double {
            val earthRadius = 6371.0 // kilometers
            
            val lat1 = Math.toRadians(point1.latitude)
            val lon1 = Math.toRadians(point1.longitude)
            val lat2 = Math.toRadians(point2.latitude)
            val lon2 = Math.toRadians(point2.longitude)
            
            val dLat = lat2 - lat1
            val dLon = lon2 - lon1
            
            val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            
            return earthRadius * c
        }
        
        private fun calculateWalkingTime(distanceKm: Double): Int {
            // Assuming average walking speed of 5 km/h
            val walkingSpeedKmPerHour = 5.0
            val timeHours = distanceKm / walkingSpeedKmPerHour
            val timeMinutes = (timeHours * 60).toInt()
            
            // Ensure minimum of 1 minute for very short distances
            return maxOf(1, timeMinutes)
        }
    }

    object RouteDiffCallback : DiffUtil.ItemCallback<com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite>() {
        override fun areItemsTheSame(oldItem: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite, newItem: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite): Boolean =
            oldItem.route.id == newItem.route.id
            
        override fun areContentsTheSame(oldItem: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite, newItem: com.fusion5.dyipqrxml.ui.terminals.RouteWithFavorite): Boolean =
            oldItem == newItem
    }
}