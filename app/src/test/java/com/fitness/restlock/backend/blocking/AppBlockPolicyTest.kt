package com.fitness.restlock.backend.blocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBlockPolicyTest {
    private val policy = StaticAppBlockPolicy(
        exemptPackages = setOf(
            "com.fitness.restlock",
            "com.android.settings",
            "com.launcher",
        ) + KnownExemptPackages.coreSystemPackages +
            KnownExemptPackages.commonLauncherPackages + KnownExemptPackages.commonHomeSurfacePackages,
    )

    @Test
    fun shouldBlockNonExemptPackageOnlyWhenLockActive() {
        val result = policy.shouldBlock(
            foregroundPackage = "com.social.app",
            allowedPackages = emptySet(),
            lockActive = true,
        )

        assertTrue(result)
    }

    @Test
    fun doesNotBlockWhenLockInactive() {
        val result = policy.shouldBlock(
            foregroundPackage = "com.social.app",
            allowedPackages = emptySet(),
            lockActive = false,
        )

        assertFalse(result)
    }

    @Test
    fun doesNotBlockAllowedPackage() {
        val result = policy.shouldBlock(
            foregroundPackage = "com.video.app",
            allowedPackages = setOf("com.video.app"),
            lockActive = true,
        )

        assertFalse(result)
    }

    @Test
    fun doesNotBlockExemptPackage() {
        val result = policy.shouldBlock(
            foregroundPackage = "com.android.settings",
            allowedPackages = emptySet(),
            lockActive = true,
        )

        assertFalse(result)
    }

    @Test
    fun commonSamsungLauncherIsKnownExemptPackage() {
        assertTrue("com.sec.android.app.launcher" in KnownExemptPackages.commonLauncherPackages)
    }

    @Test
    fun googleHomeSearchSurfaceIsKnownExemptPackage() {
        assertTrue("com.google.android.googlequicksearchbox" in KnownExemptPackages.commonHomeSurfacePackages)
    }

    @Test
    fun sanitizeAllowedPackagesDropsBlankAndExemptValues() {
        val result = policy.sanitizeAllowedPackages(
            setOf(" ", "com.social.app", "com.launcher", "com.video.app"),
        )

        assertEquals(setOf("com.social.app", "com.video.app"), result)
    }

    @Test
    fun missingOrBlankPackagesNeverBlock() {
        listOf(null, "", "  ").forEach {
            assertFalse(policy.shouldBlock(it, emptySet(), true))
        }
    }

    @Test
    fun strictModePreservesHostAndEssentialSurfaces() {
        val packages = setOf(
            "com.fitness.restlock", "com.launcher", "android", "com.android.settings",
            "com.android.systemui", "com.android.permissioncontroller",
            "com.google.android.permissioncontroller", "com.android.packageinstaller",
            "com.google.android.packageinstaller", "com.android.phone", "com.android.server.telecom",
            "com.android.dialer", "com.google.android.dialer", "com.android.emergency",
            "com.samsung.android.dialer", "com.samsung.android.incallui",
            "com.android.incallui", "com.sec.android.app.launcher",
            "com.google.android.apps.nexuslauncher", "com.google.android.googlequicksearchbox",
            "com.samsung.android.app.galaxyfinder",
        )
        packages.forEach {
            assertFalse("Essential package must remain usable: $it", policy.shouldBlock(it, emptySet(), true))
        }
        assertTrue(policy.shouldBlock("com.social.app", emptySet(), true))
    }

    @Test
    fun allowlistDoesNotPermitUnselectedApps() {
        assertTrue(policy.shouldBlock("com.social.app", setOf("com.video.app"), true))
    }
}
