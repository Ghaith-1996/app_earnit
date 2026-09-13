package com.fitness.restlock.backend.blocking

internal object KnownExemptPackages {
    val coreSystemPackages = setOf(
        "android",
        "com.android.settings",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.google.android.apps.safetycenter",
        // Fallbacks for in-call/emergency surfaces outside the selected default dialer.
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.emergency",
        "com.android.incallui",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
    )

    val commonLauncherPackages = setOf(
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.apps.pixel.launcher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher",
        "com.oneplus.launcher",
        "com.microsoft.launcher",
    )

    val commonHomeSurfacePackages = setOf(
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.searchlite",
        "com.samsung.android.app.galaxyfinder",
    )
}
