package com.fusion5.dyipqrxml

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.navigation.fragment.findNavController
import com.fusion5.dyipqrxml.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import kotlin.math.*

class HomeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    
    private val markers = mutableListOf<Marker>()
    private val polylines = mutableListOf<Polyline>()
    private val polygons = mutableListOf<Polygon>()
    private var selectedTerminal: TerminalData? = null

    // Google Maps styling constants following the example pattern
    private val COLOR_BLACK_ARGB = -0x1000000
    private val COLOR_WHITE_ARGB = -0x1
    private val COLOR_DARK_GREEN_ARGB = -0xc771c4
    private val COLOR_LIGHT_GREEN_ARGB = -0x7e387c
    private val COLOR_DARK_ORANGE_ARGB = -0xa80e9
    private val COLOR_LIGHT_ORANGE_ARGB = -0x657db
    private val POLYLINE_STROKE_WIDTH_PX = 12
    private val POLYGON_STROKE_WIDTH_PX = 8
    private val PATTERN_GAP_LENGTH_PX = 20
    private val PATTERN_DASH_LENGTH_PX = 20

    private val DOT: PatternItem = Dot()
    private val GAP: PatternItem = Gap(PATTERN_GAP_LENGTH_PX.toFloat())
    private val DASH: PatternItem = Dash(PATTERN_DASH_LENGTH_PX.toFloat())

    // Create stroke patterns following Google example
    private val PATTERN_POLYLINE_DOTTED = listOf(GAP, DOT)
    private val PATTERN_POLYGON_ALPHA = listOf(GAP, DASH)
    private val PATTERN_POLYGON_BETA = listOf(DOT, GAP, DASH, GAP)

    // Baguio center coordinates
    private val baguioCenter = LatLng(16.4023, 120.5960)

    // Known Baguio Jeepney Terminals and Routes
    private val baguioTerminals = listOf(
        TerminalData("Dangwa Terminal", "To La Trinidad, Benguet", 16.4175, 120.5936),
        TerminalData("Slaughterhouse Terminal", "To Ambuklao, Itogon", 16.4289, 120.5894),
        TerminalData("Market Terminal", "City Proper Routes", 16.4142, 120.5978),
        TerminalData("Session Road Terminal", "Central Business District", 16.4132, 120.5971),
        TerminalData("Burnham Park Terminal", "Tourist Area Routes", 16.4115, 120.5989),
        TerminalData("Mines View Terminal", "To Mines View, Gibraltar", 16.4032, 120.6247),
        TerminalData("Camp 7 Terminal", "To Kennon Road Areas", 16.4267, 120.5736),
        TerminalData("Aurora Hill Terminal", "Aurora Hill Routes", 16.4208, 120.6086)
    )

    private val baguioRoutes = listOf(
        RouteData("Dangwa - La Trinidad", "La Trinidad Public Market", 13.00, "15-20 min", listOf(
            LatLng(16.4175, 120.5936), // Dangwa Terminal
            LatLng(16.4180, 120.5940), // Magsaysay Ave
            LatLng(16.4200, 120.5950), // Towards La Trinidad
            LatLng(16.4620, 120.5870)  // La Trinidad Market
        )),
        RouteData("Slaughter - Ambuklao", "Ambuklao Dam", 25.00, "45 min", listOf(
            LatLng(16.4289, 120.5894), // Slaughter Terminal
            LatLng(16.4300, 120.5900), // Marcos Highway
            LatLng(16.4500, 120.6000), // Towards Ambuklao
            LatLng(16.5100, 120.6500)  // Ambuklao Area
        )),
        RouteData("Session - Burnham Loop", "City Center Loop", 13.00, "10 min", listOf(
            LatLng(16.4132, 120.5971), // Session Road
            LatLng(16.4115, 120.5989), // Burnham Park
            LatLng(16.4142, 120.5978), // Market
            LatLng(16.4132, 120.5971)  // Back to Session
        )),
        RouteData("Mines View - Gibraltar", "Gibraltar Proper", 15.00, "20 min", listOf(
            LatLng(16.4032, 120.6247), // Mines View
            LatLng(16.4000, 120.6300), // Gibraltar Road
            LatLng(16.3900, 120.6400)  // Gibraltar Proper
        ))
    )

    data class TerminalData(
        val name: String,
        val description: String,
        val latitude: Double,
        val longitude: Double
    )

    data class RouteData(
        val name: String,
        val destination: String,
        val fare: Double,
        val estimatedTime: String,
        val path: List<LatLng>
    )

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
        
        // Show Baguio route list as fallback
        showBaguioRouteList()
        
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
        googleMap.setOnPolylineClickListener(this)
        googleMap.setOnPolygonClickListener(this)
        
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
        
        // Display Baguio terminals
        displayBaguioTerminals()
        
        // Add sample polygons for Baguio areas following Google example
        displayBaguioAreas()
        
        // Get current location if permission granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
        
        // Add a test marker to verify map is working
        val testMarker = googleMap.addMarker(
            MarkerOptions()
                .position(baguioCenter)
                .title("Baguio City Center")
                .snippet("Summer Capital of the Philippines")
        )
        
        if (testMarker != null) {
            Log.d("HomeFragment", "Test marker added successfully")
        } else {
            Log.e("HomeFragment", "Failed to add test marker")
        }
        
        // Check if map is actually loaded
        googleMap.setOnMapLoadedCallback {
            Log.d("HomeFragment", "Map tiles loaded successfully")
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val terminalName = marker.title
        val terminal = baguioTerminals.find { it.name == terminalName }
        terminal?.let {
            showRouteInfo(it)
            displayRoutesForTerminal(it.name)
        }
        return true
    }

    override fun onPolylineClick(polyline: Polyline) {
        // Flip from solid stroke to dotted stroke pattern following Google example
        if (polyline.pattern == null || !polyline.pattern!!.contains(DOT)) {
            polyline.pattern = PATTERN_POLYLINE_DOTTED
        } else {
            // The default pattern is a solid stroke
            polyline.pattern = null
        }
        
        // Show route information when clicked
        val routeName = polyline.tag?.toString() ?: "Unknown Route"
        android.widget.Toast.makeText(requireContext(), "Route: $routeName", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onPolygonClick(polygon: Polygon) {
        // Flip the values of the red, green, and blue components of the polygon's color
        var color = polygon.strokeColor xor 0x00ffffff
        polygon.strokeColor = color
        color = polygon.fillColor xor 0x00ffffff
        polygon.fillColor = color
        
        val areaName = polygon.tag?.toString() ?: "Unknown Area"
        android.widget.Toast.makeText(requireContext(), "Area: $areaName", android.widget.Toast.LENGTH_SHORT).show()
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
                searchBaguioTerminals(s.toString())
            }
        })
    }

    private fun displayBaguioTerminals() {
        // Clear existing markers
        markers.forEach { it.remove() }
        markers.clear()
        
        // Add markers for Baguio terminals
        baguioTerminals.forEach { terminal ->
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

    private fun searchBaguioTerminals(query: String) {
        val filteredTerminals = if (query.isBlank()) {
            baguioTerminals
        } else {
            baguioTerminals.filter { 
                it.name.contains(query, true) || it.description.contains(query, true)
            }
        }
        
        // Update markers
        markers.forEach { it.remove() }
        markers.clear()
        
        filteredTerminals.forEach { terminal ->
            val position = LatLng(terminal.latitude, terminal.longitude)
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(terminal.name)
                    .snippet(terminal.description)
            )
            marker?.let { markers.add(it) }
        }
        
        // If only one result, zoom to it
        if (filteredTerminals.size == 1) {
            val position = LatLng(filteredTerminals[0].latitude, filteredTerminals[0].longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
        }
    }

    private fun displayRoutesForTerminal(terminalName: String) {
        // Clear existing polylines
        polylines.forEach { it.remove() }
        polylines.clear()
        
        // Find routes for this terminal
        val relevantRoutes = baguioRoutes.filter { route ->
            route.name.contains(terminalName, true)
        }
        
        // Display routes with Google-compliant styling
        relevantRoutes.forEachIndexed { index, route ->
            if (route.path.size >= 2) {
                val polyline = googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(route.path)
                        .clickable(true)
                )
                polyline?.let {
                    polylines.add(it)
                    // Store data object with polyline following Google example
                    it.tag = route.name
                    // Style the polyline based on route type
                    stylePolyline(it, index)
                }
            }
        }
        
        // If no specific routes found, show all Baguio routes
        if (relevantRoutes.isEmpty()) {
            showAllBaguioRoutes()
        }
    }

    private fun showAllBaguioRoutes() {
        baguioRoutes.forEachIndexed { index, route ->
            if (route.path.size >= 2) {
                val polyline = googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(route.path)
                        .clickable(true)
                )
                polyline?.let {
                    polylines.add(it)
                    // Store data object with polyline following Google example
                    it.tag = route.name
                    // Style the polyline based on route type
                    stylePolyline(it, index)
                }
            }
        }
    }

    /**
     * Styles the polyline, based on type - following Google example pattern
     * @param polyline The polyline object that needs styling.
     * @param index The index of the route for styling variation
     */
    private fun stylePolyline(polyline: Polyline, index: Int) {
        // Get the data object stored with the polyline.
        val type = polyline.tag?.toString() ?: ""
        
        when (index % 3) {
            0 -> {
                // Use a custom bitmap as the cap at the start of the line for first type
                polyline.startCap = CustomCap(
                    BitmapDescriptorFactory.fromResource(com.fusion5.dyipqrxml.R.drawable.ic_route), 10f)
                polyline.color = COLOR_DARK_GREEN_ARGB
            }
            1 -> {
                // Use a round cap at the start of the line for second type
                polyline.startCap = RoundCap()
                polyline.color = COLOR_DARK_ORANGE_ARGB
            }
            else -> {
                // Default styling for other routes
                polyline.startCap = RoundCap()
                polyline.color = COLOR_BLACK_ARGB
            }
        }
        
        polyline.endCap = RoundCap()
        polyline.width = POLYLINE_STROKE_WIDTH_PX.toFloat()
        polyline.jointType = JointType.ROUND
    }

    /**
     * Styles the polygon, based on type - following Google example pattern
     * @param polygon The polygon object that needs styling.
     * @param type The type of polygon for styling variation
     */
    private fun displayBaguioAreas() {
        // Clear existing polygons
        polygons.forEach { it.remove() }
        polygons.clear()
        
        // Create sample polygons for Baguio areas
        val burnhamParkArea = googleMap.addPolygon(
            PolygonOptions()
                .add(
                    LatLng(16.4100, 120.5940),
                    LatLng(16.4100, 120.5970),
                    LatLng(16.4130, 120.5970),
                    LatLng(16.4130, 120.5940),
                    LatLng(16.4100, 120.5940) // Close the polygon
                )
                .clickable(true)
        )
        burnhamParkArea?.let {
            polygons.add(it)
            it.tag = "Burnham Park Area"
            stylePolygon(it, "alpha")
        }
        
        val sessionRoadArea = googleMap.addPolygon(
            PolygonOptions()
                .add(
                    LatLng(16.4120, 120.5950),
                    LatLng(16.4120, 120.5980),
                    LatLng(16.4150, 120.5980),
                    LatLng(16.4150, 120.5950),
                    LatLng(16.4120, 120.5950) // Close the polygon
                )
                .clickable(true)
        )
        sessionRoadArea?.let {
            polygons.add(it)
            it.tag = "Session Road Area"
            stylePolygon(it, "beta")
        }
    }

    private fun stylePolygon(polygon: Polygon, type: String) {
        var pattern: List<PatternItem>? = null
        var strokeColor = COLOR_BLACK_ARGB
        var fillColor = COLOR_WHITE_ARGB
        
        when (type) {
            "alpha" -> {
                // Apply a stroke pattern to render a dashed line, and define colors.
                pattern = PATTERN_POLYGON_ALPHA
                strokeColor = COLOR_DARK_GREEN_ARGB
                fillColor = COLOR_LIGHT_GREEN_ARGB
            }
            "beta" -> {
                // Apply a stroke pattern to render a line of dots and dashes, and define colors.
                pattern = PATTERN_POLYGON_BETA
                strokeColor = COLOR_DARK_ORANGE_ARGB
                fillColor = COLOR_LIGHT_ORANGE_ARGB
            }
        }
        
        polygon.strokePattern = pattern
        polygon.strokeWidth = POLYGON_STROKE_WIDTH_PX.toFloat()
        polygon.strokeColor = strokeColor
        polygon.fillColor = fillColor
    }

    private fun showRouteInfo(terminal: TerminalData) {
        binding.cardRouteInfo.visibility = View.VISIBLE
        
        // Find routes for this terminal
        val route = baguioRoutes.find { it.name.contains(terminal.name, true) }
        
        if (route != null) {
            binding.textRouteName.text = route.name
            binding.textRouteDestination.text = "To: ${route.destination}"
            binding.textRouteFare.text = "Fare: ₱${"%.2f".format(route.fare)}"
            binding.textRouteTime.text = "Time: ${route.estimatedTime}"
        } else {
            binding.textRouteName.text = terminal.name
            binding.textRouteDestination.text = terminal.description
            binding.textRouteFare.text = "Fare: ₱13.00+"
            binding.textRouteTime.text = "Multiple Routes"
        }
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
            LOCATION_PERMISSION_REQUEST_CODE
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

    private fun showBaguioRouteList() {
        // Show route information even if map doesn't load
        binding.cardRouteInfo.visibility = View.VISIBLE
        binding.textRouteName.text = "Baguio Jeepney Routes"
        binding.textRouteDestination.text = "Tap terminals on map to see routes"
        binding.textRouteFare.text = "Base Fare: ₱13.00"
        binding.textRouteTime.text = "Multiple Routes Available"
        
        // Show a helpful message about the map
        view?.postDelayed({
            if (!::googleMap.isInitialized) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Map Loading")
                    .setMessage("The map is taking longer than expected to load. Here are the main Baguio jeepney terminals:\n\n• Dangwa Terminal - To La Trinidad\n• Slaughterhouse - To Ambuklao\n• Session Road - City Center\n• Mines View - To Gibraltar\n\nCheck your Google Maps API key configuration if the map doesn't appear.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }, 3000)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
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