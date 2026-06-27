package com.blockschedule.update

/**
 * Where the app looks for updates. Each GitHub release publishes two assets:
 *  - version.json  (the manifest read here)
 *  - app-release.apk (the signed APK)
 *
 * The "releases/latest/download/<asset>" URL is a stable redirect GitHub maintains to the
 * newest published (non-prerelease) release, so the app never needs an API token.
 */
object UpdateConfig {
    const val REPO_OWNER = "thedickestrick"
    const val REPO_NAME = "block-schedule"

    const val VERSION_JSON_URL =
        "https://github.com/$REPO_OWNER/$REPO_NAME/releases/latest/download/version.json"

    const val RELEASES_PAGE =
        "https://github.com/$REPO_OWNER/$REPO_NAME/releases/latest"
}
