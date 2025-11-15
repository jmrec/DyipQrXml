package com.fusion5.dyipqrxml.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fusion5.dyipqrxml.data.local.dao.FavoriteDao
import com.fusion5.dyipqrxml.data.local.dao.RouteDao
import com.fusion5.dyipqrxml.data.local.dao.ScanHistoryDao
import com.fusion5.dyipqrxml.data.local.dao.TerminalDao
import com.fusion5.dyipqrxml.data.local.dao.UserDao
import com.fusion5.dyipqrxml.data.local.entity.FavoriteEntity
import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import com.fusion5.dyipqrxml.data.local.entity.ScanHistoryEntity
import com.fusion5.dyipqrxml.data.local.entity.TerminalEntity
import com.fusion5.dyipqrxml.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        TerminalEntity::class,
        RouteEntity::class,
        FavoriteEntity::class,
        ScanHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DyipQrDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun terminalDao(): TerminalDao
    abstract fun routeDao(): RouteDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: DyipQrDatabase? = null

        fun getInstance(context: Context): DyipQrDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): DyipQrDatabase {
            val db = Room.databaseBuilder(
                context,
                DyipQrDatabase::class.java,
                "dyipqr.db"
            ).fallbackToDestructiveMigration()
                .build()
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseSeeder(context).seed(db)
            }
            return db
        }
    }
}
