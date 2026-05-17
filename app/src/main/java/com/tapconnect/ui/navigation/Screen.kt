package com.tapconnect.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Connections : Screen("connections")
    object Profile : Screen("profile")
    object Discovery : Screen("discovery")
    object EditProfile : Screen("edit_profile")
}

