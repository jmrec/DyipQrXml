package com.fusion5.dyipqrxml.data.local

import android.content.Context
import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import com.fusion5.dyipqrxml.data.local.entity.TerminalEntity
import com.fusion5.dyipqrxml.data.local.entity.UserEntity
import com.fusion5.dyipqrxml.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseSeeder(private val context: Context) {
    private val passwordHasher = PasswordHasher()

    suspend fun seed(database: DyipQrDatabase) = withContext(Dispatchers.IO) {
        seedTerminals(database)
        seedRoutes(database)
        seedShowcaseUser(database)
    }

    private suspend fun seedTerminals(database: DyipQrDatabase) {
        val dao = database.terminalDao()
        if (dao.count() > 0) return

        // Read routes to extract terminal coordinates
        val routesJson = context.assets.open("seed_routes.json").bufferedReader().use { it.readText() }
        val routesArray = JSONArray(routesJson)
        val terminals = mutableListOf<TerminalEntity>()

        val currentTime = getCurrentTimestamp()
        var terminalCounter = 0

        for (i in 0 until routesArray.length()) {
            val routeObj = routesArray.getJSONObject(i)
            val routeName = routeObj.getString("name")
            
            // Extract coordinates from GeoJSON
            val routeGeoJson = routeObj.getJSONObject("routeGeoJson")
            val features = routeGeoJson.getJSONArray("features")
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
                    
                    // Create UNIQUE terminal names by including route name
                    val routeParts = routeName.split("-")
                    val startTerminalName = if (routeParts.size > 1) "${routeParts[0].trim()} (${routeName})" else "${routeName} Start"
                    val endTerminalName = if (routeParts.size > 1) "${routeParts[1].trim()} (${routeName})" else "${routeName} End"
                    
                    terminals.add(
                        TerminalEntity(
                            name = startTerminalName,
                            description = "Terminal for $routeName",
                            latitude = startLat,
                            longitude = startLng,
                            createdAt = currentTime,
                            updatedAt = currentTime
                        )
                    )
                    
                    terminals.add(
                        TerminalEntity(
                            name = endTerminalName,
                            description = "Terminal for $routeName",
                            latitude = endLat,
                            longitude = endLng,
                            createdAt = currentTime,
                            updatedAt = currentTime
                        )
                    )
                    
                    terminalCounter += 2
                }
            }
        }
        
        dao.insertAll(terminals)
    }

    private suspend fun seedRoutes(database: DyipQrDatabase) {
        val routeDao = database.routeDao()
        if (routeDao.count() > 0) return

        val terminalDao = database.terminalDao()
        val terminals = terminalDao.getAllList()
        val terminalMap = terminals.associateBy { it.name }

        val json = context.assets.open("seed_routes.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        
        val currentTime = getCurrentTimestamp()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val routeName = obj.getString("name")
            
            // Extract terminal names from route name with route name included for uniqueness
            val routeParts = routeName.split("-")
            val startTerminalName = if (routeParts.size > 1) "${routeParts[0].trim()} (${routeName})" else "${routeName} Start"
            val endTerminalName = if (routeParts.size > 1) "${routeParts[1].trim()} (${routeName})" else "${routeName} End"
            
            val startTerminal = terminalMap[startTerminalName]
            val endTerminal = terminalMap[endTerminalName]
            
            if (startTerminal == null || endTerminal == null) {
                // Log error but continue
                continue
            }

            // Parse frequency from string like "Every 5 mins"
            val freqString = obj.getString("frequency")
            val freqInt = freqString.filter { it.isDigit() }.toIntOrNull() ?: 0

            // Parse estimated time from string like "30-45 mins"
            val timeString = obj.getString("estimatedTime")
            val estimatedSeconds = if (timeString.contains("-")) {
                val parts = timeString.split("-")
                val first = parts[0].filter { it.isDigit() }.toLongOrNull() ?: 0
                first * 60
            } else {
                val first = timeString.filter { it.isDigit() }.toLongOrNull() ?: 0
                first * 60
            }

            val route = RouteEntity(
                startTerminalId = startTerminal.id,
                endTerminalId = endTerminal.id,
                routeCode = routeName,
                fare = obj.getDouble("fare"),
                estimatedTravelTimeInSeconds = estimatedSeconds,
                frequency = freqInt,
                routeGeoJson = obj.getJSONObject("routeGeoJson").toString(),
                createdAt = currentTime,
                updatedAt = currentTime
            )
            routeDao.insertRoute(route)
        }
    }

    private suspend fun seedShowcaseUser(database: DyipQrDatabase) {
        val userDao = database.userDao()
        val existing = userDao.getByEmail("demo@dyip.local")
        if (existing != null) return
        
        val hash = passwordHasher.hash("password123")
        val currentTime = getCurrentTimestamp()
        
        userDao.insert(
            UserEntity(
                firstName = "Demo",
                lastName = "User",
                email = "demo@dyip.local",
                passwordHash = hash,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
