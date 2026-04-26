package fr.vlegall.nanoorbitapplication.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesDataStore(private val context: Context) {

    private val FAVORITE_IDS = stringSetPreferencesKey("favorite_satellite_ids")

    val favoriteIds: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[FAVORITE_IDS] ?: emptySet() }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_IDS] ?: emptySet()
            prefs[FAVORITE_IDS] = if (id in current) current - id else current + id
        }
    }
}