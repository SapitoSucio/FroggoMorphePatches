package app.froggo.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

object Constants {
    val COMPATIBILITY_FACEBOOK_573 = Compatibility(
        name = "Facebook",
        packageName = "com.facebook.katana",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1877F2,
        targets = listOf(
            AppTarget(
                version = "573.0.0.37.74",
                versionCodes = mapOf(
                    SupportedAbi.ARM64_V8A to 473623755,
                ),
            ),
        ),
    )

    val COMPATIBILITY_FACEBOOK_573_EXPERIMENTAL = Compatibility(
        name = "Facebook",
        packageName = "com.facebook.katana",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1877F2,
        targets = listOf(
            AppTarget(
                version = "573.0.0.37.74",
                versionCodes = mapOf(
                    SupportedAbi.ARM64_V8A to 473623755,
                ),
                isExperimental = true,
            ),
        ),
    )
}
