// app/src/main/java/com/airmouse/auth/AuthManager.kt
package com.airmouse.auth

import android.content.Context
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val prefs: PreferencesManager
) {
    private val _authState = MutableStateFlow(AuthState.UNAUTHENTICATED)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    enum class AuthState {
        UNAUTHENTICATED,
        AUTHENTICATING,
        AUTHENTICATED,
        ERROR
    }

    suspend fun authenticate(token: String): Boolean {
        _authState.value = AuthState.AUTHENTICATING
        return try {
            // Validate token with server
            val isValid = validateToken(token)
            if (isValid) {
                _authToken.value = token
                prefs.putString("auth_token", token)
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

    suspend fun validateToken(token: String): Boolean {
        // In production, validate with server
        return token.isNotEmpty()
    }

    fun logout() {
        _authToken.value = null
        prefs.remove("auth_token")
        _authState.value = AuthState.UNAUTHENTICATED
    }
}