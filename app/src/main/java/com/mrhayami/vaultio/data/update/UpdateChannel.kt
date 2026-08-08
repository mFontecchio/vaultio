package com.mrhayami.vaultio.data.update

enum class UpdateChannel {
    RELEASE,
    NIGHTLY,
    NONE;

    val canonicalAssetName: String?
        get() = when (this) {
            RELEASE -> "vaultio-release.apk"
            NIGHTLY -> "vaultio-nightly.apk"
            NONE -> null
        }

    val legacyAssetName: String?
        get() = when (this) {
            RELEASE -> "app-release.apk"
            NIGHTLY -> "app-nightly.apk"
            NONE -> null
        }

    companion object {
        fun fromBuildType(buildType: String): UpdateChannel = when (buildType) {
            "release" -> RELEASE
            "nightly" -> NIGHTLY
            else -> NONE
        }
    }
}
