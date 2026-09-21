package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "super_tv_favorites")

object FavoritesDataStore {
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_channel_ids")

    fun getFavoriteIds(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[FAVORITES_KEY] ?: emptySet()
        }
    }

    suspend fun toggleFavorite(context: Context, channelId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(channelId)) {
                current.remove(channelId)
            } else {
                current.add(channelId)
            }
            preferences[FAVORITES_KEY] = current
        }
    }
}
