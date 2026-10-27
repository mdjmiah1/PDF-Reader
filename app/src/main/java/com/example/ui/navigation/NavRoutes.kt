package com.example.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Files : Screen("files")
    object Bookmarks : Screen("bookmarks")
    object Settings : Screen("settings")
    object Reader : Screen("reader/{encodedUri}") {
        fun createRoute(uri: String): String = "reader/${Uri.encode(uri)}"
    }
}
