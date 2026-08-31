package com.mavaze.mygate.auth

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

data class GoogleUser(
    val email: String,
    val displayName: String?,
    val profilePictureUri: String?,
    val idToken: String
)

class GoogleAuthRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val credentialManager =
        CredentialManager.create(appContext)

    suspend fun signIn(
        expectedEmail: String
    ): Result<GoogleUser> =
        withContext(Dispatchers.Main) {
            try {
                val normalizedExpectedEmail =
                    expectedEmail.trim().lowercase()

                val serverClientId =
                    appContext.getString(
                        com.mavaze.mygate.R.string.server_client_id
                    )

                val googleOption =
                    GetSignInWithGoogleOption.Builder(serverClientId)
                        .setNonce(generateNonce())
                        .build()

                val request =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(googleOption)
                        .build()

                val result =
                    credentialManager.getCredential(
                        appContext,
                        request
                    )

                val credential = result.credential

                if (
                    credential !is CustomCredential ||
                    credential.type !=
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    return@withContext Result.failure(
                        IllegalStateException("Unexpected Google credential type")
                    )
                }

                val googleCredential =
                    try {
                        GoogleIdTokenCredential.createFrom(
                            credential.data
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        return@withContext Result.failure(
                            IllegalStateException(
                                "Unable to parse Google credential",
                                e
                            )
                        )
                    }

                android.util.Log.d(
                    "MyGateGoogle",
                    "Google credential id = '${googleCredential.id}'"
                )

                android.util.Log.d(
                    "MyGateGoogle",
                    "Google credential all = '${googleCredential}'"
                )

                val returnedEmail =
                    googleCredential.id.trim().lowercase()

                if (returnedEmail != normalizedExpectedEmail) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "The selected Google account " +
                                "($returnedEmail) does not match " +
                                "the registered society account."
                        )
                    )
                }

                Result.success(
                    GoogleUser(
                        email = returnedEmail,
                        displayName = googleCredential.displayName,
                        profilePictureUri =
                            googleCredential.profilePictureUri?.toString(),
                        idToken = googleCredential.idToken
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)

        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }
}
