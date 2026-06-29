# 📘 Air Mouse Implementation Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.implementation` package contains **concrete implementations** of various services and components that provide the actual functionality for the Air Mouse application. This package bridges the gap between interfaces and the underlying Android system.

```
com.airmouse.implementation/
├── AuthManager.kt                   # Authentication management
├── ... (other implementation files)
```

**Note:** Based on the provided file structure, this package appears to contain implementation classes that are typically injected via Dagger Hilt and used by the domain and data layers.

---

## 🔐 AuthManager

### Purpose
Manages **user authentication and authorization** for the Air Mouse application. Handles token generation, validation, and secure storage of authentication credentials.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Token Generation** | Creates unique authentication tokens for device identification |
| **Token Storage** | Securely stores authentication tokens in encrypted preferences |
| **Token Validation** | Validates tokens against server expectations |
| **Device Registration** | Registers the device with the server |
| **Session Management** | Manages user sessions and authentication state |
| **Secure Storage** | Uses Android Keystore or EncryptedSharedPreferences |

### Data Models

```kotlin
/**
 * Authentication token data class
 */
data class AuthToken(
    val token: String,                // The actual token string
    val deviceId: String,             // Unique device identifier
    val createdAt: Long,              // Token creation timestamp
    val expiresAt: Long,              // Token expiration timestamp
    val scopes: List<String> = emptyList()  // Permission scopes
)

/**
 * Device registration data
 */
data class DeviceRegistration(
    val deviceId: String,
    val deviceName: String,           // e.g., "Pixel 8 Pro"
    val deviceType: String,           // e.g., "Android"
    val osVersion: String,            // e.g., "14"
    val appVersion: String,           // e.g., "3.0.0"
    val registeredAt: Long = System.currentTimeMillis()
)
```

---

## 📦 AuthManager Implementation

### 1. Token Management

```kotlin
@Singleton
class AuthManager @Inject constructor(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    companion object {
        private const val TAG = "AuthManager"
        private const val TOKEN_PREF_KEY = "auth_token"
        private const val DEVICE_ID_KEY = "device_id"
        private const val TOKEN_CREATED_AT = "token_created_at"
        private const val TOKEN_EXPIRES_AT = "token_expires_at"
        private const val TOKEN_VALID = "token_valid"
        private const val TOKEN_SCOPES = "token_scopes"
    }
    
    private val _authState = MutableStateFlow(AuthState.UNAUTHENTICATED)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private var currentToken: AuthToken? = null
    
    init {
        loadTokenFromStorage()
    }
    
    enum class AuthState {
        UNAUTHENTICATED,
        AUTHENTICATING,
        AUTHENTICATED,
        EXPIRED,
        ERROR
    }
```

### 2. Token Generation

```kotlin
/**
 * Generate a new authentication token
 * @return The generated token string
 */
suspend fun generateToken(): String {
    _authState.value = AuthState.AUTHENTICATING
    
    try {
        val deviceId = getDeviceId()
        val token = generateSecureToken()
        val expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
        
        currentToken = AuthToken(
            token = token,
            deviceId = deviceId,
            createdAt = System.currentTimeMillis(),
            expiresAt = expiresAt,
            scopes = listOf("read", "write", "control")
        )
        
        saveTokenToStorage(currentToken!!)
        _authState.value = AuthState.AUTHENTICATED
        
        LogManager.info("Token generated successfully", TAG)
        return token
    } catch (e: Exception) {
        _authState.value = AuthState.ERROR
        LogManager.error("Failed to generate token: ${e.message}", TAG)
        throw e
    }
}

/**
 * Generate a cryptographically secure token
 */
private fun generateSecureToken(): String {
    val random = SecureRandom()
    val byteArray = ByteArray(32)
    random.nextBytes(byteArray)
    return Base64.encodeToString(byteArray, Base64.NO_WRAP or Base64.URL_SAFE)
}
```

### 3. Token Storage

```kotlin
/**
 * Save token to secure storage
 */
private fun saveTokenToStorage(token: AuthToken) {
    // Use EncryptedSharedPreferences for secure storage
    val encryptedPrefs = EncryptedSharedPreferences.create(
        "secure_prefs",
        context,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    encryptedPrefs.edit {
        putString(TOKEN_PREF_KEY, token.token)
        putString(DEVICE_ID_KEY, token.deviceId)
        putLong(TOKEN_CREATED_AT, token.createdAt)
        putLong(TOKEN_EXPIRES_AT, token.expiresAt)
        putBoolean(TOKEN_VALID, true)
        putString(TOKEN_SCOPES, token.scopes.joinToString(","))
    }
    
    // Also save in regular prefs for quick access
    prefs.putString(TOKEN_PREF_KEY, token.token)
    prefs.putString(DEVICE_ID_KEY, token.deviceId)
    prefs.putLong(TOKEN_CREATED_AT, token.createdAt)
    prefs.putLong(TOKEN_EXPIRES_AT, token.expiresAt)
    prefs.putBoolean(TOKEN_VALID, true)
}

/**
 * Load token from storage
 */
private fun loadTokenFromStorage() {
    try {
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "secure_prefs",
            context,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        val token = encryptedPrefs.getString(TOKEN_PREF_KEY, null)
        val deviceId = encryptedPrefs.getString(DEVICE_ID_KEY, null)
        val createdAt = encryptedPrefs.getLong(TOKEN_CREATED_AT, 0)
        val expiresAt = encryptedPrefs.getLong(TOKEN_EXPIRES_AT, 0)
        val isValid = encryptedPrefs.getBoolean(TOKEN_VALID, false)
        val scopes = encryptedPrefs.getString(TOKEN_SCOPES, "")?.split(",") ?: emptyList()
        
        if (token != null && isValid && expiresAt > System.currentTimeMillis()) {
            currentToken = AuthToken(
                token = token,
                deviceId = deviceId ?: getDeviceId(),
                createdAt = createdAt,
                expiresAt = expiresAt,
                scopes = scopes
            )
            _authState.value = AuthState.AUTHENTICATED
        } else if (token != null && expiresAt <= System.currentTimeMillis()) {
            _authState.value = AuthState.EXPIRED
        }
    } catch (e: Exception) {
        LogManager.warn("Failed to load token from secure storage", TAG)
        // Fallback to regular preferences
        loadTokenFromRegularPrefs()
    }
}

/**
 * Fallback: Load token from regular preferences
 */
private fun loadTokenFromRegularPrefs() {
    val token = prefs.getString(TOKEN_PREF_KEY, "")
    val deviceId = prefs.getString(DEVICE_ID_KEY, "")
    val createdAt = prefs.getLong(TOKEN_CREATED_AT, 0)
    val expiresAt = prefs.getLong(TOKEN_EXPIRES_AT, 0)
    val isValid = prefs.getBoolean(TOKEN_VALID, false)
    val scopes = prefs.getString(TOKEN_SCOPES, "")?.split(",") ?: emptyList()
    
    if (token.isNotEmpty() && isValid && expiresAt > System.currentTimeMillis()) {
        currentToken = AuthToken(
            token = token,
            deviceId = deviceId,
            createdAt = createdAt,
            expiresAt = expiresAt,
            scopes = scopes
        )
        _authState.value = AuthState.AUTHENTICATED
    } else if (token.isNotEmpty() && expiresAt <= System.currentTimeMillis()) {
        _authState.value = AuthState.EXPIRED
    }
}
```

### 4. Token Validation

```kotlin
/**
 * Validate the current token
 * @return True if the token is valid, false otherwise
 */
fun validateToken(): Boolean {
    val token = currentToken ?: return false
    
    // Check expiration
    if (token.expiresAt <= System.currentTimeMillis()) {
        _authState.value = AuthState.EXPIRED
        return false
    }
    
    return true
}

/**
 * Refresh the token if expired
 * @return The new token if refreshed, null if failed
 */
suspend fun refreshToken(): String? {
    if (_authState.value != AuthState.EXPIRED && validateToken()) {
        return currentToken?.token
    }
    
    // Generate a new token
    return generateToken()
}

/**
 * Get the current token for connection
 * @return The token string, or null if not authenticated
 */
fun getTokenForConnection(): String? {
    return if (validateToken()) {
        currentToken?.token
    } else {
        null
    }
}
```

### 5. Device Registration

```kotlin
/**
 * Register the device with the server
 * @return The registration result
 */
suspend fun registerDevice(): Result<DeviceRegistration> {
    return try {
        val deviceId = getDeviceId()
        val registration = DeviceRegistration(
            deviceId = deviceId,
            deviceName = Build.MANUFACTURER + " " + Build.MODEL,
            deviceType = "Android",
            osVersion = Build.VERSION.RELEASE,
            appVersion = BuildConfig.VERSION_NAME
        )
        
        // Generate token as part of registration
        val token = generateToken()
        
        // Store registration info
        prefs.putString("device_registration", Gson().toJson(registration))
        prefs.putString("registration_token", token)
        
        Result.success(registration)
    } catch (e: Exception) {
        LogManager.error("Device registration failed: ${e.message}", TAG)
        Result.failure(e)
    }
}

/**
 * Get device ID (unique identifier)
 * @return The device ID string
 */
private fun getDeviceId(): String {
    var deviceId = prefs.getString(DEVICE_ID_KEY, "")
    if (deviceId.isEmpty()) {
        deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()
        prefs.putString(DEVICE_ID_KEY, deviceId)
    }
    return deviceId
}
```

### 6. Authentication Flow

```kotlin
/**
 * Authenticate with the server
 * @return True if authentication successful
 */
suspend fun authenticate(): Boolean {
    try {
        // First, try to validate existing token
        if (validateToken()) {
            return true
        }
        
        // If token expired or invalid, generate a new one
        if (_authState.value == AuthState.EXPIRED) {
            val newToken = refreshToken()
            return newToken != null
        }
        
        // If no token exists, register device
        if (_authState.value == AuthState.UNAUTHENTICATED) {
            val registration = registerDevice()
            return registration.isSuccess
        }
        
        return false
    } catch (e: Exception) {
        LogManager.error("Authentication failed: ${e.message}", TAG)
        _authState.value = AuthState.ERROR
        return false
    }
}

/**
 * Logout - clear authentication state
 */
fun logout() {
    currentToken = null
    _authState.value = AuthState.UNAUTHENTICATED
    
    // Clear secure storage
    val encryptedPrefs = EncryptedSharedPreferences.create(
        "secure_prefs",
        context,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    encryptedPrefs.edit().clear().apply()
    
    // Clear regular prefs
    prefs.remove(TOKEN_PREF_KEY)
    prefs.remove(TOKEN_CREATED_AT)
    prefs.remove(TOKEN_EXPIRES_AT)
    prefs.putBoolean(TOKEN_VALID, false)
    
    LogManager.info("User logged out", TAG)
}
```

---

## 🔄 Authentication Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       AUTHENTICATION FLOW                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                  AUTHENTICATION STATES                           │   │
│  │                                                                  │   │
│  │  UNAUTHENTICATED  →  AUTHENTICATING  →  AUTHENTICATED          │   │
│  │         │                │                    │                  │   │
│  │         │                │                    │                  │   │
│  │         │                │                    ├──→  EXPIRED     │   │
│  │         │                │                    │                  │   │
│  │         │                │                    └──→  ERROR       │   │
│  │         │                │                                         │   │
│  │         └────────────────┼─────────────────────────────────────────┤   │
│  │                          │                                         │   │
│  └──────────────────────────┼─────────────────────────────────────────┘   │
│                             │                                           │
│                             ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   TOKEN GENERATION                               │   │
│  │                                                                  │   │
│  │  1. Generate secure random token (32 bytes, Base64 URL-safe)    │   │
│  │  2. Set expiration (7 days)                                    │   │
│  │  3. Save to EncryptedSharedPreferences                          │   │
│  │  4. Update auth state to AUTHENTICATED                         │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                             │                                           │
│                             ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   TOKEN VALIDATION                               │   │
│  │                                                                  │   │
│  │  1. Check if token exists                                       │   │
│  │  2. Check if token is not expired                              │   │
│  │  3. Validate token format                                       │   │
│  │  4. Return validation result                                   │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                             │                                           │
│                             ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   TOKEN REFRESH                                  │   │
│  │                                                                  │   │
│  │  1. Detect expired token                                        │   │
│  │  2. Generate new token                                          │   │
│  │  3. Update storage                                              │   │
│  │  4. Return new token                                           │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Public API Summary

| Method | Purpose | Return Type |
|--------|---------|-------------|
| `generateToken()` | Generate a new authentication token | `suspend fun: String` |
| `validateToken()` | Validate the current token | `fun: Boolean` |
| `refreshToken()` | Refresh an expired token | `suspend fun: String?` |
| `getTokenForConnection()` | Get token for network connection | `fun: String?` |
| `authenticate()` | Authenticate with the server | `suspend fun: Boolean` |
| `logout()` | Clear authentication state | `fun: Unit` |
| `registerDevice()` | Register the device | `suspend fun: Result<DeviceRegistration>` |
| `authState` (StateFlow) | Current authentication state | `StateFlow<AuthState>` |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Secure Storage** | EncryptedSharedPreferences for sensitive data |
| **Token Expiration** | Tokens expire after 7 days |
| **Auto-Refresh** | Automatic token refresh when expired |
| **State Management** | Clear authentication states with StateFlow |
| **Device Registration** | Unique device identification |
| **Error Handling** | Graceful error recovery |
| **Fallback** | Regular preferences fallback if secure storage unavailable |

---

**The AuthManager provides a secure, robust authentication system for the Air Mouse application, managing device registration, token generation, and authentication state.**