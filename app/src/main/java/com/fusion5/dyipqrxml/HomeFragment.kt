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
import com.fusion5.dyipqrxml.data.model.Terminal
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
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.PatternItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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
    
    private var isShowingFavorites = false
    
    private var currentUserLocation: LatLng? = null
    private var currentLocationMarker: Marker? = null
    
    private val nearestRoutes = mutableListOf<RouteWithDistance>()
    private var isShowingNearestRoutes = false
    data class RouteWithDistance(val route: RouteWithTerminals, val distance: Double)
    
    private var walkingPathPolyline: Polyline? = null
    
    private val routeColorCache = mutableMapOf<String, Int>()
    
    private val httpClient = OkHttpClient()
    
    private var lastToastTime = 0L
    private val toastDebounceDelay = 1000L
    
    private fun showDebouncedToast(message: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastToastTime > toastDebounceDelay) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            lastToastTime = currentTime
        }
    }

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
        
        // Init map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // Init repositories
        authRepository = ServiceLocator.provideAuthRepository(requireContext())
        favoriteRepository = ServiceLocator.provideFavoriteRepository(requireContext())
        routeRepository = ServiceLocator.provideRouteRepository(requireContext())
        
        setupClickListeners()
        setupSearch()
        checkLocationPermission()
        
        // Check if we have a scanned terminal ID from QR scanning
        val scannedTerminalId = arguments?.getLong("scannedTerminalId", -1L) ?: -1L
        if (scannedTerminalId != -1L) {
            // Auto-select routes for this terminal
            viewLifecycleOwner.lifecycleScope.launch {
                autoSelectRoutesForTerminal(scannedTerminalId)
            }
        }
        
//        view.postDelayed({
//            if (isAdded && _binding != null) {
//                binding.cardWelcome.visibility = View.GONE
//            }
//        }, 3000)
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("HomeFragment", "GoogleMap is ready")
        googleMap = map
        
        // basic map features
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.setOnMarkerClickListener(this)
        
        // Enable the blue dot for current location
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
        
        googleMap.setOnMapClickListener {
            clearRouteSelection()
        }
        
        // Add location button click listener to zoom to user location
        googleMap.setOnMyLocationButtonClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
            true // Return true to indicate that the click was handled
        }
        
        try {
            googleMap.setMapStyle(null) // Use default style
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting map style", e)
        }
        
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(baguioCenter, 13f))
        Log.d("HomeFragment", "Camera moved to Baguio center")
        
        displayRoutesFromDb()
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
        
        // Check if map is loaded
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

                // Clear existing markers and don't display new ones by default
                markers.forEach { it.remove() }
                markers.clear()
            }
        }
    }
    
    private fun toggleFavoritesView() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isShowingFavorites) {
                // Check if user is logged in before showing favorites
                val currentUser = authRepository.currentUser.first()
                if (currentUser == null) {
                    // Show login prompt
                    withContext(Dispatchers.Main) {
                        showLoginPromptForFavorites()
                    }
                    return@launch
                }
            }
            
            isShowingFavorites = !isShowingFavorites
            
            // Update FAB appearance
            withContext(Dispatchers.Main) {
                if (isShowingFavorites) {
                    binding.fabFavoritesToggle.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.favorite_active)
                    showDebouncedToast("Showing favorite routes only")
                } else {
                    binding.fabFavoritesToggle.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.secondary)
                    showDebouncedToast("Showing all routes")
                }
                
                // Refresh the displayed routes
                displayRoutesFromDb()
            }
        }
    }
    
    private fun showLoginPromptForFavorites() {
        AlertDialog.Builder(requireContext())
            .setTitle("Login Required")
            .setMessage("You need to be logged in to view your favorite routes. Would you like to log in or create an account?")
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
    
    private fun toggleNearestRoutesView() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isShowingNearestRoutes) {
                // Check if we have current location
                if (currentUserLocation == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Location not available. Please enable location services.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
            }
            
            isShowingNearestRoutes = !isShowingNearestRoutes
            
            // Update FAB appearance
            withContext(Dispatchers.Main) {
                if (isShowingNearestRoutes) {
                    binding.fabNearestRoutes.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.favorite_active)
                    showDebouncedToast("Showing nearest routes only")
                    
                    // Show nearest routes panel
                    binding.cardNearestRoutes.visibility = View.VISIBLE
                    
                    // If we have location data, then recalculate nearest routes
                    if (currentUserLocation != null) {
                        calculateNearestRoutes()
                    } else {
                        binding.textNearestRoutes.text = "Finding routes near you..."
                    }
                    
                    // Filter to show only nearest routes
                    filterToNearestRoutes()
                } else {
                    binding.fabNearestRoutes.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.secondary)
                    showDebouncedToast("Showing all routes")
                    
                    binding.cardNearestRoutes.visibility = View.GONE
                    
                    // Show all routes
                    displayRoutesFromDb()
                }
            }
        }
    }
    
    private fun filterToNearestRoutes() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // Clear current view
                markers.forEach { it.remove() }
                markers.clear()
                routePolylines.forEach { it.remove() }
                routePolylines.clear()
                
                val routesToShow = nearestRoutes.take(2).map { it.route }
                
                // Create polylines for nearest routes
                routesToShow.forEach { route ->
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
                                    
                                    // Create polyline for nearest route
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
                                    
                                    Log.d("HomeFragment", "Added nearest route polyline: ${route.route.routeCode}")
                                }
                            }
                            
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error loading GeoJSON for nearest route ${route.route.routeCode}", e)
                        }
                    }
                }
                
                // Set up polyline click listener for nearest routes
                setupPolylineClickListener()
                
                // Auto-select the route if only one nearest route remains
                if (routesToShow.size == 1) {
                    Log.d("HomeFragment", "Auto-selecting single nearest route: ${routesToShow[0].route.routeCode}")
                    selectRoute(routesToShow[0], null)
                } else if (routesToShow.isNotEmpty()) {
                    // Zoom to show all nearest routes
                    val builder = LatLngBounds.Builder()
                    routesToShow.forEach { route ->
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
                                            // Add first and last points to bounds
                                            val firstCoord = coordinates.getJSONArray(0)
                                            val lastCoord = coordinates.getJSONArray(coordinates.length() - 1)
                                            builder.include(LatLng(firstCoord.getDouble(1), firstCoord.getDouble(0)))
                                            builder.include(LatLng(lastCoord.getDouble(1), lastCoord.getDouble(0)))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("HomeFragment", "Error calculating bounds for route ${route.route.routeCode}", e)
                            }
                        }
                    }
                    
                    try {
                        val bounds = builder.build()
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error zooming to nearest routes", e)
                        // Fallback: zoom to user location
                        currentUserLocation?.let { location ->
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14f))
                        }
                    }
                }
            }
        }
    }
    
    private fun displayRoutesFromDb() {
        displayRoutesJob?.cancel()
        val database = DyipQrDatabase.getInstance(requireContext())
        val routeDao = database.routeDao()
        
        displayRoutesJob = viewLifecycleOwner.lifecycleScope.launch {
            val routesFlow = if (isShowingFavorites) {
                // Get current user and then favorite routes
                val currentUser = authRepository.currentUser.first()
                if (currentUser != null) {
                    routeDao.observeFavoriteRoutesWithTerminals(currentUser.id)
                } else {
                    // If no user is logged in, show empty list
                    flow { emit(emptyList<RouteWithTerminals>()) }
                }
            } else {
                routeDao.observeAllRoutesWithTerminals()
            }
            
            routesFlow.collect { routes ->
                Log.d("HomeFragment", "Displaying ${routes.size} routes")
                Log.d("HomeFragment", "Route order: ${routes.map { it.route.routeCode }}")
                
                // Clear existing layers, polylines and labels
                routeLayers.forEach { it.removeLayerFromMap() }
                routeLayers.clear()
                routePolylines.forEach { it.remove() }
                routePolylines.clear()
                routeLabelMarkers.forEach { it.remove() }
                routeLabelMarkers.clear()

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
                
                setupPolylineClickListener()
                
                // Auto-select the route if only one result remains after filtering
                if (routes.size == 1) {
                    Log.d("HomeFragment", "=== AUTO-SELECTION TRIGGERED ===")
                    Log.d("HomeFragment", "Auto-selecting single filtered route: ${routes[0].route.routeCode}")
                    Log.d("HomeFragment", "Current filter state: favorites=$isShowingFavorites, nearest=$isShowingNearestRoutes")

                    // Add small delay to prevent flickering during route display
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(100)
                        withContext(Dispatchers.Main) {
                            selectRoute(routes[0], null)
                        }
                    }
                }
                
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
                selectRoute(route, null)
            }
        }
        
        googleMap.setOnMapClickListener {
            clearRouteSelection()
        }
    }
    
    private fun addDebugRouteTestButton() {
//        binding.fabCurrentLocation.setOnLongClickListener {
//            testRouteClickability()
//            true
//        }
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
        return routeColorCache.getOrPut(name) {
            val hash = name.hashCode()
            val colorIndex = (hash and 0x7FFFFFFF) % 6
            when (colorIndex) {
                0 -> Color.BLUE
                1 -> Color.RED
                2 -> Color.GREEN
                3 -> Color.MAGENTA
                4 -> Color.CYAN
                5 -> Color.YELLOW
                else -> Color.BLACK
            }
        }
    }
    
    private fun getHueFromColor(color: Int): Float {
        return when (color) {
            Color.BLUE -> BitmapDescriptorFactory.HUE_BLUE
            Color.RED -> BitmapDescriptorFactory.HUE_RED
            Color.GREEN -> BitmapDescriptorFactory.HUE_GREEN
            Color.MAGENTA -> BitmapDescriptorFactory.HUE_MAGENTA
            Color.CYAN -> BitmapDescriptorFactory.HUE_CYAN
            Color.YELLOW -> BitmapDescriptorFactory.HUE_YELLOW
            else -> BitmapDescriptorFactory.HUE_ORANGE
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
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                                    .alpha(0.8f)
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
        
        clearRouteSelection()
        
        routePolylines.forEach { polyline ->
            val polylineRoute = polyline.tag as? RouteWithTerminals
            if (polylineRoute?.route?.id == route.route.id) {
                // Highlight selected route with increased width but keep original color
                polyline.width = 16f
            } else {
                // Reset other routes
                polyline.width = 12f
            }
        }
        
        showTerminalsForRoute(route)
        showRouteInfo(route)
    }
    
    private fun clearRouteSelection() {
        // Clear route highlighting for polylines; only reset width, keep original colors
        routePolylines.forEach { polyline ->
            polyline.width = 12f
        }
        
        markers.forEach { it.remove() }
        markers.clear()
        
        clearWalkingPath()
        
        // Hide route info
        binding.cardRouteInfo.visibility = View.GONE
        binding.layoutDistanceToTerminal.visibility = View.GONE
        currentDisplayedRoute = null
    }
    
    private fun showTerminalsForRoute(route: RouteWithTerminals) {
        Log.d("HomeFragment", "=== SHOWING TERMINALS FOR ROUTE: ${route.route.routeCode} ===")
        
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // Clear ALL markers completely but preserve walking path
                markers.forEach { it.remove() }
                markers.clear()
                
                val terminalPositions = extractTerminalPositionsFromRoute(route)
                
                val routeColor = getColorForRoute(route.route.routeCode)
                
                // Add color to terminal marker
                terminalPositions.start?.let { position ->
                    val routeParts = route.route.routeCode.split("-")
                    val startTerminalName = if (routeParts.size > 1) routeParts[0].trim() else "Start"
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title("$startTerminalName")
                            .snippet("Terminal for ${route.route.routeCode}")
                            .icon(BitmapDescriptorFactory.defaultMarker(getHueFromColor(routeColor)))
                    )
                    marker?.let { markers.add(it) }
                    Log.d("HomeFragment", "Added start terminal marker for ${route.route.routeCode} at: $position")
                }
                
                // Add color to terminal marker
                terminalPositions.end?.let { position ->
                    val routeParts = route.route.routeCode.split("-")
                    val endTerminalName = if (routeParts.size > 1) routeParts[1].trim() else "End"
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title("$endTerminalName")
                            .snippet("Terminal for ${route.route.routeCode}")
                            .icon(BitmapDescriptorFactory.defaultMarker(getHueFromColor(routeColor)))
                    )
                    marker?.let { markers.add(it) }
                    Log.d("HomeFragment", "Added end terminal marker for ${route.route.routeCode} at: $position")
                }
                
                // Zoom to show the entire route with terminals, ensuring majority of route is visible
                val routeBounds = calculateRouteBounds(route)
                if (routeBounds != null) {
                    // Add padding to ensure the route is fully visible
                    val padding = 100 // Padding in pixels
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(routeBounds, padding))
                    Log.d("HomeFragment", "Zoomed to show entire route for ${route.route.routeCode} with padding: $padding")
                } else if (markers.isNotEmpty()) {
                    // Fallback: zoom to terminals if route bounds calculation fails
                    val builder = LatLngBounds.Builder()
                    markers.forEach { marker -> builder.include(marker.position) }
                    val bounds = builder.build()
                    
                    val padding = 300
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    Log.d("HomeFragment", "Zoomed to show terminals for ${route.route.routeCode} with maximum padding: $padding")
                } else {
                    Log.w("HomeFragment", "No markers or route bounds to zoom to for ${route.route.routeCode}")
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
                    // First coordinate (termina 1)
                    val startCoord = coordinates.getJSONArray(0)
                    val startLng = startCoord.getDouble(0)
                    val startLat = startCoord.getDouble(1)
                    val startPosition = LatLng(startLat, startLng)
                    
                    // Last coordinate (terminal 2)
                    val lastIndex = coordinates.length() - 1
                    val endCoord = coordinates.getJSONArray(lastIndex)
                    val endLng = endCoord.getDouble(0)
                    val endLat = endCoord.getDouble(1)
                    val endPosition = LatLng(endLat, endLng)
                    
                    // Burnham Park coordinates (approximate center)
                    val plazaCenter = LatLng(16.4125, 120.5975)
                    
                    // Calculate which terminal is closer to Plaza
                    val startDistance = calculateDistance(startPosition, plazaCenter)
                    val endDistance = calculateDistance(endPosition, plazaCenter)
                    
                    // If the route name contains "Plaza", ensure the Plaza terminal is identified correctly
                    val routeName = route.route.routeCode
                    if (routeName.contains("Plaza", ignoreCase = true)) {
                        // For routes ending at Plaza, the Plaza terminal should be the end terminal
	                    return if (endDistance < startDistance) {
		                    // End is closer to Plaza, so it's likely the Plaza terminal
		                    TerminalPositions(
			                    start = startPosition,
			                    end = endPosition
		                    )
	                    } else {
		                    // Start is closer to Plaza, so swap positions
		                    TerminalPositions(
			                    start = endPosition,
			                    end = startPosition
		                    )
	                    }
                    } else {
                        // For other routes, use the original order
                        return TerminalPositions(
                            start = startPosition,
                            end = endPosition
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error extracting terminal positions from route GeoJSON: ${e.message}")
        }
        
        return TerminalPositions(null, null)
    }
    
    data class TerminalPositions(val start: LatLng?, val end: LatLng?)

    private fun calculateRouteBounds(route: RouteWithTerminals): LatLngBounds? {
        return try {
            route.route.routeGeoJson?.let { geoJson ->
                val jsonObject = JSONObject(geoJson)
                val features = jsonObject.getJSONArray("features")
                
                if (features.length() > 0) {
                    val feature = features.getJSONObject(0)
                    val geometry = feature.getJSONObject("geometry")
                    
                    if (geometry.getString("type") == "LineString") {
                        val coordinates = geometry.getJSONArray("coordinates")
                        val builder = LatLngBounds.Builder()
                        
                        // Include all route points in bounds
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            builder.include(LatLng(lat, lng))
                        }
                        
                        // Include user location if available
                        currentUserLocation?.let { userLocation ->
                            builder.include(userLocation)
                        }
                        
                        return builder.build()
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error calculating route bounds for ${route.route.routeCode}", e)
            null
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        return false
    }

    private fun setupClickListeners() {
//        binding.fabCurrentLocation.setOnClickListener {
//            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//                getCurrentLocation()
//            } else {
//                requestLocationPermission()
//            }
//        }

        binding.fabAddRoute.setOnClickListener {
            showAddBaguioRouteDialog()
        }
        
        // Add debug button to reset database (for testing)
//        binding.fabCurrentLocation.setOnLongClickListener {
//            resetDatabaseForDebug()
//            true
//        }

//        binding.buttonViewRoutes.setOnClickListener {
//            findNavController().navigate(R.id.action_home_to_terminals)
//        }
        
        binding.buttonFavoriteRoute.setOnClickListener {
            currentDisplayedRoute?.let { route ->
                toggleFavorite(route.route.id)
            }
        }
        
        // Favorites toggle button
        binding.fabFavoritesToggle.setOnClickListener {
            toggleFavoritesView()
        }
        
        // Nearest routes toggle button
        binding.fabNearestRoutes.setOnClickListener {
            toggleNearestRoutesView()
        }
    }

    private fun setupSearch() {
        val activity = requireActivity()
        val searchEditText = activity.findViewById<android.widget.EditText>(R.id.editSearch)
        searchEditText?.addTextChangedListener(object : TextWatcher {
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
            // Reset to default view - show all routes or favorites based on toggle
            withContext(Dispatchers.Main) {
                // Clear everything and show appropriate routes
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
        
        // Search Routes only (no terminals) - combine search with favorites filter if active
        val searchFlow = if (isShowingFavorites) {
            val currentUser = authRepository.currentUser.first()
            if (currentUser != null) {
                routeDao.searchFavoriteRoutesWithTerminals(currentUser.id, query)
            } else {
                routeDao.searchRoutesWithTerminals(query)
            }
        } else {
            routeDao.searchRoutesWithTerminals(query)
        }
        
        searchFlow.collect { routes ->
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
                
                // Auto-select the route if only one result remains
                if (routes.size == 1) {
                    Log.d("HomeFragment", "Auto-selecting single search result: ${routes[0].route.routeCode}")
                    selectRoute(routes[0], null)
                } else if (routes.isNotEmpty()) {
                    // Zoom to first result if multiple results
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
        binding.textRouteFare.text = "₱${"%.2f".format(route.route.fare)}"
        
        // Calculate and display route length with estimated time
        val routeLength = calculateRouteLength(route)
        val routeLengthText = if (routeLength < 1.0) {
            "${"%.0f".format(routeLength * 1000)}m"
        } else {
            "${"%.1f".format(routeLength)}km"
        }
        
        // Calculate estimated walking time (assuming 5 km/h walking speed)
        val estimatedWalkingTimeMinutes = calculateWalkingTime(routeLength)
        val routeLengthWithTime = "$routeLengthText / $estimatedWalkingTimeMinutes min"
        binding.textRouteLength.text = routeLengthWithTime
        
        // Calculate and display distance to nearest terminal if user location is available
        if (currentUserLocation != null) {
            val (nearestTerminal, distance) = findNearestTerminal(route)
            if (distance != null && nearestTerminal != null) {
                val distanceText = if (distance < 1.0) {
                    "${"%.0f".format(distance * 1000)}m"
                } else {
                    "${"%.1f".format(distance)}km"
                }
                binding.textDistanceToTerminal.text = "Walk $distanceText to ${nearestTerminal.name}"
                binding.layoutDistanceToTerminal.visibility = View.VISIBLE
                
                // Draw walking path
                val terminalLocation = LatLng(nearestTerminal.latitude, nearestTerminal.longitude)
                drawWalkingPath(currentUserLocation!!, terminalLocation)
            } else {
                binding.layoutDistanceToTerminal.visibility = View.GONE
                clearWalkingPath()
            }
        } else {
            binding.layoutDistanceToTerminal.visibility = View.GONE
            clearWalkingPath()
        }
        
        Log.d("HomeFragment", "Route info displayed: ${route.route.routeCode}, ${route.startTerminal.name} → ${route.endTerminal.name}")
        
        // Update favorite button state
        updateFavoriteButtonState(route.route.id)
    }
    
    private fun calculateRouteLength(route: RouteWithTerminals): Double {
        return try {
            route.route.routeGeoJson?.let { geoJson ->
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
                            
                            val point1 = LatLng(lat1, lng1)
                            val point2 = LatLng(lat2, lng2)
                            
                            totalLength += calculateDistance(point1, point2)
                        }
                        
                        return totalLength
                    }
                }
            }
            0.0
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error calculating route length for ${route.route.routeCode}", e)
            0.0
        }
    }
    
    private fun calculateWalkingTime(distanceKm: Double): Int {
        // Assuming average walking speed of 5 km/h
        val walkingSpeedKmPerHour = 5.0
        val timeHours = distanceKm / walkingSpeedKmPerHour
        val timeMinutes = (timeHours * 60).toInt()
        
        // Ensure minimum of 1 minute for very short distances
        return maxOf(1, timeMinutes)
    }
    
    private fun findNearestTerminal(route: RouteWithTerminals): Pair<Terminal?, Double?> {
        return try {
            val terminalPositions = extractTerminalPositionsFromRoute(route)
            var nearestTerminal: Terminal? = null
            var minDistance = Double.MAX_VALUE
            
            // Check start terminal
            terminalPositions.start?.let { startPosition ->
                val distance = calculateDistance(currentUserLocation!!, startPosition)
                if (distance < minDistance) {
                    minDistance = distance
                    nearestTerminal = Terminal(
                        id = route.startTerminal.id,
                        name = route.startTerminal.name,
                        description = route.startTerminal.description,
                        latitude = startPosition.latitude,
                        longitude = startPosition.longitude,
                        createdAt = route.startTerminal.createdAt,
                        updatedAt = route.startTerminal.updatedAt
                    )
                }
            }
            
            // Check end terminal
            terminalPositions.end?.let { endPosition ->
                val distance = calculateDistance(currentUserLocation!!, endPosition)
                if (distance < minDistance) {
                    minDistance = distance
                    nearestTerminal = Terminal(
                        id = route.endTerminal.id,
                        name = route.endTerminal.name,
                        description = route.endTerminal.description,
                        latitude = endPosition.latitude,
                        longitude = endPosition.longitude,
                        createdAt = route.endTerminal.createdAt,
                        updatedAt = route.endTerminal.updatedAt
                    )
                }
            }
            
            if (nearestTerminal != null && minDistance != Double.MAX_VALUE) {
                Pair(nearestTerminal, minDistance)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error finding nearest terminal for route ${route.route.routeCode}", e)
            Pair(null, null)
        }
    }
    
    private fun drawWalkingPath(userLocation: LatLng, terminalLocation: LatLng) {
        clearWalkingPath()
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Get walking route from Google Routes API
                val walkingRoute = getWalkingRoute(userLocation, terminalLocation)
                withContext(Dispatchers.Main) {
                    if (walkingRoute != null) {
                        // Always use dashed pattern for walking routes to distinguish them from jeepney routes
                        val pattern = listOf<PatternItem>(
                            Dash(20f),
                            Gap(10f)
                        )
                        
                        walkingPathPolyline = googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(walkingRoute)
                                .color(Color.BLUE)
                                .width(8f)
                                .pattern(pattern)
                                .zIndex(10f) // Higher z-index to appear above other routes
                        )
                        
                        Log.d("HomeFragment", "Walking path drawn with ${walkingRoute.size} points")
                    } else {
                        Log.w("HomeFragment", "No walking route available")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error drawing walking path", e)
                // Final fallback to straight dashed line
                withContext(Dispatchers.Main) {
                    val pattern = listOf<PatternItem>(
                        Dash(20f),
                        Gap(10f)
                    )
                    
                    walkingPathPolyline = googleMap.addPolyline(
                        PolylineOptions()
                            .add(userLocation, terminalLocation)
                            .color(Color.BLUE)
                            .width(8f)
                            .pattern(pattern)
                            .zIndex(10f)
                    )
                    
                    Log.d("HomeFragment", "Error fallback: Straight walking path drawn")
                }
            }
        }
    }
    
    private fun getRoutesApiWalkingPolyline(origin: LatLng, destination: LatLng): List<LatLng>? {
        return try {
            val url = "https://routes.googleapis.com/directions/v2:computeRoutes"

            val jsonBody = """
{
  "origin": {
    "location": {"latLng": {"latitude": ${origin.latitude}, "longitude": ${origin.longitude}}}
  },
  "destination": {
    "location": {"latLng": {"latitude": ${destination.latitude}, "longitude": ${destination.longitude}}}
  },
  "travelMode": "WALK",
  "polylineQuality": "HIGH_QUALITY",
  "polylineEncoding": "ENCODED_POLYLINE"
}
""".trimIndent()

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", "AIzaSyDHJBpnq-46VcsC0vBdvzUbu4ZcN8nrnEY")
                .addHeader("X-Goog-FieldMask", "routes.polyline")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return null

            val polylineObj = routes.getJSONObject(0).getJSONObject("polyline")
            val encoded = polylineObj.getString("encodedPolyline")

            decodePolyline(encoded)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Routes API walking error", e)
            null
        }
    }

    private suspend fun getWalkingRoute(origin: LatLng, destination: LatLng): List<LatLng>? {
        return withContext(Dispatchers.IO) {
            try {
                // Try to get actual walking route from Google Routes API
                val routesApi = getRoutesApiWalkingPolyline(origin, destination)
                if (!routesApi.isNullOrEmpty()) {
                    Log.d("HomeFragment", "Using Routes API walking route with ${routesApi.size} points")
                    return@withContext routesApi
                }

                val walkingRoute = getDirectionsWalkingRoute(origin, destination)
                if (!walkingRoute.isNullOrEmpty()) {
                    Log.d("HomeFragment", "Using Directions API walking route with ${walkingRoute.size} points")
                    return@withContext walkingRoute
                }

                // Directions API failed completely — use simulated fallback
                Log.w("HomeFragment", "Directions API returned no usable route, using simulated walking route")
                createSimulatedWalkingRoute(origin, destination)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error getting walking route", e)
                // Final fallback to simulated route
                createSimulatedWalkingRoute(origin, destination)
            }
        }
    }

    private fun getDirectionsWalkingRoute(origin: LatLng, destination: LatLng): List<LatLng>? {
        return try {
            Log.d("HomeFragment", "Calling Directions API for walking route from $origin to $destination")

            // Use classic Google Directions API which is more widely supported
            val originStr = "${origin.latitude},${origin.longitude}"
            val destinationStr = "${destination.latitude},${destination.longitude}"
            val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$originStr&destination=$destinationStr&mode=walking&key=AIzaSyDHJBpnq-46VcsC0vBdvzUbu4ZcN8nrnEY"

            Log.d("HomeFragment", "Directions API URL: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("HomeFragment", "Directions API response code: ${response.code}")

            if (response.isSuccessful && responseBody != null) {
                Log.d("HomeFragment", "Directions API response received")
                val jsonResponse = JSONObject(responseBody)

                if (jsonResponse.getString("status") == "OK") {
                    val routes = jsonResponse.getJSONArray("routes")
                    Log.d("HomeFragment", "Directions API returned ${routes.length()} routes")

                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        // Insert support for Google's overview polyline
                        if (route.has("overview_polyline")) {
                            val overview = route.getJSONObject("overview_polyline").getString("points")
                            val decodedOverview = decodePolyline(overview)
                            if (decodedOverview.isNotEmpty()) {
                                Log.d("HomeFragment", "Using overview polyline with ${decodedOverview.size} points")
                                return decodedOverview
                            }
                        }
                        val legs = route.getJSONArray("legs")

                        if (legs.length() > 0) {
                            val leg = legs.getJSONObject(0)
                            val steps = leg.getJSONArray("steps")
                            val path = mutableListOf<LatLng>()

                            Log.d("HomeFragment", "Processing ${steps.length()} steps")

                            for (i in 0 until steps.length()) {
                                val step = steps.getJSONObject(i)
                                val polyline = step.getJSONObject("polyline")
                                val points = polyline.getString("points")

                                // Decode polyline points for each step
                                val decodedPoints = decodePolyline(points)
                                path.addAll(decodedPoints)
                            }

                            Log.d("HomeFragment", "Successfully decoded walking route with ${path.size} points")
                            return path
                        } else {
                            Log.w("HomeFragment", "No legs in route")
                        }
                    } else {
                        Log.w("HomeFragment", "No routes returned from API")
                    }
                } else {
                    Log.w("HomeFragment", "Directions API status: ${jsonResponse.getString("status")}")
                }
            } else {
                Log.e("HomeFragment", "Directions API failed: ${response.code} - ${response.message}")
                if (responseBody != null) {
                    Log.e("HomeFragment", "Error response body: $responseBody")
                }
            }
            null
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error calling Directions API", e)
            null
        }
    }
    
    private fun createEnhancedWalkingRoute(origin: LatLng, destination: LatLng): List<LatLng> {
        val routePoints = mutableListOf<LatLng>()
        
        // Add origin point
        routePoints.add(origin)
        
        // Calculate distance to determine complexity of the route
        val distance = calculateDistance(origin, destination)
        val steps = maxOf(5, minOf(20, (distance * 100).toInt())) // More points for longer distances
        
        // Create a more realistic walking path with multiple curves
        for (i in 1 until steps) {
            val fraction = i.toDouble() / steps.toDouble()
            
            // Calculate base point along straight line
            val baseLat = origin.latitude + (destination.latitude - origin.latitude) * fraction
            val baseLng = origin.longitude + (destination.longitude - origin.longitude) * fraction
            
            // Add realistic curvature that simulates following roads
            val curveFactor = 0.0005 * sin(fraction * Math.PI * 2) // Multiple curves
            val curveOffsetLat = sin(fraction * Math.PI * 3) * curveFactor
            val curveOffsetLng = cos(fraction * Math.PI * 2) * curveFactor
            
            val curvedLat = baseLat + curveOffsetLat
            val curvedLng = baseLng + curveOffsetLng
            
            routePoints.add(LatLng(curvedLat, curvedLng))
        }
        
        // Add destination point
        routePoints.add(destination)
        
        return routePoints
    }
    
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            
            val point = LatLng(lat / 1E5, lng / 1E5)
            poly.add(point)
        }
        
        return poly
    }
    
    private fun createSimulatedWalkingRoute(origin: LatLng, destination: LatLng): List<LatLng> {
        val routePoints = mutableListOf<LatLng>()
        
        // Add origin point
        routePoints.add(origin)
        
        // Calculate intermediate points to create a more natural walking path
        val steps = 10 // Number of intermediate points
        for (i in 1 until steps) {
            val fraction = i.toDouble() / steps.toDouble()
            
            // Calculate base point along straight line
            val baseLat = origin.latitude + (destination.latitude - origin.latitude) * fraction
            val baseLng = origin.longitude + (destination.longitude - origin.longitude) * fraction
            
            // Add slight curvature to simulate walking paths
            val curveFactor = 0.0002 // Small curvature
            val curveOffset = sin(fraction * Math.PI) * curveFactor
            
            val curvedLat = baseLat + curveOffset
            val curvedLng = baseLng + curveOffset
            
            routePoints.add(LatLng(curvedLat, curvedLng))
        }
        
        // Add destination point
        routePoints.add(destination)
        
        return routePoints
    }
    
    private fun clearWalkingPath() {
        walkingPathPolyline?.remove()
        walkingPathPolyline = null
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
                    currentUserLocation = currentLatLng
                    
                    // Check if user is in Baguio area (within ~50km)
                    val distance = calculateDistance(currentLatLng, baguioCenter)
                    if (distance < 50.0) { // Within 50km of Baguio
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        
                        // Calculate nearest routes
                        calculateNearestRoutes()
                    } else {
                        // User is far from Baguio, show Baguio center
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(baguioCenter, 13f))
                        showBaguioWelcomeMessage()
                    }
                }
            }
        }
    }
    
    private fun calculateNearestRoutes() {
        viewLifecycleOwner.lifecycleScope.launch {
            val database = DyipQrDatabase.getInstance(requireContext())
            val routeDao = database.routeDao()
            
            routeDao.observeAllRoutesWithTerminals().collect { routes ->
                nearestRoutes.clear()
                
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
                                    var minDistance = Double.MAX_VALUE
                                    
                                    // Calculate minimum distance from user to any point on the route
                                    for (i in 0 until coordinates.length()) {
                                        val coord = coordinates.getJSONArray(i)
                                        val lng = coord.getDouble(0)
                                        val lat = coord.getDouble(1)
                                        val routePoint = LatLng(lat, lng)
                                        
                                        val distance = calculateDistance(currentUserLocation!!, routePoint)
                                        if (distance < minDistance) {
                                            minDistance = distance
                                        }
                                    }
                                    
                                    nearestRoutes.add(RouteWithDistance(route, minDistance))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error calculating distance for route ${route.route.routeCode}", e)
                        }
                    }
                }
                
                // Sort by distance (nearest first)
                nearestRoutes.sortBy { it.distance }
                
                // Update UI with nearest routes info
                updateNearestRoutesDisplay()
            }
        }
    }
    
    private fun updateNearestRoutesDisplay() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                if (nearestRoutes.isNotEmpty() && isShowingNearestRoutes) {
                    Log.d("HomeFragment", "Found ${nearestRoutes.size} routes, nearest: ${nearestRoutes[0].route.route.routeCode} (${"%.2f".format(nearestRoutes[0].distance)} km)")
                    
                    // Show nearest routes panel only when toggle is active
                    binding.cardNearestRoutes.visibility = View.VISIBLE
                    
                    // Build display text for nearest routes
                    val nearestRoutesText = buildString {
                        append("Routes near you:\n")
                        nearestRoutes.take(3).forEachIndexed { index, routeWithDistance ->
                            val distanceText = if (routeWithDistance.distance < 1.0) {
                                "${"%.0f".format(routeWithDistance.distance * 1000)}m"
                            } else {
                                "${"%.1f".format(routeWithDistance.distance)}km"
                            }
                            append("${index + 1}. ${routeWithDistance.route.route.routeCode} ($distanceText)\n")
                        }
                    }
                    
                    binding.textNearestRoutes.text = nearestRoutesText
                    
                    // Show toast for the nearest route if very close
                    if (nearestRoutes[0].distance < 0.5) { // Within 500m
                        Toast.makeText(
                            requireContext(),
                            "🚌 Nearest route: ${nearestRoutes[0].route.route.routeCode} (${"%.0f".format(nearestRoutes[0].distance * 1000)}m away)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (nearestRoutes.isEmpty() && isShowingNearestRoutes) {
                    // Show "Finding routes..." message when toggle is active but no routes calculated yet
                    binding.cardNearestRoutes.visibility = View.VISIBLE
                    binding.textNearestRoutes.text = "Finding routes near you..."
                } else {
                    // Hide panel if toggle is inactive
                    binding.cardNearestRoutes.visibility = View.GONE
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

    private suspend fun autoSelectRoutesForTerminal(terminalId: Long) {
        try {
            // Find all routes that contain this terminal
            val routes = routeRepository.findRoutesByTerminalId(terminalId)
            
            if (routes.isNotEmpty()) {
                // Auto-select the first route
                val firstRoute = routes[0]
                withContext(Dispatchers.Main) {
                    // Find the corresponding RouteWithTerminals object
                    val database = DyipQrDatabase.getInstance(requireContext())
                    val routeDao = database.routeDao()
                    viewLifecycleOwner.lifecycleScope.launch {
                        val routeWithTerminals = routeDao.getRouteWithTerminalsById(firstRoute.id)
                        if (routeWithTerminals != null) {
                            selectRoute(routeWithTerminals, null)
                            showDebouncedToast("Auto-selected route: ${firstRoute.routeCode}")
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    showDebouncedToast("No routes found for this terminal")
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error auto-selecting routes for terminal $terminalId", e)
            withContext(Dispatchers.Main) {
                showDebouncedToast("Error finding routes for this terminal")
            }
        }
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
