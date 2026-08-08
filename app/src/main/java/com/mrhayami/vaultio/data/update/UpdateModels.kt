package com.mrhayami.vaultio.data.update

data class AvailableUpdate(
    val releaseId: Long,
    val assetId: Long,
    val tagName: String,
    val downloadUrl: String,
    val assetName: String,
    val publishedAt: String?
)

sealed class UpdateCheckResult {
    data object Unsupported : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data object NotModified : UpdateCheckResult()
    data class UpdateAvailable(val update: AvailableUpdate) : UpdateCheckResult()
    data class Error(val kind: UpdateErrorKind, val message: String) : UpdateCheckResult()
}

enum class UpdateErrorKind {
    NoReleases,
    RateLimited,
    VerificationFailed,
    Network,
    Generic
}
