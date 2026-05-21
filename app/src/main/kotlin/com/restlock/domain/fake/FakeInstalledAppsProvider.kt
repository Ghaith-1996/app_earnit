package com.restlock.domain.fake

import com.restlock.domain.InstalledApp
import com.restlock.domain.InstalledAppsProvider
import kotlinx.coroutines.delay

/**
 * A small list of realistic-looking apps so the picker has content during
 * development. Replaced by a PackageManager-backed implementation in the real
 * backend (see InstalledAppsProvider docs).
 */
class FakeInstalledAppsProvider : InstalledAppsProvider {
    override suspend fun listLaunchableApps(): List<InstalledApp> {
        delay(120) // mimic real I/O so we exercise the loading state
        return listOf(
            "Instagram" to "com.instagram.android",
            "TikTok" to "com.zhiliaoapp.musically",
            "YouTube" to "com.google.android.youtube",
            "Reddit" to "com.reddit.frontpage",
            "X" to "com.twitter.android",
            "Facebook" to "com.facebook.katana",
            "Snapchat" to "com.snapchat.android",
            "Discord" to "com.discord",
            "Chrome" to "com.android.chrome",
            "Netflix" to "com.netflix.mediaclient",
            "Spotify" to "com.spotify.music",
            "WhatsApp" to "com.whatsapp",
            "Telegram" to "org.telegram.messenger",
            "Slack" to "com.Slack",
            "Gmail" to "com.google.android.gm",
        ).map { (label, pkg) -> InstalledApp(packageName = pkg, label = label) }
    }
}
