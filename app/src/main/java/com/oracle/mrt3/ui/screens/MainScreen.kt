package com.oracle.mrt3.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oracle.mrt3.ui.navigation.BottomNavBar
import com.oracle.mrt3.ui.navigation.Screen
import com.oracle.mrt3.ui.theme.PrimaryGreen
import com.oracle.mrt3.viewmodel.AuthViewModel
import com.oracle.mrt3.viewmodel.LiveTrainViewModel
import com.oracle.mrt3.viewmodel.TripViewModel

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit,
    onNavigateToAccountSettings: () -> Unit
) {
    val navController = rememberNavController()
    val currentUser   by authViewModel.currentUser.collectAsStateWithLifecycle()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

    val liveTrainViewModel: LiveTrainViewModel = hiltViewModel()
    val tripViewModel:      TripViewModel      = hiltViewModel()

    Scaffold(
        bottomBar = {
            BottomNavBar(
                navController = navController,
                currentRoute  = currentRoute
            )
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToFare      = { navController.navigate(Screen.Fare.route) },
                    onNavigateToMap       = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToEmergency = { navController.navigate(Screen.Emergency.route) }
                )
            }

            composable(Screen.Fare.route) {
                FareScreen(
                    viewModel       = tripViewModel,
                    onNavigateToMap = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Map.route) {
                MapScreen(
                    viewModel          = tripViewModel,
                    liveTrainViewModel = liveTrainViewModel,
                    onExitMap = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Emergency.route) {
                EmergencyScreen(isSignedIn = currentUser != null)
            }

            composable(Screen.Profile.route) {
                if (currentUser != null) {
                    ProfileScreen(
                        authViewModel               = authViewModel,
                        onSignOut                   = onSignOut,
                        onNavigateToAccountSettings = onNavigateToAccountSettings,
                        liveTrainViewModel          = liveTrainViewModel
                    )
                } else {
                    SignInPromptScreen(
                        onSignIn = { navController.navigate(Screen.Login.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun SignInPromptScreen(onSignIn: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sign in to view your profile", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSignIn,
                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Sign In")
            }
        }
    }
}
