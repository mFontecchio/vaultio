package com.mrhayami.vaultio.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mrhayami.vaultio.VaultioApplication
import com.mrhayami.vaultio.data.repository.AppUpdateRepository
import com.mrhayami.vaultio.data.update.UpdateCheckResult
import com.mrhayami.vaultio.data.update.UpdateErrorKind
import com.mrhayami.vaultio.data.update.UpdateNotificationHelper
import kotlinx.coroutines.flow.first

class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? VaultioApplication ?: return Result.failure()
        val updateRepository = app.appUpdateRepository
        val prefs = app.userPreferencesRepository

        if (!prefs.autoUpdateEnabled.first()) {
            return Result.success()
        }
        if (!updateRepository.isUpdaterSupported()) {
            return Result.success()
        }

        return try {
            when (val check = updateRepository.checkForUpdate(force = true)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    if (updateRepository.wasAlreadyNotified(
                            check.update.releaseId,
                            check.update.assetId
                        ) && updateRepository.hasVerifiedPendingApk()
                    ) {
                        return Result.success()
                    }
                    val download = updateRepository.downloadUpdate(check.update)
                    if (download.isFailure) {
                        val message = download.exceptionOrNull()?.message.orEmpty()
                        return if (isVerificationFailure(message)) {
                            Result.failure()
                        } else {
                            Result.retry()
                        }
                    }
                    if (!updateRepository.hasVerifiedPendingApk()) {
                        return Result.failure()
                    }
                    UpdateNotificationHelper.notifyUpdateReady(
                        context = applicationContext,
                        tagName = check.update.tagName
                    )
                    updateRepository.markNotified(check.update.releaseId, check.update.assetId)
                    Result.success()
                }
                is UpdateCheckResult.Error -> when (check.kind) {
                    UpdateErrorKind.RateLimited, UpdateErrorKind.Network -> Result.retry()
                    UpdateErrorKind.VerificationFailed -> Result.failure()
                    else -> Result.success()
                }
                else -> Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "App update worker failed", e)
            Result.retry()
        }
    }

    private fun isVerificationFailure(message: String): Boolean {
        val lower = message.lowercase()
        return "certificate" in lower ||
            "package mismatch" in lower ||
            "downgrade" in lower ||
            "metadata" in lower
    }

    companion object {
        private const val TAG = "AppUpdateWorker"
    }
}
