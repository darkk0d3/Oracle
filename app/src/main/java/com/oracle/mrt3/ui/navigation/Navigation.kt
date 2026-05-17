package com.oracle.mrt3.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector

// Requires: androidx.compose.material:material-icons-extended
sealed class Screen(val route: String) {
    object Splash          : Screen("splash")
    object Login           : Screen("login")
    object Home            : Screen("home")
    object Fare            : Screen("fare")
    object Map             : Screen("map")
    object Emergency       : Screen("emergency")
    object Profile         : Screen("profile")
    object AccountSettings : Screen("account_settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "Home",      Icons.Filled.Home,    Icons.Outlined.Home),
    BottomNavItem(Screen.Fare,      "Fare",      Icons.Filled.Receipt, Icons.Outlined.Receipt),
    BottomNavItem(Screen.Map,       "Map",       Icons.Filled.Map,     Icons.Outlined.Map),
    BottomNavItem(Screen.Emergency, "Emergency", Icons.Filled.Warning, Icons.Outlined.Warning),
    BottomNavItem(Screen.Profile,   "Profile",   Icons.Filled.Person,  Icons.Outlined.Person)
)
