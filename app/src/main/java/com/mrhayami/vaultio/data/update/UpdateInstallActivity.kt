package com.mrhayami.vaultio.data.update

import android.app.Activity
import android.os.Bundle
import android.util.Log

/**
 * Explicit trampoline for update-install notifications.
 *
 * Notifications must use an explicit PendingIntent (own component) so the
 * ACTION_VIEW package-installer Intent is never exposed as an implicit PendingIntent.
 */
class UpdateInstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val apkFile = ApkInstaller.updateApkFile(this)
            if (!apkFile.exists()) {
                Log.w(TAG, "No pending APK to install")
                return
            }
            if (ApkVerifier.verify(this, apkFile) !is ApkVerificationResult.Ok) {
                Log.w(TAG, "Pending APK failed verification; deleting")
                apkFile.delete()
                return
            }
            startActivity(ApkInstaller.createInstallIntent(this, apkFile))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
        } finally {
            finish()
        }
    }

    companion object {
        private const val TAG = "UpdateInstallActivity"
    }
}
