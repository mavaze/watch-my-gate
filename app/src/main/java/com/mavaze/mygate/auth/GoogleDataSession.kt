package com.mavaze.mygate.auth

/**
 * Short-lived in-memory Google access token.
 *
 * Access tokens are deliberately not persisted in Room or preferences.
 * Google Identity Services caches authorization and can issue a fresh
 * access token when the app needs it.
 */
object GoogleDataSession {
    var accessToken: String? = null
        private set

    var societyId: Long? = null
        private set

    fun set(
        societyId: Long,
        accessToken: String
    ) {
        this.societyId = societyId
        this.accessToken = accessToken
    }

    fun clear() {
        accessToken = null
        societyId = null
    }
}
