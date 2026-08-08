package com.mrhayami.vaultio.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {
    const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    const val UPDATES_DIR = "updates"
    const val APK_FILE_NAME = "vaultio-update.apk"

    fun updatesDir(context: Context): File = File(context.filesDir, UPDATES_DIR)

    fun updateApkFile(context: Context): File = File(updatesDir(context), APK_FILE_NAME)

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun unknownSourcesSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val authority = context.packageName + FILE_PROVIDER_SUFFIX
        val uri = FileProvider.getUriForFile(context, authority, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
