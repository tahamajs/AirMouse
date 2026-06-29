// app/src/main/java/com/airmouse/community/CommunityHub.kt
package com.airmouse.community

import android.content.Context
import android.util.Log
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

/**
 * Community hub for sharing gestures, profiles, macros, and themes.
 *
 * Provides:
 * - Fetch trending items with caching
 * - Search
 * - Upload, like, download, rate, report
 * - Favorites management
 * - My uploads management
 *
 * All network operations are performed on background threads.
 */
class CommunityHub(
    private val context: Context,
    private val prefs: PreferencesManager
) {

    companion object {
        private const val TAG = "CommunityHub"
        private const val API_BASE_URL = "https://api.airmouse.com/v1"
        private const val CACHE_DIR = "community_cache"
        private const val MAX_CACHE_AGE_MS = 5 * 60 * 1000 // 5 minutes
    }

    // ============================================================
    // Data Classes
    // ============================================================

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

    // ============================================================
    // State Flows
    // ============================================================

    private val _trending = MutableStateFlow<List<SharedItem>>(emptyList())
    val trending: StateFlow<List<SharedItem>> = _trending.asStateFlow()

    private val _favorites = MutableStateFlow<List<SharedItem>>(emptyList())
    val favorites: StateFlow<List<SharedItem>> = _favorites.asStateFlow()

    private val _myUploads = MutableStateFlow<List<SharedItem>>(emptyList())
    val myUploads: StateFlow<List<SharedItem>> = _myUploads.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SharedItem>>(emptyList())
    val searchResults: StateFlow<List<SharedItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    // ============================================================
    // Internal State
    // ============================================================

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cacheDir = File(context.cacheDir, CACHE_DIR)

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        loadFavorites()
        loadMyUploads()
        loadCachedTrending()
    }

    // ============================================================
    // Public API
    // ============================================================

    /**
     * Fetch trending items from the server.
     * @param type Optional filter by item type.
     * @param limit Number of items to fetch (default 20).
     * @param offset Pagination offset (default 0).
     */
    fun fetchTrending(type: ItemType? = null, limit: Int = 20, offset: Int = 0) {
        scope.launch {
            _isLoading.value = true
            _lastError.value = null
            try {
                val url = URL(
                    "$API_BASE_URL/trending?limit=$limit&offset=$offset&type=${type?.name ?: ""}"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val items = parseItems(JSONArray(response))
                    _trending.value = items
                    cacheTrending(items)
                    Log.d(TAG, "Fetched ${items.size} trending items")
                } else {
                    val error = "Server error: ${connection.responseCode}"
                    Log.e(TAG, error)
                    _lastError.value = error
                    loadCachedTrending()
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch trending", e)
                _lastError.value = e.message ?: "Network error"
                loadCachedTrending()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Search for items by query.
     */
    fun search(query: String, type: ItemType? = null, limit: Int = 20) {
        scope.launch {
            _isLoading.value = true
            _lastError.value = null
            try {
                val url = URL(
                    "$API_BASE_URL/search?q=${query}&type=${type?.name ?: ""}&limit=$limit"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val items = parseItems(JSONArray(response))
                    _searchResults.value = items
                    Log.d(TAG, "Search found ${items.size} items")
                } else {
                    _lastError.value = "Search failed: ${connection.responseCode}"
                    _searchResults.value = emptyList()
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _lastError.value = e.message ?: "Search error"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Upload a new item to the community.
     */
    fun uploadItem(item: SharedItem) {
        scope.launch {
            _isLoading.value = true
            _lastError.value = null
            try {
                val url = URL("$API_BASE_URL/upload")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val json = JSONObject().apply {
                    put("title", item.title)
                    put("description", item.description)
                    put("type", item.type.name)
                    put("content", item.content)
                    put("tags", JSONArray(item.tags))
                }
                connection.outputStream.write(json.toString().toByteArray())

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val newItem = parseItem(JSONObject(response))
                    _myUploads.value = listOf(newItem) + _myUploads.value
                    cacheMyUploads()
                    Log.d(TAG, "Uploaded item: ${newItem.id}")
                } else {
                    _lastError.value = "Upload failed: ${connection.responseCode}"
                    Log.e(TAG, "Upload failed: ${connection.responseCode}")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
                _lastError.value = e.message ?: "Upload error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete an uploaded item.
     */
    fun deleteUpload(itemId: String) {
        scope.launch {
            _isLoading.value = true
            _lastError.value = null
            try {
                val url = URL("$API_BASE_URL/items/$itemId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    _myUploads.value = _myUploads.value.filter { it.id != itemId }
                    cacheMyUploads()
                    Log.d(TAG, "Deleted item: $itemId")
                } else {
                    _lastError.value = "Delete failed: ${connection.responseCode}"
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
                _lastError.value = e.message ?: "Delete error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Like an item.
     */
    fun likeItem(itemId: String) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/items/$itemId/like")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    updateItemLikes(itemId, true)
                    Log.d(TAG, "Liked item: $itemId")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Like failed", e)
            }
        }
    }

    /**
     * Unlike an item.
     */
    fun unlikeItem(itemId: String) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/items/$itemId/unlike")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    updateItemLikes(itemId, false)
                    Log.d(TAG, "Unliked item: $itemId")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Unlike failed", e)
            }
        }
    }

    /**
     * Download an item and import it locally.
     * @return The downloaded item, or null if failed.
     */
    suspend fun downloadItem(itemId: String): SharedItem? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE_URL/items/$itemId/download")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().readText()
                val downloadedItem = parseItem(JSONObject(content))

                // Import to local storage
                when (downloadedItem.type) {
                    ItemType.GESTURE -> importGesture(downloadedItem)
                    ItemType.PROFILE -> importProfile(downloadedItem)
                    ItemType.MACRO -> importMacro(downloadedItem)
                    ItemType.THEME -> importTheme(downloadedItem)
                    ItemType.SETTINGS -> importSettings(downloadedItem)
                }
                connection.disconnect()
                Log.d(TAG, "Downloaded item: ${downloadedItem.id}")
                return@withContext downloadedItem
            } else {
                Log.e(TAG, "Download failed: ${connection.responseCode}")
                connection.disconnect()
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            return@withContext null
        }
    }

    /**
     * Add an item to favorites.
     */
    fun addToFavorites(item: SharedItem) {
        val updatedItem = item.copy(isFavorite = true)
        _favorites.value = listOf(updatedItem) + _favorites.value.filter { it.id != item.id }
        saveFavorites()
        Log.d(TAG, "Added to favorites: ${item.id}")
    }

    /**
     * Remove an item from favorites.
     */
    fun removeFromFavorites(itemId: String) {
        _favorites.value = _favorites.value.filter { it.id != itemId }
        saveFavorites()
        Log.d(TAG, "Removed from favorites: $itemId")
    }

    /**
     * Rate an item.
     */
    fun rateItem(itemId: String, rating: Float) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/items/$itemId/rate")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val json = JSONObject().apply { put("rating", rating) }
                connection.outputStream.write(json.toString().toByteArray())

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    updateItemRating(itemId, rating)
                    Log.d(TAG, "Rated item: $itemId -> $rating")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Rate failed", e)
            }
        }
    }

    /**
     * Report an item for moderation.
     */
    fun reportItem(itemId: String, reason: String) {
        scope.launch {
            try {
                val url = URL("$API_BASE_URL/items/$itemId/report")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val json = JSONObject().apply { put("reason", reason) }
                connection.outputStream.write(json.toString().toByteArray())

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Reported item: $itemId")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Report failed", e)
            }
        }
    }

    /**
     * Force refresh trending items (ignores cache).
     */
    fun refresh() {
        fetchTrending()
    }

    /**
     * Get all available categories.
     */
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

    /**
     * Check if an item is in favorites.
     */
    fun isFavorite(itemId: String): Boolean {
        return _favorites.value.any { it.id == itemId }
    }

    /**
     * Get the count of my uploads.
     */
    fun getMyUploadsCount(): Int = _myUploads.value.size

    /**
     * Get the count of favorites.
     */
    fun getFavoritesCount(): Int = _favorites.value.size

    /**
     * Clear all cached data.
     */
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        scope.cancel()
    }

    // ============================================================
    // Private Helpers
    // ============================================================

    private fun parseItems(array: JSONArray): List<SharedItem> {
        val items = mutableListOf<SharedItem>()
        for (i in 0 until array.length()) {
            try {
                items.add(parseItem(array.getJSONObject(i)))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse item at index $i", e)
            }
        }
        return items
    }

    private fun parseItem(json: JSONObject): SharedItem {
        val authorJson = json.getJSONObject("author")
        val tagsArray = json.optJSONArray("tags")
        val tags = if (tagsArray != null) {
            (0 until tagsArray.length()).map { tagsArray.getString(it) }
        } else emptyList()

        return SharedItem(
            id = json.getString("id"),
            title = json.getString("title"),
            description = json.getString("description"),
            type = ItemType.valueOf(json.getString("type")),
            author = Author(
                id = authorJson.getString("id"),
                username = authorJson.getString("username"),
                avatarUrl = if (authorJson.has("avatarUrl") && !authorJson.isNull("avatarUrl")) authorJson.getString("avatarUrl") else null,
                reputation = authorJson.optInt("reputation", 0)
            ),
            downloads = json.getInt("downloads"),
            likes = json.getInt("likes"),
            rating = json.optDouble("rating", 0.0).toFloat(),
            tags = tags,
            content = json.getString("content"),
            createdAt = json.getLong("createdAt"),
            isFavorite = _favorites.value.any { it.id == json.getString("id") }
        )
    }

    private fun updateItemLikes(itemId: String, increment: Boolean) {
        val delta = if (increment) 1 else -1
        _trending.value = _trending.value.map {
            if (it.id == itemId) it.copy(likes = it.likes + delta) else it
        }
        _favorites.value = _favorites.value.map {
            if (it.id == itemId) it.copy(likes = it.likes + delta) else it
        }
        _myUploads.value = _myUploads.value.map {
            if (it.id == itemId) it.copy(likes = it.likes + delta) else it
        }
        _searchResults.value = _searchResults.value.map {
            if (it.id == itemId) it.copy(likes = it.likes + delta) else it
        }
    }

    private fun updateItemRating(itemId: String, rating: Float) {
        _trending.value = _trending.value.map {
            if (it.id == itemId) it.copy(rating = rating) else it
        }
        _favorites.value = _favorites.value.map {
            if (it.id == itemId) it.copy(rating = rating) else it
        }
        _myUploads.value = _myUploads.value.map {
            if (it.id == itemId) it.copy(rating = rating) else it
        }
        _searchResults.value = _searchResults.value.map {
            if (it.id == itemId) it.copy(rating = rating) else it
        }
    }

    private fun importGesture(item: SharedItem) {
        prefs.putString("imported_gesture_${item.id}", item.content)
    }

    private fun importProfile(item: SharedItem) {
        prefs.putString("imported_profile_${item.id}", item.content)
    }

    private fun importMacro(item: SharedItem) {
        prefs.putString("imported_macro_${item.id}", item.content)
    }

    private fun importTheme(item: SharedItem) {
        prefs.putString("imported_theme_${item.id}", item.content)
    }

    private fun importSettings(item: SharedItem) {
        prefs.putString("imported_settings_${item.id}", item.content)
    }

    // ============================================================
    // Caching
    // ============================================================

    private fun loadCachedTrending() {
        val cacheFile = File(cacheDir, "trending.json")
        if (cacheFile.exists()) {
            try {
                val json = JSONArray(cacheFile.readText())
                _trending.value = parseItems(json)
                Log.d(TAG, "Loaded ${_trending.value.size} trending from cache")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load cached trending", e)
            }
        }
    }

    private fun cacheTrending(items: List<SharedItem>) {
        try {
            val cacheFile = File(cacheDir, "trending.json")
            val array = JSONArray()
            items.forEach { item ->
                array.put(toJson(item))
            }
            cacheFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache trending", e)
        }
    }

    private fun loadMyUploads() {
        val cacheFile = File(cacheDir, "my_uploads.json")
        if (cacheFile.exists()) {
            try {
                val json = JSONArray(cacheFile.readText())
                _myUploads.value = parseItems(json)
                Log.d(TAG, "Loaded ${_myUploads.value.size} uploads from cache")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load cached uploads", e)
            }
        }
    }

    private fun cacheMyUploads() {
        try {
            val cacheFile = File(cacheDir, "my_uploads.json")
            val array = JSONArray()
            _myUploads.value.forEach { item ->
                array.put(toJson(item))
            }
            cacheFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache uploads", e)
        }
    }

    private fun loadFavorites() {
        val favoritesJson = prefs.getString("community_favorites", "")
        if (favoritesJson.isNotEmpty()) {
            try {
                val array = JSONArray(favoritesJson)
                _favorites.value = parseItems(array)
                Log.d(TAG, "Loaded ${_favorites.value.size} favorites")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load favorites", e)
            }
        }
    }

    private fun saveFavorites() {
        try {
            val array = JSONArray()
            _favorites.value.forEach { item ->
                array.put(toJson(item))
            }
            prefs.putString("community_favorites", array.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save favorites", e)
        }
    }

    private fun toJson(item: SharedItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("description", item.description)
            put("type", item.type.name)
            put("author", JSONObject().apply {
                put("id", item.author.id)
                put("username", item.author.username)
                put("avatarUrl", item.author.avatarUrl ?: "")
                put("reputation", item.author.reputation)
            })
            put("downloads", item.downloads)
            put("likes", item.likes)
            put("rating", item.rating)
            put("tags", JSONArray(item.tags))
            put("content", item.content)
            put("createdAt", item.createdAt)
        }
    }
}