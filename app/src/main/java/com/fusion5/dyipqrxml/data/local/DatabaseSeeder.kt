package com.fusion5.dyipqrxml.data.local

import android.content.Context
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
            terminals.add(
                TerminalEntity(
                    name = obj.getString("name"),
                    description = obj.getString("description"),
                    latitude = obj.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() },
                    longitude = obj.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
                )
            )
        }
        dao.insertAll(terminals)
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
