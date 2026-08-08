package com.mrhayami.vaultio.data.update

import com.mrhayami.vaultio.data.remote.GitHubAsset

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null
) : Comparable<SemVer> {
    val isPreRelease: Boolean get() = !preRelease.isNullOrBlank()

    override fun compareTo(other: SemVer): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        patch.compareTo(other.patch).let { if (it != 0) return it }
        return when {
            preRelease == null && other.preRelease == null -> 0
            preRelease == null -> 1
            other.preRelease == null -> -1
            else -> preRelease.compareTo(other.preRelease)
        }
    }
}

object UpdateVersionCompare {
    private val SEMVER_REGEX = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?$""")

    /** Strips build-type suffixes like `-nightly` / `-debug` before parsing. */
    fun normalizeVersionName(versionName: String): String {
        return versionName
            .removeSuffix("-nightly")
            .removeSuffix("-debug")
            .trim()
    }

    fun parseSemVer(raw: String): SemVer? {
        val match = SEMVER_REGEX.matchEntire(normalizeVersionName(raw).trim()) ?: return null
        return SemVer(
            major = match.groupValues[1].toInt(),
            minor = match.groupValues[2].toInt(),
            patch = match.groupValues[3].toInt(),
            preRelease = match.groupValues[4].ifBlank { null }
        )
    }

    /**
     * True when [remoteTag] is a strictly newer stable (non-pre-release) release than [localVersionName].
     * Pre-release remote tags are never considered newer than a non-pre-release local build.
     */
    fun isRemoteReleaseNewer(remoteTag: String, localVersionName: String): Boolean {
        val remote = parseSemVer(remoteTag) ?: return false
        val local = parseSemVer(localVersionName) ?: return false
        if (remote.isPreRelease && !local.isPreRelease) return false
        return remote > local
    }
}

object UpdateAssetPicker {
    /**
     * Prefer exactly one canonical asset; else exactly one legacy asset.
     * Rejects drafts (caller), zero-size, and ambiguous multi-matches.
     */
    fun pickApkAsset(
        assets: List<GitHubAsset>,
        channel: UpdateChannel
    ): GitHubAsset? {
        val canonical = channel.canonicalAssetName ?: return null
        val legacy = channel.legacyAssetName ?: return null

        val canonicalMatches = assets.filter { it.name == canonical && it.size > 0L }
        when {
            canonicalMatches.size == 1 -> return canonicalMatches.first()
            canonicalMatches.size > 1 -> return null
        }

        val legacyMatches = assets.filter { it.name == legacy && it.size > 0L }
        return when {
            legacyMatches.size == 1 -> legacyMatches.first()
            else -> null
        }
    }
}
