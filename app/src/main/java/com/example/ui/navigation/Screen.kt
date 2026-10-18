package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Browser : Screen("browser")
    object Tabs : Screen("tabs")
    object Bookmarks : Screen("bookmarks")
    object History : Screen("history")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
    object SpeedTest : Screen("speed_test")
}
