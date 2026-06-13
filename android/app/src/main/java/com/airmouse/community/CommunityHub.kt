package com.airmouse.community

import android.content.Context
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Community hub for sharing gestures, profiles, and macros
 */
class CommunityHub(private val context: Context, private val prefs: PreferencesManager) {

    companion object {
        private const val TAG = "CommunityHub"
        private const val API_BASE_URL = "https://api.airmouse.com/v1"
        private const val CACHE_DIR = "community_cache"
    }

    data class SharedItem(
        val id: String,
        val title: String,
        val description: String,
        val type: ItemType,
        val author: Author,
        val downloads: Int,
        val likes: Int,
        val rating: Float,
        val tags: List<String>,
        val content: String,
        val createdAt: Long,
        val isFavorite: Boolean = false
    )

    data class Author(
        val id: String,
        val username: String,
        val avatarUrl: String? = null,
        val reputation: Int = 0
    )

    enum class ItemType(val displayName: String) {
        GESTURE("Gesture"),
        PROFILE("Profile"),
        MACRO("Macro"),
        THEME("Theme"),
        SETTINGS("Settings")
    }

    data class Category(
        val id: String,
        val name: String,
        val icon: String,
        val itemCount: Int
    )

    private val _trending = MutableStateFlow<List<SharedItem>>(emptyList())
    val trending: StateFlow<List<SharedItem>> = _trending.asStateFlow()

    private val _favorites = MutableStateFlow<List<SharedItem>>(emptyList())
    val favorites: StateFlow<List<SharedItem>> = _favorites.asStateFlow()

    private val _myUploads = MutableStateFlow<List<SharedItem>>(emptyList())
    val myUploads: StateFlow<List<SharedItem>> = _myUploads.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cacheDir = File(context.cacheDir, CACHE_DIR)

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        loadFavorites()
    }

    fun fetchTrending(type: ItemType? = null, limit: Int = 20) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/trending?limit=$limit&type=${type?.name}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val items = parseItems(JSONArray(response))
                    _trending.value = items
                }
                connection.disconnect()
            } catch (e: Exception) {
                // Load from cache
                loadCachedTrending()
            }
        }
    }

    fun search(query: String, type: ItemType? = null) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/search?q=${query}&type=${type?.name}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val items = parseItems(JSONArray(response))
                    // Update search results
                }
                connection.disconnect()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun uploadItem(item: SharedItem) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/upload")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("title", item.title)
                    put("description", item.description)
                    put("type", item.type.name)
                    put("content", item.content)
                    put("tags", JSONArray(item.tags))
                }

                connection.outputStream.write(json.toString().toByteArray())
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val newItem = parseItem(JSONObject(connection.inputStream.bufferedReader().readText()))
                    _myUploads.value = listOf(newItem) + _myUploads.value
                    cacheMyUploads()
                }
                connection.disconnect()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun likeItem(itemId: String) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/items/$itemId/like")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    updateItemLikes(itemId, true)
                }
                connection.disconnect()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun downloadItem(itemId: String): SharedItem? {
        var result: SharedItem? = null
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/items/$itemId/download")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val content = connection.inputStream.bufferedReader().readText()
                    result = parseItem(JSONObject(content))
                    // Import to local storage
                    when (result?.type) {
                        ItemType.GESTURE -> importGesture(result)
                        ItemType.PROFILE -> importProfile(result)
                        ItemType.MACRO -> importMacro(result)
                        ItemType.THEME -> importTheme(result)
                        else -> {}
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                // Handle error
            }
        }
        return result
    }

    fun addToFavorites(item: SharedItem) {
        val updatedItem = item.copy(isFavorite = true)
        _favorites.value = listOf(updatedItem) + _favorites.value.filter { it.id != item.id }
        saveFavorites()
    }

    fun removeFromFavorites(itemId: String) {
        _favorites.value = _favorites.value.filter { it.id != itemId }
        saveFavorites()
    }

    fun rateItem(itemId: String, rating: Float) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/items/$itemId/rate")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply { put("rating", rating) }
                connection.outputStream.write(json.toString().toByteArray())
                connection.connect()
                connection.disconnect()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun reportItem(itemId: String, reason: String) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/items/$itemId/report")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply { put("reason", reason) }
                connection.outputStream.write(json.toString().toByteArray())
                connection.connect()
                connection.disconnect()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun parseItems(array: JSONArray): List<SharedItem> {
        val items = mutableListOf<SharedItem>()
        for (i in 0 until array.length()) {
            items.add(parseItem(array.getJSONObject(i)))
        }
        return items
    }

    private fun parseItem(json: JSONObject): SharedItem {
        val authorJson = json.getJSONObject("author")
        return SharedItem(
            id = json.getString("id"),
            title = json.getString("title"),
            description = json.getString("description"),
            type = ItemType.valueOf(json.getString("type")),
            author = Author(
                id = authorJson.getString("id"),
                username = authorJson.getString("username"),
                avatarUrl = authorJson.optString("avatarUrl", null),
                reputation = authorJson.optInt("reputation", 0)
            ),
            downloads = json.getInt("downloads"),
            likes = json.getInt("likes"),
            rating = json.optDouble("rating", 0.0).toFloat(),
            tags = json.optJSONArray("tags")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            content = json.getString("content"),
            createdAt = json.getLong("createdAt"),
            isFavorite = _favorites.value.any { it.id == json.getString("id") }
        )
    }

    private fun updateItemLikes(itemId: String, increment: Boolean) {
        val updateList = { list: List<SharedItem> ->
            list.map {
                if (it.id == itemId) {
                    it.copy(likes = it.likes + if (increment) 1 else -1)
                } else it
            }
        }
        _trending.value = updateList(_trending.value)
        _favorites.value = updateList(_favorites.value)
    }

    private fun importGesture(item: SharedItem) {
        // Save gesture to local storage
        prefs.putString("imported_gesture_${item.id}", item.content)
    }

    private fun importProfile(item: SharedItem) {
        // Save profile to local storage
        prefs.putString("imported_profile_${item.id}", item.content)
    }

    private fun importMacro(item: SharedItem) {
        // Save macro to local storage
        prefs.putString("imported_macro_${item.id}", item.content)
    }

    private fun importTheme(item: SharedItem) {
        // Save theme to local storage
        prefs.putString("imported_theme_${item.id}", item.content)
    }

    private fun loadFavorites() {
        val favoritesJson = prefs.getString("community_favorites", "")
        if (favoritesJson.isNotEmpty()) {
            try {
                val array = JSONArray(favoritesJson)
                _favorites.value = parseItems(array)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun saveFavorites() {
        val array = JSONArray()
        _favorites.value.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("description", item.description)
                put("type", item.type.name)
                put("author", JSONObject().apply {
                    put("id", item.author.id)
                    put("username", item.author.username)
                })
                put("downloads", item.downloads)
                put("likes", item.likes)
                put("rating", item.rating)
                put("tags", JSONArray(item.tags))
                put("content", item.content)
                put("createdAt", item.createdAt)
            })
        }
        prefs.putString("community_favorites", array.toString())
    }

    private fun loadCachedTrending() {
        val cacheFile = File(cacheDir, "trending.json")
        if (cacheFile.exists()) {
            try {
                val json = JSONArray(cacheFile.readText())
                _trending.value = parseItems(json)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun cacheMyUploads() {
        val cacheFile = File(cacheDir, "my_uploads.json")
        val array = JSONArray()
        _myUploads.value.forEach { item ->
            array.put(JSONObject().apply { put("id", item.id) })
        }
        cacheFile.writeText(array.toString())
    }

    fun getCategories(): List<Category> {
        return listOf(
            Category("popular", "Popular", "🔥", 1523),
            Category("new", "New", "✨", 847),
            Category("gestures", "Gestures", "✋", 2341),
            Category("profiles", "Profiles", "👤", 892),
            Category("macros", "Macros", "⚡", 456),
            Category("themes", "Themes", "🎨", 1234)
        )
    }

    fun cleanup() {
        scope.cancel()
    }
}