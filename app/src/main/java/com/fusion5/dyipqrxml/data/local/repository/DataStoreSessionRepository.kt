package com.fusion5.dyipqrxml.data.local.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fusion5.dyipqrxml.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "dyipqr_session")

private val KEY_USER_ID = longPreferencesKey("session_user_id")

class DataStoreSessionRepository(private val context: Context) : SessionRepository {
    private val store: DataStore<Preferences>
        get() = context.sessionDataStore

    override val sessionUserId: Flow<Long?> = store.data.map { it[KEY_USER_ID] }

    override suspend fun saveUserId(userId: Long) {
        store.edit { prefs -> prefs[KEY_USER_ID] = userId }
    }

    override suspend fun clear() {
        store.edit { prefs -> prefs.remove(KEY_USER_ID) }
    }
}
