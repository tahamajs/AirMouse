package com.airmouse.auth

import android.content.Context
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication state and token storage.
 *
 * The token is stored in PreferencesManager and persisted across app restarts.
 * The auth state is exposed as a StateFlow for reactive UI updates.
 */
@Singleton
class AuthManager @Inject constructor(
    private val prefs: PreferencesManager
) {
    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_AUTH_STATE = "auth_state"
    }

    private val _authState = MutableStateFlow(AuthState.UNAUTHENTICATED)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    enum class AuthState {
        UNAUTHENTICATED,
        AUTHENTICATING,
        AUTHENTICATED,
        ERROR,
        EXPIRED
    }

    init {
        // Load token from storage on startup
        loadToken()
    }

    /**
     * Load the stored token from PreferencesManager and update state.
     */
    private fun loadToken() {
        val token = prefs.getString(KEY_AUTH_TOKEN, "")
        if (!token.isNullOrBlank()) {
            _authToken.value = token
            _authState.value = AuthState.AUTHENTICATED
        } else {
            _authToken.value = null
            _authState.value = AuthState.UNAUTHENTICATED
        }
    }

    /**
     * Authenticate with a token.
     * @param token The token received from the server (or generated).
     * @return true if token is valid and authentication succeeded.
     */
    suspend fun authenticate(token: String): Boolean {
        _authState.value = AuthState.AUTHENTICATING
        return try {
            // Perform local validation (can be extended to call server)
            val isValid = validateToken(token)
            if (isValid) {
                _authToken.value = token
                prefs.putString(KEY_AUTH_TOKEN, token)
                _authState.value = AuthState.AUTHENTICATED
            } else {
                _authState.value = AuthState.ERROR
            }
            isValid
        } catch (e: Exception) {
            _authState.value = AuthState.ERROR
            false
        }
    }

    /**
     * Validate a token locally.
     * For production, you can extend this to call the server's /validate endpoint.
     */
    private suspend fun validateToken(token: String): Boolean {
        // Simple check: token must be non-empty and reasonably long
        // In a real app, you would decode a JWT or call server.
        return token.isNotEmpty() && token.length >= 8
    }

    /**
     * Get the current auth token for use in network requests.
     * Returns null if not authenticated.
     */
    fun getAuthToken(): String? = _authToken.value

    /**
     * Check if the user is currently authenticated.
     */
    fun isAuthenticated(): Boolean = _authState.value == AuthState.AUTHENTICATED

    /**
     * Get the current authentication state.
     */
    fun getAuthState(): AuthState = _authState.value

    /**
     * Log out the user – clear token and reset state.
     */
    fun logout() {
        _authToken.value = null
        prefs.remove(KEY_AUTH_TOKEN)
        _authState.value = AuthState.UNAUTHENTICATED
    }

    /**
     * Clear the token without resetting state (e.g., for token expiration).
     */
    fun clearToken() {
        _authToken.value = null
        prefs.remove(KEY_AUTH_TOKEN)
        _authState.value = AuthState.EXPIRED
    }

    /**
     * Refresh the token (placeholder – extend with actual refresh logic).
     */
    suspend fun refreshToken(): Boolean {
        // In a real app, you would call a refresh endpoint with the current token.
        // For now, we just return true if we have a token.
        val token = _authToken.value
        if (!token.isNullOrBlank()) {
            return authenticate(token) // Re‑validate
        }
        return false
    }

    /**
     * Get the token for inclusion in the WebSocket URL or hello payload.
     */
    fun getTokenForConnection(): String? {
        return if (isAuthenticated()) _authToken.value else null
    }

    /**
     * Set a new token directly (e.g., after receiving it from a server response).
     */
    fun setToken(token: String) {
        _authToken.value = token
        prefs.putString(KEY_AUTH_TOKEN, token)
        _authState.value = AuthState.AUTHENTICATED
    }
}
