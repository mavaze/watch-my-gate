package com.mavaze.mygate.auth

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

object GoogleScopes {
    const val CONTACTS_READONLY =
        "https://www.googleapis.com/auth/contacts.readonly"

    val required: List<Scope> = listOf(
        Scope("https://www.googleapis.com/auth/drive.file"),
        Scope(CONTACTS_READONLY)
    )
}

class GoogleAuthorizationRepository(
    context: Context
) {
    private val client =
        Identity.getAuthorizationClient(context)

    fun authorize() =
        client.authorize(
            AuthorizationRequest.builder()
                .setRequestedScopes(GoogleScopes.required)
                .build()
        )

    fun resultFromIntent(data: android.content.Intent?): AuthorizationResult =
        client.getAuthorizationResultFromIntent(data)
}
