package com.fusion5.dyipqrxml

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fusion5.dyipqrxml.data.local.DatabaseSeeder
import com.fusion5.dyipqrxml.data.local.DyipQrDatabase
import com.fusion5.dyipqrxml.data.local.entity.RouteWithTerminals
import com.fusion5.dyipqrxml.data.local.repository.LocalTerminalRepository
import com.fusion5.dyipqrxml.data.model.Favorite
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.FavoriteRepository
import com.fusion5.dyipqrxml.data.repository.RouteRepository
import com.fusion5.dyipqrxml.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class HomeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequestCode = 1001
    
    private val markers = mutableListOf<Marker>()
    private val routeLayers = mutableListOf<GeoJsonLayer>()
    private val routePolylines = mutableListOf<Polyline>()
    private val routeLabelMarkers = mutableListOf<Marker>()
    
    private var displayTerminalsJob: Job? = null
    private var displayRoutesJob: Job? = null
    private var searchJob: Job? = null
    
    private lateinit var authRepository: AuthRepository
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var routeRepository: RouteRepository
    
    private var currentDisplayedRoute: RouteWithTerminals? = null
    private var selectedRouteLayer: GeoJsonLayer? = null

    // Baguio center coordinates
    private val baguioCenter = LatLng(16.4023, 120.5960)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // Initialize repositories
        authRepository = ServiceLocator.provideAuthRepository(requireContext())
        favoriteRepository = ServiceLocator.provideFavoriteRepository(requireContext())
        routeRepository = ServiceLocator.provideRouteRepository(requireContext())
        
        setupClickListeners()
        setupSearch()
        checkLocationPermission()
        
        // Hide welcome banner after 5 seconds
        view.postDelayed({
            if (isAdded && _binding != null) {
                binding.cardWelcome.visibility = View.GONE
            }
        }, 3000)
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("HomeFragment", "GoogleMap is ready")
        googleMap = map
        
        // Enable basic map features
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.setOnMarkerClickListener(this)
        
        // Add map click listener to clear selection
        googleMap.setOnMapClickListener {
            clearRouteSelection()
        }
        
        // Add map style for better visibility
        try {
            // You can customize the map style here if needed
            googleMap.setMapStyle(null) // Using default style for now
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting map style", e)
        }
        
        // Move camera to Baguio center
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(baguioCenter, 13f))
        Log.d("HomeFragment", "Camera moved to Baguio center")
        
        // Display Routes from DB (terminals will be hidden by default)
        displayRoutesFromDb()
        
        // Get current location if permission granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
        
        // Check if map is actually loaded
        googleMap.setOnMapLoadedCallback {
            Log.d("HomeFragment", "Map tiles loaded successfully")
        }
    }

    private fun displayTerminalsFromDb() {
        displayTerminalsJob?.cancel()
        val database = DyipQrDatabase.getInstance(requireContext())
        val terminalRepository = LocalTerminalRepository(database.terminalDao())
        
        displayTerminalsJob = viewLifecycleOwner.lifecycleScope.launch {
            terminalRepository.observeAll().collect { terminals ->
                Log.d("HomeFragment", "Loaded ${terminals.size} terminals (hidden by default)")
                // Clear existing markers but don't display new ones by default
                markers.forEach { it.remove() }
                markers.clear()
                
                // Store terminals for later use when routes are clicked
                // We'll create markers only when showTerminalsForRoute is called
            }
        }
    }
    
    private fun displayRoutesFromDb() {
        displayRoutesJob?.cancel()
        val database = DyipQrDatabase.getInstance(requireContext())
        val routeDao = database.routeDao()
        
        displayRoutesJob = viewLifecycleOwner.lifecycleScope.launch {
            routeDao.observeAllRoutesWithTerminals().collect { routes ->
                Log.d("HomeFragment", "Displaying ${routes.size} routes")
                Log.d("HomeFragment", "Route order: ${routes.map { it.route.routeCode }}")
                
                // Clear existing layers, polylines and labels
                routeLayers.forEach { it.removeLayerFromMap() }
                routeLayers.clear()
                routePolylines.forEach { it.remove() }
                routePolylines.clear()
                routeLabelMarkers.forEach { it.remove() }
                routeLabelMarkers.clear()

                // Use Polyline approach for guaranteed clickability
                routes.forEachIndexed { index, route ->
                    route.route.routeGeoJson?.let { geoJson ->
                        try {
                            Log.d("HomeFragment", "Processing route ${index + 1}/${routes.size}: ${route.route.routeCode}, has GeoJSON: ${geoJson.isNotEmpty()}")
                            
                            // Parse GeoJSON and create Polyline instead of GeoJsonLayer
                            val jsonObject = JSONObject(geoJson)
                            val features = jsonObject.getJSONArray("features")
                            
                            if (features.length() > 0) {
                                val feature = features.getJSONObject(0)
                                val geometry = feature.getJSONObject("geometry")
                                
                                if (geometry.getString("type") == "LineString") {
                                    val coordinates = geometry.getJSONArray("coordinates")
                                    val latLngList = mutableListOf<LatLng>()
                                    
                                    for (i in 0 until coordinates.length()) {
                                        val coord = coordinates.getJSONArray(i)
                                        val lng = coord.getDouble(0)
                                        val lat = coord.getDouble(1)
                                        latLngList.add(LatLng(lat, lng))
                                    }
                                    
                                    // Create polyline with click listener
                                    val polyline = googleMap.addPolyline(
                                        PolylineOptions()
                                            .addAll(latLngList)
                                            .color(getColorForRoute(route.route.routeCode))
                                            .width(12f)
                                            .clickable(true) // THIS IS KEY - make it clickable
                                    )
                                    
                                    // Store route reference in polyline tag
                                    polyline.tag = route
                                    routePolylines.add(polyline)
                                    
                                    // Route labels removed as requested
                                    
                                    Log.d("HomeFragment", "Successfully created polyline for route: ${route.route.routeCode}")
                                    Log.d("HomeFragment", "Polyline points: ${latLngList.size}")
                                }
                            }
                            
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error loading GeoJSON for route ${route.route.routeCode}", e)
                            Log.e("HomeFragment", "GeoJSON content: ${geoJson.take(200)}...")
                        }
                    } ?: run {
                        Log.w("HomeFragment", "Route ${route.route.routeCode} has null or empty GeoJSON")
                    }
                }
                
                // Set up polyline click listener
                setupPolylineClickListener()
                
                // Add debug button to test route clickability
                addDebugRouteTestButton()
            }
        }
    }
    
    private fun setupPolylineClickListener() {
        googleMap.setOnPolylineClickListener { polyline ->
            val route = polyline.tag as? RouteWithTerminals
            if (route != null) {
                Log.d("HomeFragment", "=== POLYLINE CLICK DETECTED ===")
                Log.d("HomeFragment", "Route clicked: ${route.route.routeCode}")
                selectRoute(route, null) // No layer needed for polyline approach
            }
        }
        
        // Add map click listener to clear selection
        googleMap.setOnMapClickListener {
            clearRouteSelection()
        }
    }
    
    private fun addDebugRouteTestButton() {
        // Add a debug button to manually test route clickability
        binding.fabCurrentLocation.setOnLongClickListener {
            testRouteClickability()
            true
        }
    }
    
    private fun testRouteClickability() {
        Log.d("HomeFragment", "=== ROUTE CLICKABILITY TEST ===")
        Log.d("HomeFragment", "Total route layers: ${routeLayers.size}")
        
        routeLayers.forEachIndexed { index, layer ->
            Log.d("HomeFragment", "Layer $index: ${layer.features.count()} features")
            layer.features.forEach { feature ->
                Log.d("HomeFragment", "  - Feature: ${feature.geometry?.geometryType}")
            }
        }
        
        // Show a dialog with test results
        AlertDialog.Builder(requireContext())
            .setTitle("Route Clickability Test")
            .setMessage("Total layers: ${routeLayers.size}\n" +
                       "Layers with features: ${routeLayers.count { it.features.any() }}\n" +
                       "Check logs for detailed analysis")
            .setPositiveButton("OK", null)
            .show()
    }
    
    
    private fun getColorForRoute(name: String): Int {
        // Use hash-based colors for consistent assignment across all routes
        val hash = name.hashCode()
        val colorIndex = (hash and 0x7FFFFFFF) % 6 // Ensure positive modulo
        return when (colorIndex) {
            0 -> Color.BLUE
            1 -> Color.RED
            2 -> Color.GREEN
            3 -> Color.MAGENTA
            4 -> Color.CYAN
            5 -> Color.YELLOW
            else -> Color.BLACK
        }
    }
    
    private fun addRouteLabel(route: RouteWithTerminals) {
        route.route.routeGeoJson?.let { geoJson ->
            try {
                val jsonObject = JSONObject(geoJson)
                val features = jsonObject.getJSONArray("features")
                if (features.length() > 0) {
                    val feature = features.getJSONObject(0)
                    val geometry = feature.getJSONObject("geometry")
                    if (geometry.getString("type") == "LineString") {
                        val coordinates = geometry.getJSONArray("coordinates")
                        if (coordinates.length() > 0) {
                            // Calculate midpoint of the route
                            val midIndex = coordinates.length() / 2
                            val midCoordinate = coordinates.getJSONArray(midIndex)
                            val latLng = LatLng(midCoordinate.getDouble(1), midCoordinate.getDouble(0))
                            
                            // Add label marker with distinct styling and shape
                            val marker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(route.route.routeCode)
                                    .snippet("Route: ${route.startTerminal.name} → ${route.endTerminal.name}")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)) // Orange for route labels
                                    .alpha(0.8f) // Slightly transparent
                                    .zIndex(1f) // Lower z-index than terminal markers
                            )
                            marker?.let { routeLabelMarkers.add(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error adding route label for ${route.route.routeCode}", e)
            }
        }
    }
    
    private fun selectRoute(route: RouteWithTerminals, layer: GeoJsonLayer?) {
        Log.d("HomeFragment", "Route selected: ${route.route.routeCode}")
        Log.d("HomeFragment", "Route has start terminal: ${route.startTerminal.name} (id: ${route.route.startTerminalId})")
        Log.d("HomeFragment", "Route has end terminal: ${route.endTerminal.name} (id: ${route.route.endTerminalId})")
        
        // Clear previous selection
        clearRouteSelection()
        
        // Highlight the selected route (for polyline approach)
        routePolylines.forEach { polyline ->
            val polylineRoute = polyline.tag as? RouteWithTerminals
            if (polylineRoute?.route?.id == route.route.id) {
                // Highlight selected route
                polyline.color = Color.rgb(255, 165, 0) // Orange
                polyline.width = 16f
            } else {
                // Reset other routes
                polyline.color = getColorForRoute(polylineRoute?.route?.routeCode ?: "")
                polyline.width = 12f
            }
        }
        
        // Show terminals for this route
        showTerminalsForRoute(route)
        
        // Show route info
        showRouteInfo(route)
    }
    
    private fun clearRouteSelection() {
        // Clear route highlighting for polylines
        routePolylines.forEach { polyline ->
            val route = polyline.tag as? RouteWithTerminals
            polyline.color = getColorForRoute(route?.route?.routeCode ?: "")
            polyline.width = 12f
        }
        
        // Hide terminals
        markers.forEach { it.remove() }
        markers.clear()
        
        // Hide route info
        binding.cardRouteInfo.visibility = View.GONE
        currentDisplayedRoute = null
    }
    
    private fun showTerminalsForRoute(route: RouteWithTerminals) {
        Log.d("HomeFragment", "=== SHOWING TERMINALS FOR ROUTE: ${route.route.routeCode} ===")
        
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // Clear ALL markers completely
                markers.forEach { it.remove() }
                markers.clear()
                googleMap.clear()
                
                // Re-add the routes since we cleared the map
                displayRoutesFromDb()
                
                // Extract terminal coordinates directly from the route GeoJSON
                val terminalPositions = extractTerminalPositionsFromRoute(route)
                
                // Add start terminal marker
                terminalPositions.start?.let { position ->
                    val routeParts = route.route.routeCode.split("-")
                    val startTerminalName = if (routeParts.size > 1) routeParts[0].trim() else "Start"
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title("Start: $startTerminalName")
                            .snippet("${route.route.routeCode} • Terminal for ${route.route.routeCode}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                    marker?.let { markers.add(it) }
                    Log.d("HomeFragment", "Added start terminal marker for ${route.route.routeCode} at: $position")
                }
                
                // Add end terminal marker
                terminalPositions.end?.let { position ->
                    val routeParts = route.route.routeCode.split("-")
                    val endTerminalName = if (routeParts.size > 1) routeParts[1].trim() else "End"
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title("End: $endTerminalName")
                            .snippet("${route.route.routeCode} • Terminal for ${route.route.routeCode}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                    marker?.let { markers.add(it) }
                    Log.d("HomeFragment", "Added end terminal marker for ${route.route.routeCode} at: $position")
                }
                
                // Zoom to show both terminals and the route with adjusted padding for UI elements
                if (markers.isNotEmpty()) {
                    val builder = LatLngBounds.Builder()
                    markers.forEach { marker -> builder.include(marker.position) }
                    val bounds = builder.build()
                    
                    // Use even more padding to ensure route visibility
                    val padding = 300 // Maximum padding to ensure route is fully visible
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    Log.d("HomeFragment", "Zoomed to show terminals for ${route.route.routeCode} with maximum padding: $padding")
                } else {
                    Log.w("HomeFragment", "No markers to zoom to for ${route.route.routeCode}")
                }
            }
        }
    }
    
    private fun extractTerminalPositionsFromRoute(route: RouteWithTerminals): TerminalPositions {
        try {
            val geoJson = JSONObject(route.route.routeGeoJson)
            val features = geoJson.getJSONArray("features")
            if (features.length() > 0) {
                val firstFeature = features.getJSONObject(0)
                val geometry = firstFeature.getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                
                if (coordinates.length() > 1) {
                    // First coordinate (start terminal)
                    val startCoord = coordinates.getJSONArray(0)
                    val startLng = startCoord.getDouble(0)
                    val startLat = startCoord.getDouble(1)
                    
                    // Last coordinate (end terminal)
                    val lastIndex = coordinates.length() - 1
                    val endCoord = coordinates.getJSONArray(lastIndex)
                    val endLng = endCoord.getDouble(0)
                    val endLat = endCoord.getDouble(1)
                    
                    return TerminalPositions(
                        start = LatLng(startLat, startLng),
                        end = LatLng(endLat, endLng)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error extracting terminal positions from route GeoJSON: ${e.message}")
        }
        
        return TerminalPositions(null, null)
    }
    
    data class TerminalPositions(val start: LatLng?, val end: LatLng?)

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        return false
    }

    private fun setupClickListeners() {
        binding.fabCurrentLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }

        binding.fabAddRoute.setOnClickListener {
            showAddBaguioRouteDialog()
        }
        
        // Add debug button to reset database (for testing)
        binding.fabCurrentLocation.setOnLongClickListener {
            resetDatabaseForDebug()
            true
        }

        binding.buttonViewRoutes.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_terminals)
        }
        
        binding.buttonFavoriteRoute.setOnClickListener {
            currentDisplayedRoute?.let { route ->
                toggleFavorite(route.route.id)
            }
        }
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300) // Debounce
                    performSearch(query)
                }
            }
        })
    }
    
    private suspend fun performSearch(query: String) {
        // Cancel default display jobs so they don't interfere
        displayTerminalsJob?.cancel()
        displayRoutesJob?.cancel()

        val database = DyipQrDatabase.getInstance(requireContext())
        val routeDao = database.routeDao()
        
        if (query.isBlank()) {
            // Reset to default view - show all routes
            withContext(Dispatchers.Main) {
                // Clear everything and show all routes
                markers.forEach { it.remove() }
                markers.clear()
                routePolylines.forEach { it.remove() }
                routePolylines.clear()
                displayRoutesFromDb()
            }
            return
        }
        
        // Clear current view
        withContext(Dispatchers.Main) {
            markers.forEach { it.remove() }
            markers.clear()
            routePolylines.forEach { it.remove() }
            routePolylines.clear()
        }
        
        // Search Routes only (no terminals)
        routeDao.searchRoutesWithTerminals(query).collect { routes ->
            withContext(Dispatchers.Main) {
                // Clear existing polylines from previous search
                routePolylines.forEach { it.remove() }
                routePolylines.clear()
                
                // Show only matching routes as polylines
                routes.forEach { route ->
                    route.route.routeGeoJson?.let { geoJson ->
                        try {
                            val jsonObject = JSONObject(geoJson)
                            val features = jsonObject.getJSONArray("features")
                            
                            if (features.length() > 0) {
                                val feature = features.getJSONObject(0)
                                val geometry = feature.getJSONObject("geometry")
                                
                                if (geometry.getString("type") == "LineString") {
                                    val coordinates = geometry.getJSONArray("coordinates")
                                    val latLngList = mutableListOf<LatLng>()
                                    
                                    for (i in 0 until coordinates.length()) {
                                        val coord = coordinates.getJSONArray(i)
                                        val lng = coord.getDouble(0)
                                        val lat = coord.getDouble(1)
                                        latLngList.add(LatLng(lat, lng))
                                    }
                                    
                                    // Create polyline for search result
                                    val polyline = googleMap.addPolyline(
                                        PolylineOptions()
                                            .addAll(latLngList)
                                            .color(getColorForRoute(route.route.routeCode))
                                            .width(12f)
                                            .clickable(true)
                                    )
                                    
                                    // Store route reference in polyline tag
                                    polyline.tag = route
                                    routePolylines.add(polyline)
                                    
                                    Log.d("HomeFragment", "Added search result polyline for route: ${route.route.routeCode}")
                                }
                            }
                            
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error loading GeoJSON for route ${route.route.routeCode}", e)
                        }
                    }
                }
                
                // Set up polyline click listener for search results
                setupPolylineClickListener()
                
                // Zoom to first result if available
                if (routes.isNotEmpty()) {
                    routes[0].route.routeGeoJson?.let { geoJson ->
                        try {
                            val jsonObject = JSONObject(geoJson)
                            val features = jsonObject.getJSONArray("features")
                            if (features.length() > 0) {
                                val feature = features.getJSONObject(0)
                                val geometry = feature.getJSONObject("geometry")
                                if (geometry.getString("type") == "LineString") {
                                    val coordinates = geometry.getJSONArray("coordinates")
                                    if (coordinates.length() > 0) {
                                        val firstCoord = coordinates.getJSONArray(0)
                                        val latLng = LatLng(firstCoord.getDouble(1), firstCoord.getDouble(0))
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error zooming to search result", e)
                        }
                    }
                }
            }
        }
    }

    private fun showRouteInfo(route: RouteWithTerminals) {
        Log.d("HomeFragment", "Showing route info for: ${route.route.routeCode}")
        currentDisplayedRoute = route
        binding.cardRouteInfo.visibility = View.VISIBLE
        
        binding.textRouteName.text = route.route.routeCode
        binding.textRouteDestination.text = "${route.startTerminal.name} → ${route.endTerminal.name}"
        binding.textRouteFare.text = "Fare: ₱${"%.2f".format(route.route.fare)}"
        val estimatedTime = if (route.route.estimatedTravelTimeInSeconds != null) {
            "${route.route.estimatedTravelTimeInSeconds / 60} mins"
        } else {
            "Unknown"
        }
        binding.textRouteTime.text = "Time: $estimatedTime"
        
        Log.d("HomeFragment", "Route info displayed: ${route.route.routeCode}, ${route.startTerminal.name} → ${route.endTerminal.name}")
        
        // Update favorite button state
        updateFavoriteButtonState(route.route.id)
    }
    
    private fun updateFavoriteButtonState(routeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUser = authRepository.currentUser.first()
            if (currentUser != null) {
                val isFavorite = favoriteRepository.isFavorite(currentUser.id, routeId)
                withContext(Dispatchers.Main) {
                    if (isFavorite) {
                        binding.buttonFavoriteRoute.setImageResource(R.drawable.ic_bookmark_added)
                    } else {
                        binding.buttonFavoriteRoute.setImageResource(R.drawable.ic_bookmark_add)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.buttonFavoriteRoute.setImageResource(R.drawable.ic_bookmark_add)
                }
            }
        }
    }

    private fun toggleFavorite(routeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                // User not logged in, show login prompt
                withContext(Dispatchers.Main) {
                    showLoginPrompt()
                }
                return@launch
            }
            
            val isCurrentlyFavorite = favoriteRepository.isFavorite(currentUser.id, routeId)
            
            if (isCurrentlyFavorite) {
                favoriteRepository.removeFavoriteByRoute(currentUser.id, routeId)
            } else {
                val favorite = Favorite(
	                id = 0, // Will be auto-generated by database
	                userId = currentUser.id,
	                routeId = routeId,
	                createdAt = SimpleDateFormat(
		                "yyyy-MM-dd HH:mm:ss",
		                Locale.getDefault()
	                ).format(Date()),
	                updatedAt = SimpleDateFormat(
		                "yyyy-MM-dd HH:mm:ss",
		                Locale.getDefault()
	                ).format(Date())
                )
                favoriteRepository.addFavorite(favorite)
            }
            
            // Update button state
            updateFavoriteButtonState(routeId)
        }
    }
    
    private fun showLoginPrompt() {
        AlertDialog.Builder(requireContext())
            .setTitle("Login Required")
            .setMessage("You need to be logged in to save routes. Would you like to log in or create an account?")
            .setPositiveButton("Log In") { dialog, _ ->
                findNavController().navigate(R.id.action_home_to_login)
                dialog.dismiss()
            }
            .setNegativeButton("Sign Up") { dialog, _ ->
                findNavController().navigate(R.id.action_login_to_signup)
                dialog.dismiss()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun showAddBaguioRouteDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("🏔️ Add Baguio Jeepney Route")
            .setMessage("Help expand our Baguio route database!\n\nCommon Baguio routes:\n• Dangwa - La Trinidad\n• Session Road Loop\n• Mines View - Gibraltar\n• Slaughter - Ambuklao\n\nContact us to add new routes.")
            .setPositiveButton("Suggest Route") { dialog, _ ->
                // Here you can implement route suggestion functionality
                showRouteSuggestionForm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRouteSuggestionForm() {
        // Simple route suggestion form
        AlertDialog.Builder(requireContext())
            .setTitle("Suggest Baguio Route")
            .setMessage("Route suggestion feature coming soon!\n\nFor now, you can:\n1. Take note of the route details\n2. Contact the app developers\n3. Help us map Baguio's jeepney network!")
            .setPositiveButton("Got it", null)
            .show()
    }
    
    private fun resetDatabaseForDebug() {
        AlertDialog.Builder(requireContext())
            .setTitle("Debug: Reset Database")
            .setMessage("This will clear and reseed the database. Use this if routes are not working properly.")
            .setPositiveButton("Reset") { dialog, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val database = DyipQrDatabase.getInstance(requireContext())
                    database.routeDao().clearAll()
                    database.terminalDao().clearAll()
                    
                    // Reseed database
                    DatabaseSeeder(requireContext()).seed(database)
                    
                    // Reload routes
                    displayRoutesFromDb()
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Database reset complete", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionRequestCode
        )
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    
                    // Check if user is in Baguio area (within ~50km)
                    val distance = calculateDistance(currentLatLng, baguioCenter)
                    if (distance < 50.0) { // Within 50km of Baguio
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } else {
                        // User is far from Baguio, show Baguio center
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(baguioCenter, 13f))
                        showBaguioWelcomeMessage()
                    }
                }
            }
        }
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
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

    private fun showBaguioWelcomeMessage() {
        AlertDialog.Builder(requireContext())
            .setTitle("Welcome to Baguio Jeepney Routes!")
            .setMessage("It looks like you're not in Baguio yet. This app shows jeepney routes in Baguio City, Philippines.\n\nWhen you arrive in Baguio, the map will automatically show routes near you!")
            .setPositiveButton("Explore Baguio Routes") { dialog, _ ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(baguioCenter, 13f))
                dialog.dismiss()
            }
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
