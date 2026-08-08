package com.mrhayami.vaultio.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.mrhayami.vaultio.BuildConfig
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.remote.GitHubRelease
import com.mrhayami.vaultio.data.remote.GitHubReleasesApi
import com.mrhayami.vaultio.data.update.ApkDownloader
import com.mrhayami.vaultio.data.update.ApkInstaller
import com.mrhayami.vaultio.data.update.ApkVerificationResult
import com.mrhayami.vaultio.data.update.ApkVerifier
import com.mrhayami.vaultio.data.update.AvailableUpdate
import com.mrhayami.vaultio.data.update.PlayInstallDetector
import com.mrhayami.vaultio.data.update.UpdateAssetPicker
import com.mrhayami.vaultio.data.update.UpdateChannel
import com.mrhayami.vaultio.data.update.UpdateCheckResult
import com.mrhayami.vaultio.data.update.UpdateErrorKind
import com.mrhayami.vaultio.data.update.UpdateVersionCompare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.Instant

class AppUpdateRepository(
    private val context: Context,
    private val gitHubReleasesApi: GitHubReleasesApi,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val checkMutex = Mutex()
    private val downloader = ApkDownloader(okHttpClient)

    val channel: UpdateChannel = UpdateChannel.fromBuildType(BuildConfig.BUILD_TYPE)

    fun isPlayInstall(): Boolean = PlayInstallDetector.isPlayInstall(context)

    fun isUpdaterSupported(): Boolean = channel != UpdateChannel.NONE && !isPlayInstall()

    fun canInstallPackages(): Boolean = ApkInstaller.canInstallPackages(context)

    fun unknownSourcesSettingsIntent(): Intent = ApkInstaller.unknownSourcesSettingsIntent(context)

    fun pendingApkFile(): File = ApkInstaller.updateApkFile(context)

    fun hasVerifiedPendingApk(): Boolean {
        val file = pendingApkFile()
        return file.exists() &&
            ApkVerifier.verify(context, file) is ApkVerificationResult.Ok
    }

    suspend fun checkForUpdate(force: Boolean = false): UpdateCheckResult = checkMutex.withLock {
        if (!isUpdaterSupported()) return UpdateCheckResult.Unsupported

        if (!force) {
            val lastCheck = userPreferencesRepository.lastUpdateCheck.first()
            if (lastCheck > 0L && System.currentTimeMillis() - lastCheck < CHECK_THROTTLE_MS) {
                return UpdateCheckResult.NotModified
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                fetchAndCompare()
            } catch (e: HttpException) {
                mapHttpError(e.code())
            } catch (e: IOException) {
                UpdateCheckResult.Error(UpdateErrorKind.Network, e.message ?: "Network error")
            } catch (e: Exception) {
                UpdateCheckResult.Error(UpdateErrorKind.Generic, e.message ?: "Update check failed")
            } finally {
                userPreferencesRepository.setLastUpdateCheck(System.currentTimeMillis())
            }
        }
    }

    private suspend fun fetchAndCompare(): UpdateCheckResult {
        val userAgent = userAgent()
        val etag = userPreferencesRepository.updateEtag.first()
        val response = when (channel) {
            UpdateChannel.RELEASE -> gitHubReleasesApi.getLatestRelease(
                owner = REPO_OWNER,
                repo = REPO_NAME,
                userAgent = userAgent,
                ifNoneMatch = etag
            )
            UpdateChannel.NIGHTLY -> gitHubReleasesApi.getReleaseByTag(
                owner = REPO_OWNER,
                repo = REPO_NAME,
                tag = NIGHTLY_TAG,
                userAgent = userAgent,
                ifNoneMatch = etag
            )
            UpdateChannel.NONE -> return UpdateCheckResult.Unsupported
        }

        when (response.code()) {
            304 -> return UpdateCheckResult.NotModified
            404 -> return UpdateCheckResult.Error(
                UpdateErrorKind.NoReleases,
                "No releases published yet"
            )
            403, 429 -> return UpdateCheckResult.Error(
                UpdateErrorKind.RateLimited,
                "GitHub rate limit reached. Try again later."
            )
        }

        if (!response.isSuccessful) {
            return mapHttpError(response.code())
        }

        val release = response.body()
            ?: return UpdateCheckResult.Error(UpdateErrorKind.Generic, "Empty release response")

        response.headers()["etag"]?.let { userPreferencesRepository.setUpdateEtag(it) }

        if (release.draft) {
            return UpdateCheckResult.Error(UpdateErrorKind.NoReleases, "No published release found")
        }

        val asset = UpdateAssetPicker.pickApkAsset(release.assets, channel)
            ?: return UpdateCheckResult.Error(
                UpdateErrorKind.Generic,
                "No matching APK asset on the release"
            )

        val available = AvailableUpdate(
            releaseId = release.id,
            assetId = asset.id,
            tagName = release.tagName,
            downloadUrl = asset.browserDownloadUrl,
            assetName = asset.name,
            publishedAt = release.publishedAt
        )

        return when (channel) {
            UpdateChannel.RELEASE -> evaluateRelease(available)
            UpdateChannel.NIGHTLY -> evaluateNightly(release, available)
            UpdateChannel.NONE -> UpdateCheckResult.Unsupported
        }
    }

    private fun evaluateRelease(update: AvailableUpdate): UpdateCheckResult {
        return if (UpdateVersionCompare.isRemoteReleaseNewer(update.tagName, BuildConfig.VERSION_NAME)) {
            UpdateCheckResult.UpdateAvailable(update)
        } else {
            clearPendingApk()
            UpdateCheckResult.UpToDate
        }
    }

    private suspend fun evaluateNightly(
        release: GitHubRelease,
        update: AvailableUpdate
    ): UpdateCheckResult {
        val acceptedReleaseId = userPreferencesRepository.lastAcceptedReleaseId.first()
        val acceptedAssetId = userPreferencesRepository.lastAcceptedAssetId.first()

        if (acceptedReleaseId == release.id && acceptedAssetId == update.assetId) {
            clearPendingApk()
            return UpdateCheckResult.UpToDate
        }

        if (acceptedReleaseId == 0L && acceptedAssetId == 0L) {
            val publishedMs = parseIsoInstant(release.publishedAt)
            val installedMs = packageLastUpdateTime()
            if (publishedMs != null && installedMs != null && publishedMs <= installedMs + INSTALL_GRACE_MS) {
                userPreferencesRepository.setLastAcceptedReleaseIdentity(release.id, update.assetId)
                clearPendingApk()
                return UpdateCheckResult.UpToDate
            }
        }

        return UpdateCheckResult.UpdateAvailable(update)
    }

    suspend fun downloadUpdate(
        update: AvailableUpdate,
        onProgress: ((bytesRead: Long, contentLength: Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dest = pendingApkFile()
            downloader.download(
                url = update.downloadUrl,
                destination = dest,
                userAgent = userAgent(),
                onProgress = onProgress
            )
            when (val verification = ApkVerifier.verify(context, dest)) {
                ApkVerificationResult.Ok -> Result.success(dest)
                is ApkVerificationResult.Failed -> {
                    dest.delete()
                    Result.failure(IOException(verification.reason))
                }
            }
        } catch (e: Exception) {
            pendingApkFile().delete()
            Result.failure(e)
        }
    }

    fun createInstallIntent(): Intent? {
        val file = pendingApkFile()
        if (!file.exists()) return null
        if (ApkVerifier.verify(context, file) !is ApkVerificationResult.Ok) {
            file.delete()
            return null
        }
        return ApkInstaller.createInstallIntent(context, file)
    }

    suspend fun markAccepted(releaseId: Long, assetId: Long) {
        userPreferencesRepository.setLastAcceptedReleaseIdentity(releaseId, assetId)
    }

    suspend fun markNotified(releaseId: Long, assetId: Long) {
        userPreferencesRepository.setLastNotifiedReleaseIdentity(releaseId, assetId)
    }

    suspend fun wasAlreadyNotified(releaseId: Long, assetId: Long): Boolean {
        val notifiedRelease = userPreferencesRepository.lastNotifiedReleaseId.first()
        val notifiedAsset = userPreferencesRepository.lastNotifiedAssetId.first()
        return notifiedRelease == releaseId && notifiedAsset == assetId
    }

    fun clearPendingApk() {
        pendingApkFile().delete()
        File(pendingApkFile().parentFile, "${pendingApkFile().name}.partial").delete()
    }

    private fun userAgent(): String = "Vaultio/${BuildConfig.VERSION_NAME}"

    private fun mapHttpError(code: Int): UpdateCheckResult = when (code) {
        404 -> UpdateCheckResult.Error(UpdateErrorKind.NoReleases, "No releases published yet")
        403, 429 -> UpdateCheckResult.Error(
            UpdateErrorKind.RateLimited,
            "GitHub rate limit reached. Try again later."
        )
        else -> UpdateCheckResult.Error(UpdateErrorKind.Generic, "GitHub error HTTP $code")
    }

    private fun packageLastUpdateTime(): Long? {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.lastUpdateTime
        } catch (_: Exception) {
            null
        }
    }

    private fun parseIsoInstant(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val REPO_OWNER = "mFontecchio"
        const val REPO_NAME = "vaultio"
        const val NIGHTLY_TAG = "nightly"
        const val CHECK_THROTTLE_MS = 24L * 60L * 60L * 1000L
        private const val INSTALL_GRACE_MS = 5L * 60L * 1000L
        const val WORK_NAME = "app_update_check"
    }
}
