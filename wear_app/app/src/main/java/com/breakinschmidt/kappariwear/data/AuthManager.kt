package com.breakinschmidt.kappariwear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AuthManager(private val context: Context) {
    companion object {
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val jwtToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[JWT_TOKEN_KEY]
        }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[JWT_TOKEN_KEY] = token
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN_KEY)
        }
    }

    fun saveCredentials(email: String, pass: String) {
        securePrefs.edit()
            .putString("email", email)
            .putString("password", pass)
            .apply()
    }

    fun getEmail(): String? = securePrefs.getString("email", null)
    fun getPassword(): String? = securePrefs.getString("password", null)
    
    fun clearCredentials() {
        securePrefs.edit()
            .remove("email")
            .remove("password")
            .apply()
    }
}
