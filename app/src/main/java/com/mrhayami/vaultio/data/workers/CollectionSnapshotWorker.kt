package com.mrhayami.vaultio.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mrhayami.vaultio.VaultioApplication
import com.mrhayami.vaultio.data.repository.VaultioRepository

class CollectionSnapshotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository: VaultioRepository =
        (applicationContext as VaultioApplication).repository

    override suspend fun doWork(): Result {
        Log.d("CollectionSnapshotWorker", "Taking daily collection snapshot...")
        return try {
            repository.takeSnapshot()
            Log.d("CollectionSnapshotWorker", "Snapshot taken successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("CollectionSnapshotWorker", "Failed to take snapshot", e)
            Result.retry()
        }
    }
}
