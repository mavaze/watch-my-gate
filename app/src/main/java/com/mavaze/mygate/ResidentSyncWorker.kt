package com.mavaze.mygate

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mavaze.mygate.auth.GoogleDataSession
import com.mavaze.mygate.data.repository.GoogleDataRepository
import java.util.concurrent.TimeUnit

class ResidentSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val token = GoogleDataSession.accessToken ?: return Result.success()
        val societyId = GoogleDataSession.societyId ?: return Result.success()
        val db = (applicationContext as MyGateApplication).database
        val society = db.societyDao().findById(societyId) ?: return Result.success()
        if (!society.enabled) return Result.success()
        return GoogleDataRepository().synchronizeSociety(token, society.name, society.adminEmail).fold(
            onSuccess = { synced ->
                db.gateContactDao().replaceForSociety(societyId, synced.contacts.map {
                    com.mavaze.mygate.data.local.GateContact(
                        societyId = societyId,
                        googleResourceName = it.resourceName,
                        displayName = it.displayName,
                        phoneNumbersJson = org.json.JSONArray(it.phoneNumbers).toString(),
                        alias = it.alias,
                        priority = it.priority
                    )
                })
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        private const val UNIQUE_NAME = "mygate-resident-sync"
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ResidentSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
