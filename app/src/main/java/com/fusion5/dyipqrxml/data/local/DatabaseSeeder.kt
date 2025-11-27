package com.fusion5.dyipqrxml.data.local

import android.content.Context
import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import com.fusion5.dyipqrxml.data.local.entity.RouteTerminalCrossRef
import com.fusion5.dyipqrxml.data.local.entity.TerminalEntity
import com.fusion5.dyipqrxml.data.local.entity.UserEntity
import com.fusion5.dyipqrxml.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

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
        val json = context.assets.open("seed_terminals.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val terminals = mutableListOf<TerminalEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            
            var lat = obj.optDouble("latitude", Double.NaN)
            var lon = obj.optDouble("longitude", Double.NaN)
            
            if (lat.isNaN() || lon.isNaN()) {
                val terminalsArray = obj.optJSONArray("terminals")
                if (terminalsArray != null && terminalsArray.length() > 0) {
                     val firstTerminal = terminalsArray.getJSONObject(0)
                     lat = firstTerminal.optDouble("latitude", Double.NaN)
                     lon = firstTerminal.optDouble("longitude", Double.NaN)
                }
            }
            
            terminals.add(
                TerminalEntity(
                    name = obj.getString("name"),
                    description = obj.getString("description"),
                    latitude = lat.takeIf { !it.isNaN() },
                    longitude = lon.takeIf { !it.isNaN() }
                )
            )
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
        val crossRefs = mutableListOf<RouteTerminalCrossRef>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            
            // Create and Insert Route
            val route = RouteEntity(
                name = obj.getString("name"),
                destination = obj.getString("destination"),
                fare = obj.getDouble("fare"),
                estimatedTime = obj.getString("estimatedTime"),
                frequency = obj.getString("frequency"),
                routeGeoJson = obj.getJSONObject("routeGeoJson").toString()
            )
            val routeId = routeDao.insertRoute(route)

            // Create Cross Refs
            val terminalName = obj.optString("terminalName")
            // Handle single terminalName or potentially an array if JSON structure changes in future
            // For now, based on previous structure, it's "terminalName"
            
            val terminal = terminalMap[terminalName]
            if (terminal != null) {
                crossRefs.add(RouteTerminalCrossRef(routeId, terminal.id))
            }
            
            // Note: If seed_routes.json is updated to have "terminalNames" array, 
            // we would iterate here.
        }
        routeDao.insertRouteTerminalCrossRefs(crossRefs)
    }

    private suspend fun seedShowcaseUser(database: DyipQrDatabase) {
        val userDao = database.userDao()
        val existing = userDao.getByEmail("demo@dyip.local")
        if (existing != null) return
        val hash = passwordHasher.hash("password123")
        userDao.insert(
            UserEntity(
                fullName = "Demo User",
                email = "demo@dyip.local",
                passwordHash = hash
            )
        )
    }
}
