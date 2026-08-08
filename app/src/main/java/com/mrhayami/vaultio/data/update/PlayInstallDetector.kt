package com.mrhayami.vaultio.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object PlayInstallDetector {
    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    fun isPlayInstall(context: Context): Boolean {
        val installer = installerPackageName(context) ?: return false
        return installer == PLAY_STORE_PACKAGE
    }

    fun installerPackageName(context: Context): String? {
        val pm = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(context.packageName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
