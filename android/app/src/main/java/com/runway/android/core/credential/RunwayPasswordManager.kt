package com.runway.android.core.credential

import android.content.Context
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential

class RunwayPasswordManager(context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun save(context: Context, email: String, password: String) {
        credentialManager.createCredential(
            context = context,
            request = CreatePasswordRequest(
                id = email,
                password = password,
            ),
        )
    }

    suspend fun get(context: Context): SavedPassword? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetPasswordOption())
            .build()
        val credential = credentialManager.getCredential(context, request).credential
        return (credential as? PasswordCredential)?.let {
            SavedPassword(email = it.id, password = it.password)
        }
    }
}

data class SavedPassword(
    val email: String,
    val password: String,
)
