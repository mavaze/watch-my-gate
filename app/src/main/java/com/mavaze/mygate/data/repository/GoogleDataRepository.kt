package com.mavaze.mygate.data.repository

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GoogleSyncResult(
    val societyName: String,
    val contacts: List<GoogleContact>
)

data class GoogleContact(
    val resourceName: String,
    val displayName: String,
    val phoneNumbers: List<String>,
    val alias: String?,
    val priority: Int
)

class GoogleDataRepository {

    companion object {
        private const val DRIVE_FILES =
            "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_FILES =
            "https://www.googleapis.com/upload/drive/v3/files"
        private const val PEOPLE_CONNECTIONS =
            "https://people.googleapis.com/v1/people/me/connections"
        private const val PEOPLE_CONTACT_GROUPS =
            "https://people.googleapis.com/v1/contactGroups"
        private const val DEFAULT_CONTACT_GROUP =
            "Resident"
        private const val MY_GATE_ALIAS_KEY =
            "MyGateAlias"
        private const val MY_GATE_PRIORITY_KEY =
            "MyGatePriority"
        private const val METADATA_FILE =
            "MyGate-society.json"
        private const val METADATA_MIME =
            "application/json"
    }

    suspend fun discoverSociety(
        accessToken: String,
        adminEmail: String
    ): Result<String?> {
        return try {
            val fileId = findMetadataFile(accessToken)
            if (fileId == null) {
                Result.success(null)
            } else {
                val json = JSONObject(
                    downloadDriveFile(accessToken, fileId)
                )
                val storedEmail =
                    json.optString("adminEmail")
                        .trim()
                        .lowercase()

                if (
                    storedEmail.isNotBlank() &&
                    storedEmail != adminEmail.trim().lowercase()
                ) {
                    Result.failure(
                        IllegalStateException(
                            "The Google Drive society metadata belongs to a different Gmail account."
                        )
                    )
                } else {
                    Result.success(
                        json.optString("name")
                            .takeIf { it.isNotBlank() }
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun synchronizeSociety(
        accessToken: String,
        localName: String,
        adminEmail: String
    ): Result<GoogleSyncResult> = withContext(Dispatchers.IO){
         try {
            val metadata = loadOrCreateMetadata(
                accessToken,
                localName,
                adminEmail
            )

            val contacts =
                loadContacts(accessToken)

            Result.success(
                GoogleSyncResult(
                    societyName =
                        metadata.optString("name", localName),
                    contacts = contacts
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSocietyMetadata(
        accessToken: String,
        name: String,
        adminEmail: String,
        logoPath: String?
    ): Result<Unit> = withContext(Dispatchers.IO){
        try {
            val existing = findMetadataFile(accessToken)

            val json = JSONObject()
                .put("schemaVersion", 1)
                .put("name", name.trim())
                .put("adminEmail", adminEmail.trim().lowercase())
                .put("logoPath", logoPath ?: JSONObject.NULL)
                .put("updatedAt", System.currentTimeMillis())

            if (existing == null) {
                createDriveFile(accessToken, json)
            } else {
                updateDriveFile(
                    accessToken,
                    existing,
                    json
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadOrCreateMetadata(
        token: String,
        localName: String,
        adminEmail: String
    ): JSONObject {
        val existing = findMetadataFile(token)

        if (existing != null) {
            val content = downloadDriveFile(
                token,
                existing
            )
            return JSONObject(content)
        }

        val json = JSONObject()
            .put("schemaVersion", 1)
            .put("name", localName.trim())
            .put("adminEmail", adminEmail.trim().lowercase())
            .put("logoPath", JSONObject.NULL)
            .put("updatedAt", System.currentTimeMillis())

        createDriveFile(token, json)
        return json
    }

    private fun findMetadataFile(
        token: String
    ): String? {
        val query =
            "name = '$METADATA_FILE' and trashed = false"

        val url =
            "$DRIVE_FILES?q=" +
                URLEncoder.encode(query, "UTF-8") +
                "&pageSize=1&fields=files(id,name)"

        val json = request(
            method = "GET",
            url = url,
            token = token
        )

        val files = json.optJSONArray("files")
            ?: return null

        return if (files.length() == 0) {
            null
        } else {
            files.getJSONObject(0).getString("id")
        }
    }

    private fun createDriveFile(
        token: String,
        json: JSONObject
    ): String {
        val metadata = JSONObject()
            .put("name", METADATA_FILE)
            .put("mimeType", METADATA_MIME)

        val boundary =
            "MyGateBoundary${System.currentTimeMillis()}"

        val body = multipartBody(
            boundary,
            metadata.toString(),
            json.toString()
        )

        val connection =
            openConnection(
                "$DRIVE_UPLOAD_FILES?uploadType=multipart&fields=id",
                token
            )

        connection.requestMethod = "POST"
        connection.setRequestProperty(
            "Content-Type",
            "multipart/related; boundary=$boundary"
        )
        connection.doOutput = true
        connection.outputStream.use {
            it.write(body)
        }

        return readJson(connection)
            .getString("id")
    }

    private fun updateDriveFile(
        token: String,
        fileId: String,
        json: JSONObject
    ) {
        val url =
            "$DRIVE_UPLOAD_FILES/$fileId?uploadType=media"

        val connection =
            openConnection(url, token)

        connection.requestMethod = "PATCH"
        connection.setRequestProperty(
            "Content-Type",
            METADATA_MIME
        )
        connection.doOutput = true
        connection.outputStream.use {
            it.write(json.toString().toByteArray())
        }

        readJson(connection)
    }

    private fun downloadDriveFile(
        token: String,
        fileId: String
    ): String {
        val url =
            "$DRIVE_FILES/$fileId?alt=media"

        val connection =
            openConnection(url, token)

        connection.requestMethod = "GET"

        return readText(connection)
    }

    private fun loadContacts(
        token: String
    ): List<GoogleContact> {
        Log.d("MyGateContacts", "Looking for Google Contacts label '$DEFAULT_CONTACT_GROUP'")

        val residentGroup = findContactGroup(
            token,
            DEFAULT_CONTACT_GROUP
        ) ?: throw IllegalStateException(
            "Google Contacts label 'Resident' was not found."
        )

        Log.d("MyGateContacts", "Resident label found: resourceName=$residentGroup")

        val memberResourceNames =
            getContactGroupMembers(
                token,
                residentGroup
            ).toSet()

        Log.d(
            "MyGateContacts",
            "Resident label members=${memberResourceNames.size}"
        )

        if (memberResourceNames.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<GoogleContact>()
        var pageToken: String? = null
        var pageNumber = 0
        var connectionCount = 0
        var matchedCount = 0

        do {
            pageNumber++
            var url =
                PEOPLE_CONNECTIONS +
                    "?personFields=names,phoneNumbers,userDefined,memberships" +
                    "&pageSize=1000"

            if (!pageToken.isNullOrBlank()) {
                url += "&pageToken=" +
                    URLEncoder.encode(pageToken, "UTF-8")
            }

            val json = request("GET", url, token)
            val connections =
                json.optJSONArray("connections")

            if (connections != null) {
                connectionCount += connections.length()
                Log.d(
                    "MyGateContacts",
                    "People connections page=$pageNumber count=${connections.length()}"
                )
                for (i in 0 until connections.length()) {
                    val person =
                        connections.getJSONObject(i)

                    val resourceName =
                        person.optString("resourceName")

                    if (!memberResourceNames.contains(resourceName)) {
                        continue
                    }

                    matchedCount++

                    val names =
                        person.optJSONArray("names")

                    val displayName =
                        names?.optJSONObject(0)
                            ?.optString("displayName")
                            ?.takeIf { it.isNotBlank() }
                            ?: ""

                    val phones = mutableListOf<String>()
                    val phoneArray =
                        person.optJSONArray("phoneNumbers")

                    if (phoneArray != null) {
                        for (j in 0 until phoneArray.length()) {
                            phoneArray
                                .optJSONObject(j)
                                ?.optString("value")
                                ?.takeIf { it.isNotBlank() }
                                ?.let(phones::add)
                        }
                    }

                    val userDefined =
                        person.optJSONArray("userDefined")

                    var alias: String? = null
                    var priority: Int? = null

                    if (userDefined != null) {
                        for (j in 0 until userDefined.length()) {
                            val field =
                                userDefined.optJSONObject(j)
                                    ?: continue

                            val key =
                                field.optString("key")
                                    .trim()

                            val value =
                                field.optString("value")
                                    .trim()

                            Log.d(
                                "MyGateContacts",
                                "userDefined field: key='$key', value='$value'"
                            )

                            when (value.lowercase()) {
                                MY_GATE_ALIAS_KEY.lowercase() -> {
                                    alias =
                                        key.takeIf {
                                            it.isNotBlank()
                                        }
                                }

                                MY_GATE_PRIORITY_KEY.lowercase() -> {
                                    priority =
                                        key.toIntOrNull()
                                }
                            }
                        }
                    }

                    result += GoogleContact(
                        resourceName = resourceName,
                        displayName = displayName,
                        phoneNumbers = phones,
                        alias = alias,
                        priority = priority ?: 0
                    )

                    Log.d(
                        "MyGateContacts",
                        "Matched resident name='$displayName', alias='$alias', priority=${priority ?: 0}, phones=${phones.size}"
                    )
                }
            }

            pageToken =
                json.optString("nextPageToken")
                    .takeIf { it.isNotBlank() }

        } while (pageToken != null)

        Log.d(
            "MyGateContacts",
            "Contact sync result: pages=$pageNumber, connections=$connectionCount, groupMembers=${memberResourceNames.size}, matched=$matchedCount, returned=${result.size}"
        )

        return result
    }

    private fun findContactGroup(
        token: String,
        groupName: String
    ): String? {
        var pageToken: String? = null

        do {
            var url =
                "$PEOPLE_CONTACT_GROUPS" +
                        "?pageSize=1000&groupFields=name"

            if (!pageToken.isNullOrBlank()) {
                url += "&pageToken=" +
                        URLEncoder.encode(pageToken, "UTF-8")
            }

            val json = request("GET", url, token)

            val groups =
                json.optJSONArray("contactGroups")

            if (groups != null) {
                for (i in 0 until groups.length()) {
                    val group = groups.optJSONObject(i)
                        ?: continue

                    if (
                        group.optString("name")
                            .trim()
                            .equals(groupName, ignoreCase = true)
                    ) {
                        return group.optString("resourceName")
                            .takeIf { it.isNotBlank() }
                    }
                }
            }

            pageToken =
                json.optString("nextPageToken")
                    .takeIf { it.isNotBlank() }

        } while (pageToken != null)

        return null
    }

    private fun getContactGroupMembers(
        token: String,
        resourceName: String
    ): List<String> {

        val url =
            "https://people.googleapis.com/v1/" +
                    resourceName +
                    "?maxMembers=1000"

        Log.d(
            "MyGateGoogle",
            "Getting contact group: $url"
        )

        Log.d(
            "MyGateGoogle",
            "Resident group resourceName = '$resourceName'"
        )
        Log.d(
            "MyGateGoogle",
            "Resident group URL = '$url'"
        )

        val json =
            request("GET", url, token)

        Log.d(
            "MyGateContacts",
            "Resident group response keys=${json.keys().asSequence().toList()}"
        )

        val members =
            json.optJSONArray("memberResourceNames")
                ?: return emptyList()

        return buildList {
            for (i in 0 until members.length()) {
                members.optString(i)
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }

    private fun multipartBody(
        boundary: String,
        metadata: String,
        content: String
    ): ByteArray {
        val separator = "\r\n"
        val body =
            "--$boundary$separator" +
                "Content-Type: application/json; charset=UTF-8$separator$separator" +
                metadata +
                separator +
                "--$boundary$separator" +
                "Content-Type: application/json$separator$separator" +
                content +
                separator +
                "--$boundary--$separator"

        return body.toByteArray(Charsets.UTF_8)
    }

    private fun request(
        method: String,
        url: String,
        token: String
    ): JSONObject {
        val connection =
            openConnection(url, token)

        connection.requestMethod = method

        return readJson(connection)
    }

    private fun openConnection(
        url: String,
        token: String
    ): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty(
                "Authorization",
                "Bearer $token"
            )
            setRequestProperty(
                "Accept",
                "application/json"
            )
        }
    }

    private fun readJson(
        connection: HttpURLConnection
    ): JSONObject {
        return JSONObject(readText(connection))
    }

    private fun readText(
        connection: HttpURLConnection
    ): String {
        val code = connection.responseCode

        val stream =
            if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val text =
            BufferedReader(
                InputStreamReader(
                    stream ?: throw IllegalStateException(
                        "Google API returned HTTP $code"
                    )
                )
            ).use { reader ->
                reader.readText()
            }

        if (code !in 200..299) {
            throw IllegalStateException(
                "Google API returned HTTP $code: $text"
            )
        }

        return text
    }
}
