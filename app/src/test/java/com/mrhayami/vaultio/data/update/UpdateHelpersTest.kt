package com.mrhayami.vaultio.data.update

import com.mrhayami.vaultio.data.remote.GitHubAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateHelpersTest {

    @Test
    fun `parseSemVer strips nightly suffix and v prefix`() {
        val version = UpdateVersionCompare.parseSemVer("v1.2.7-nightly")
        assertNotNull(version)
        assertEquals(1, version!!.major)
        assertEquals(2, version.minor)
        assertEquals(7, version.patch)
        assertNull(version.preRelease)
    }

    @Test
    fun `parseSemVer keeps pre-release label`() {
        val version = UpdateVersionCompare.parseSemVer("1.2.8-rc1")
        assertNotNull(version)
        assertTrue(version!!.isPreRelease)
        assertEquals("rc1", version.preRelease)
    }

    @Test
    fun `isRemoteReleaseNewer requires higher semver`() {
        assertTrue(UpdateVersionCompare.isRemoteReleaseNewer("v1.2.8", "1.2.7"))
        assertFalse(UpdateVersionCompare.isRemoteReleaseNewer("v1.2.7", "1.2.7"))
        assertFalse(UpdateVersionCompare.isRemoteReleaseNewer("v1.2.6", "1.2.7"))
    }

    @Test
    fun `isRemoteReleaseNewer ignores pre-release remote against stable local`() {
        assertFalse(UpdateVersionCompare.isRemoteReleaseNewer("v1.2.8-rc1", "1.2.7"))
    }

    @Test
    fun `pickApkAsset prefers canonical over legacy`() {
        val assets = listOf(
            GitHubAsset(1, "app-release.apk", 10, "https://example.com/legacy"),
            GitHubAsset(2, "vaultio-release.apk", 20, "https://example.com/canonical")
        )
        val picked = UpdateAssetPicker.pickApkAsset(assets, UpdateChannel.RELEASE)
        assertNotNull(picked)
        assertEquals("vaultio-release.apk", picked!!.name)
    }

    @Test
    fun `pickApkAsset falls back to legacy`() {
        val assets = listOf(
            GitHubAsset(1, "app-nightly.apk", 10, "https://example.com/legacy")
        )
        val picked = UpdateAssetPicker.pickApkAsset(assets, UpdateChannel.NIGHTLY)
        assertNotNull(picked)
        assertEquals("app-nightly.apk", picked!!.name)
    }

    @Test
    fun `pickApkAsset rejects zero-size and ambiguous matches`() {
        assertNull(
            UpdateAssetPicker.pickApkAsset(
                listOf(GitHubAsset(1, "vaultio-release.apk", 0, "https://example.com/empty")),
                UpdateChannel.RELEASE
            )
        )
        assertNull(
            UpdateAssetPicker.pickApkAsset(
                listOf(
                    GitHubAsset(1, "vaultio-release.apk", 10, "https://example.com/a"),
                    GitHubAsset(2, "vaultio-release.apk", 11, "https://example.com/b")
                ),
                UpdateChannel.RELEASE
            )
        )
    }

    @Test
    fun `update channel from build type`() {
        assertEquals(UpdateChannel.RELEASE, UpdateChannel.fromBuildType("release"))
        assertEquals(UpdateChannel.NIGHTLY, UpdateChannel.fromBuildType("nightly"))
        assertEquals(UpdateChannel.NONE, UpdateChannel.fromBuildType("debug"))
    }
}
