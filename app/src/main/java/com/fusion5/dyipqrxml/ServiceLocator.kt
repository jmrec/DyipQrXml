package com.fusion5.dyipqrxml

import android.content.Context
import com.fusion5.dyipqrxml.data.local.DyipQrDatabase
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.FavoriteRepository
import com.fusion5.dyipqrxml.data.repository.RouteRepository
import com.fusion5.dyipqrxml.data.repository.ScanHistoryRepository
import com.fusion5.dyipqrxml.data.repository.SessionRepository
import com.fusion5.dyipqrxml.data.repository.TerminalRepository
import com.fusion5.dyipqrxml.data.repository.local.DataStoreSessionRepository
import com.fusion5.dyipqrxml.data.repository.local.LocalAuthRepository
import com.fusion5.dyipqrxml.data.repository.local.LocalFavoriteRepository
import com.fusion5.dyipqrxml.data.repository.local.LocalRouteRepository
import com.fusion5.dyipqrxml.data.repository.local.LocalScanHistoryRepository
import com.fusion5.dyipqrxml.data.repository.local.LocalTerminalRepository
import com.fusion5.dyipqrxml.util.PasswordHasher

object ServiceLocator {
    @Volatile
    private var database: DyipQrDatabase? = null

    fun provideDatabase(context: Context): DyipQrDatabase =
        database ?: synchronized(this) {
            database ?: DyipQrDatabase.getInstance(context).also { database = it }
        }

    fun providePasswordHasher(): PasswordHasher = PasswordHasher()

    fun provideSessionRepository(context: Context): SessionRepository =
        DataStoreSessionRepository(context.applicationContext)

    fun provideAuthRepository(context: Context): AuthRepository =
        LocalAuthRepository(
            database = provideDatabase(context),
            sessionRepository = provideSessionRepository(context),
            passwordHasher = providePasswordHasher()
        )

    fun provideTerminalRepository(context: Context): TerminalRepository =
        LocalTerminalRepository(provideDatabase(context).terminalDao())

    fun provideRouteRepository(context: Context): RouteRepository =
        LocalRouteRepository(provideDatabase(context).routeDao())

    fun provideFavoriteRepository(context: Context): FavoriteRepository =
        LocalFavoriteRepository(provideDatabase(context).favoriteDao())

    fun provideScanHistoryRepository(context: Context): ScanHistoryRepository =
        LocalScanHistoryRepository(provideDatabase(context).scanHistoryDao())
}

