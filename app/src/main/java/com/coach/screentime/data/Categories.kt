package com.coach.screentime.data

import com.coach.screentime.data.db.entities.CategoryEntity

object Categories {
    const val SOCIAL = "social"
    const val VIDEO = "video"
    const val GAMES = "games"
    const val NEWS = "news"
    const val MESSAGING = "messaging"
    const val OTHER = "other"

    val seed = listOf(
        CategoryEntity(SOCIAL, "Social", dailyMinutesCap = 90),
        CategoryEntity(VIDEO, "Video", dailyMinutesCap = 60),
        CategoryEntity(GAMES, "Games", dailyMinutesCap = 60),
        CategoryEntity(NEWS, "News", dailyMinutesCap = 30),
        CategoryEntity(MESSAGING, "Messaging", dailyMinutesCap = null),
        CategoryEntity(OTHER, "Other", dailyMinutesCap = null),
    )

    private val socialPackages = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.x.android",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.pinterest",
        "com.linkedin.android",
        "com.threads.android",
        "com.bluesky.android",
        "com.mastodon.android",
    )

    private val videoPackages = setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "tv.twitch.android.app",
        "com.hulu.plus",
    )

    private val newsPackages = setOf(
        "com.google.android.apps.magazines",
        "flipboard.app",
        "com.nytimes.android",
        "com.reddit.news",
    )

    private val messagingPackages = setOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.discord",
        "com.slack",
        "com.google.android.apps.messaging",
        "com.facebook.orca",
        "com.signal.android",
        "org.thoughtcrime.securesms",
    )

    fun categoryFor(packageName: String): String = when {
        packageName in socialPackages -> SOCIAL
        packageName in videoPackages -> VIDEO
        packageName in newsPackages -> NEWS
        packageName in messagingPackages -> MESSAGING
        packageName.contains("game", ignoreCase = true) -> GAMES
        else -> OTHER
    }

    /** Heuristic: should we suggest this app as flag-worthy by default? */
    fun isCommonlyTimeWasting(packageName: String): Boolean =
        packageName in socialPackages || packageName in videoPackages || packageName in newsPackages
}
