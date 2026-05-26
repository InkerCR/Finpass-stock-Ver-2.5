package com.example.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object CloudSyncService {
    private const val TAG = "CloudSyncService"
    
    // We use a highly unique anonymous bucket ID on kvdb.io to prevent any overlap
    private const val BUCKET_ID = "wh_sync_v1_aifstock_bj5m1"
    private const val BASE_URL = "https://kvdb.io/$BUCKET_ID"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Upload database state (JSON format) to the cloud room.
     */
    suspend fun pushToCloud(roomId: String, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        val sanitizedRoomId = roomId.lowercase().trim().replace(Regex("[^a-z0-9_-]"), "")
        if (sanitizedRoomId.isEmpty()) return@withContext false

        val url = "$BASE_URL/$sanitizedRoomId"
        try {
            val body = jsonString.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Pushed to cloud room $sanitizedRoomId successfully.")
                    true
                } else {
                    Log.e(TAG, "Push failed: response code ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing to cloud: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Download database state (JSON format) from the cloud room.
     */
    suspend fun pullFromCloud(roomId: String): String? = withContext(Dispatchers.IO) {
        val sanitizedRoomId = roomId.lowercase().trim().replace(Regex("[^a-z0-9_-]"), "")
        if (sanitizedRoomId.isEmpty()) return@withContext null

        val url = "$BASE_URL/$sanitizedRoomId"
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    Log.d(TAG, "Pulled from cloud room $sanitizedRoomId successfully.")
                    body
                } else if (response.code == 404) {
                    Log.i(TAG, "Room $sanitizedRoomId does not exist on cloud yet (404).")
                    "EMPTY"
                } else {
                    Log.e(TAG, "Pull failed: response code ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling from cloud: ${e.localizedMessage}")
            null
        }
    }
}
