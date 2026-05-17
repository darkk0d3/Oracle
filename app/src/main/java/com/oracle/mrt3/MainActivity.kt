package com.oracle.mrt3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oracle.mrt3.ui.navigation.Screen
import com.oracle.mrt3.ui.screens.AccountSettingsScreen
import com.oracle.mrt3.ui.screens.LoginScreen
import com.oracle.mrt3.ui.screens.MainScreen
import com.oracle.mrt3.ui.screens.SplashScreen
import com.oracle.mrt3.ui.theme.OracleTheme
import com.oracle.mrt3.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OracleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen {
                                val dest = if (currentUser != null) Screen.Home.route
                                           else Screen.Login.route
                                navController.navigate(dest) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }

                        composable(Screen.Login.route) {
                            LoginScreen(
                                authViewModel = authViewModel,
                                activity = this@MainActivity,
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Home.route) {
                            MainScreen(
                                authViewModel = authViewModel,
                                onSignOut = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigateToAccountSettings = {
                                    navController.navigate(Screen.AccountSettings.route)
                                }
                            )
                        }

                        composable(Screen.AccountSettings.route) {
                            AccountSettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
