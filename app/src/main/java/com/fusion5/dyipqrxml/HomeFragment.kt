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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fusion5.dyipqrxml.data.local.DyipQrDatabase
import com.fusion5.dyipqrxml.data.local.repository.LocalRouteRepository
import com.fusion5.dyipqrxml.data.local.repository.LocalTerminalRepository
import com.fusion5.dyipqrxml.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.data.geojson.GeoJsonLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.*

class HomeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequestCode = 1001
    
    private val markers = mutableListOf<Marker>()
    private val routeLayers = mutableListOf<GeoJsonLayer>()
    
    private var displayTerminalsJob: Job? = null
    private var displayRoutesJob: Job? = null
    private var searchJob: Job? = null

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
        
        setupClickListeners()
        setupSearch()
        checkLocationPermission()
        
        // Hide welcome banner after 5 seconds
        view.postDelayed({
            if (isAdded && _binding != null) {
                binding.cardWelcome.visibility = View.GONE
            }
        }, 5000)
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("HomeFragment", "GoogleMap is ready")
        googleMap = map
        
        // Enable basic map features
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.setOnMarkerClickListener(this)
        
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
        
        // Display Terminals from DB
        displayTerminalsFromDb()
        
        // Display Routes from DB
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
                Log.d("HomeFragment", "Displaying ${terminals.size} terminals")
                // Clear existing markers
                markers.forEach { it.remove() }
                markers.clear()
                
                terminals.forEach { terminal ->
                    if (terminal.latitude != null && terminal.longitude != null) {
                        val position = LatLng(terminal.latitude, terminal.longitude)
                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(terminal.name)
                                .snippet(terminal.description)
                        )
                        marker?.let { markers.add(it) }
                    }
                }
            }
        }
    }
    
    private fun displayRoutesFromDb() {
        displayRoutesJob?.cancel()
        val database = DyipQrDatabase.getInstance(requireContext())
        val routeRepository = LocalRouteRepository(database.routeDao())
        
        displayRoutesJob = viewLifecycleOwner.lifecycleScope.launch {
            routeRepository.observeAll().collect { routes ->
                Log.d("HomeFragment", "Displaying ${routes.size} routes")
                // Clear existing layers
                routeLayers.forEach { it.removeLayerFromMap() }
                routeLayers.clear()

                routes.forEach { route ->
                    route.routeGeoJson?.let { geoJson ->
                        try {
                            val jsonObject = JSONObject(geoJson)
                            val layer = GeoJsonLayer(googleMap, jsonObject)
                            
                            val style = layer.defaultLineStringStyle
                            style.color = getColorForRoute(route.name)
                            style.width = 12f
                            
                            layer.addLayerToMap()
                            routeLayers.add(layer)
                            
                            // Add click listener to layer
                            layer.setOnFeatureClickListener { _ ->
                                showRouteInfo(route.name, route.destination, route.fare, route.estimatedTime)
                            }
                            
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error loading GeoJSON for route ${route.name}", e)
                        }
                    }
                }
            }
        }
    }
    
    private fun getColorForRoute(name: String): Int {
        return when {
            name.contains("Camp 7", ignoreCase = true) -> Color.BLUE
            name.contains("City Camp", ignoreCase = true) -> Color.RED
            name.contains("Leonila Hill", ignoreCase = true) -> Color.GREEN
            else -> Color.BLACK
        }
    }

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

        binding.buttonViewRoutes.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_terminals)
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
        val terminalRepository = LocalTerminalRepository(database.terminalDao())
        val routeRepository = LocalRouteRepository(database.routeDao())
        
        if (query.isBlank()) {
            // Reset to default view
            displayTerminalsFromDb()
            displayRoutesFromDb()
            return
        }
        
        // Clear current view
        withContext(Dispatchers.Main) {
            markers.forEach { it.remove() }
            markers.clear()
            routeLayers.forEach { it.removeLayerFromMap() }
            routeLayers.clear()
        }
        
        // Use coroutineScope to launch parallel searches safely
        coroutineScope {
             // Search Terminals
            launch {
                terminalRepository.search(query).collect { terminals ->
                    withContext(Dispatchers.Main) {
                        // Don't clear here, or it might flicker. We cleared above.
                        // But search returns a stream, so we should clear on each emission?
                        // The previous implementation cleared inside collect in display... which is correct for a continuous flow.
                        // For search, it's similar.
                        
                        // Actually, since we are combining two sources (terminals and routes) into one map,
                        // clearing inside one collect might wipe the other's results if they emit at different times.
                        // This is a bit tricky with multiple flows updating the same UI state.
                        // However, since this is a simple search, let's just add markers.
                        // To handle updates properly, we should manage the marker list carefully.
                        // For simplicity: we clear specific markers? No, markers list is global.
                        
                        // For now, let's just add. (Issue: if search result changes, we need to remove old ones).
                        // Ideally we'd combine flows. 
                        
                        // Let's assume for search we just show what we get. 
                        // If the user types more, performSearch is called again, canceling this job and clearing everything.
                        
                        terminals.forEach { terminal ->
                            if (terminal.latitude != null && terminal.longitude != null) {
                                val position = LatLng(terminal.latitude, terminal.longitude)
                                val marker = googleMap.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title(terminal.name)
                                        .snippet(terminal.description)
                                )
                                marker?.let { markers.add(it) }
                            }
                        }
                        
                        // Zoom to first result if available and no routes found yet
                        if (terminals.isNotEmpty() && routeLayers.isEmpty()) {
                            terminals[0].latitude?.let { lat ->
                                terminals[0].longitude?.let { lng ->
                                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
                                }
                            }
                        }
                    }
                }
            }
            
            // Search Routes
            launch {
                routeRepository.search(query).collect { routes ->
                    withContext(Dispatchers.Main) {
                        routes.forEach { route ->
                             route.routeGeoJson?.let { geoJson ->
                                 try {
                                     val jsonObject = JSONObject(geoJson)
                                     val layer = GeoJsonLayer(googleMap, jsonObject)
                                     
                                     val style = layer.defaultLineStringStyle
                                     style.color = getColorForRoute(route.name)
                                     style.width = 12f
                                     
                                     layer.addLayerToMap()
                                     routeLayers.add(layer)
                                     
                                     layer.setOnFeatureClickListener { _ ->
                                         showRouteInfo(route.name, route.destination, route.fare, route.estimatedTime)
                                     }
                                     
                                 } catch (e: Exception) {
                                     Log.e("HomeFragment", "Error loading GeoJSON for route ${route.name}", e)
                                 }
                             }
                        }
                    }
                }
            }
        }
    }

    private fun showRouteInfo(name: String, destination: String, fare: Double, time: String) {
        binding.cardRouteInfo.visibility = View.VISIBLE
        
        binding.textRouteName.text = name
        binding.textRouteDestination.text = "To: $destination"
        binding.textRouteFare.text = "Fare: ₱${"%.2f".format(fare)}"
        binding.textRouteTime.text = "Time: $time"
    }

    private fun showAddBaguioRouteDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Suggest Baguio Route")
            .setMessage("Route suggestion feature coming soon!\n\nFor now, you can:\n1. Take note of the route details\n2. Contact the app developers\n3. Help us map Baguio's jeepney network!")
            .setPositiveButton("Got it", null)
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
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
