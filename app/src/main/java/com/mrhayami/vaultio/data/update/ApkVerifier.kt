package com.mrhayami.vaultio.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

sealed class ApkVerificationResult {
    data object Ok : ApkVerificationResult()
    data class Failed(val reason: String) : ApkVerificationResult()
}

object ApkVerifier {

    fun verify(context: Context, apkFile: File): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return ApkVerificationResult.Failed("Downloaded APK is missing or empty")
        }

        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: return ApkVerificationResult.Failed("Could not read APK metadata")

        val expectedPackage = context.packageName
        val archivePackage = archiveInfo.packageName
        if (archivePackage != expectedPackage) {
            return ApkVerificationResult.Failed("Package mismatch ($archivePackage)")
        }

        val installedInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    expectedPackage,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(expectedPackage, PackageManager.GET_SIGNING_CERTIFICATES)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return ApkVerificationResult.Failed("Installed package not found")
        }

        val installedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            installedInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            installedInfo.versionCode.toLong()
        }
        val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }
        if (archiveVersionCode < installedVersionCode) {
            return ApkVerificationResult.Failed("APK version code is a downgrade")
        }

        val installedCerts = signingCertDigests(installedInfo)
        val archiveCerts = signingCertDigests(archiveInfo)
        if (installedCerts.isEmpty() || archiveCerts.isEmpty()) {
            return ApkVerificationResult.Failed("Missing signing certificates")
        }
        if (installedCerts.intersect(archiveCerts).isEmpty()) {
            return ApkVerificationResult.Failed("Signing certificate mismatch")
        }

        return ApkVerificationResult.Ok
    }

    private fun signingCertDigests(info: android.content.pm.PackageInfo): Set<String> {
        val digests = mutableSetOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            val signers = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            signers.forEach { digests += sha256(it.toByteArray()) }
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.forEach { digests += sha256(it.toByteArray()) }
        }
        return digests
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
