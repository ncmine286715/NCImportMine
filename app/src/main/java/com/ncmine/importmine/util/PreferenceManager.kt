package com.ncmine.importmine.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gerencia a persistência simples de Favoritos e Histórico
 */
class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ncmine_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FAVORITES = "favorites_ids"
        private const val KEY_HISTORY = "history_ids"
    }

    fun toggleFavorite(id: String) {
        val favorites = getFavorites().toMutableSet()
        if (favorites.contains(id)) {
            favorites.remove(id)
        } else {
            favorites.add(id)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun isFavorite(id: String): Boolean = getFavorites().contains(id)

    fun addToHistory(id: String) {
        val history = getHistory().toMutableSet()
        history.add(id)
        prefs.edit().putStringSet(KEY_HISTORY, history).apply()
    }

    fun getHistory(): Set<String> {
        return prefs.getStringSet(KEY_HISTORY, emptySet()) ?: emptySet()
    }

    fun isImported(id: String): Boolean = getHistory().contains(id)

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun clearFavorites() {
        prefs.edit().remove(KEY_FAVORITES).apply()
    }

    // Salva o ID por path para persistência entre scans (já que o ID aleatório muda)
    fun getPersistentId(path: String): String {
        val key = "path_id_$path"
        var id = prefs.getString(key, null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(key, id).apply()
        }
        return id
    }
}
