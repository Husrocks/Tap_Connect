package com.tapconnect.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tapconnect.ui.screens.*
import com.tapconnect.ui.theme.CardBg

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val profileViewModel: ProfileViewModel = viewModel()
    
    // Persist login state: check if we have a valid token saved
    val hasToken = remember { com.tapconnect.data.local.TokenManager.getToken() != null }
    val startDestination = if (hasToken) Screen.Home.route else "login"
    val showBottomBar = currentDestination?.route != "login" && currentDestination?.route != "register"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = CardBg) {
                    // Home Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    // Discovery Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Radar, contentDescription = null) },
                        label = { Text("Radar") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Discovery.route } == true,
                        onClick = {
                            navController.navigate(Screen.Discovery.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    // Connections Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        label = { Text("History") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Connections.route } == true,
                        onClick = {
                            navController.navigate(Screen.Connections.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    // Profile Tab
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        label = { Text("Profile") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Profile.route } == true,
                        onClick = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegisterClick = {
                        navController.navigate("register")
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Discovery.route) {
                DiscoveryScreen()
            }
            composable(Screen.Connections.route) {
                ConnectionsScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onEditClick = { navController.navigate(Screen.EditProfile.route) },
                    onLogout = {
                        com.tapconnect.data.remote.WebSocketManager.getInstance().disconnect()
                        com.tapconnect.data.local.TokenManager.clearToken()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}




