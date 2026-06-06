package com.duren.app.feature.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duren.app.feature.auth.AuthScreen
import com.duren.app.feature.landing.LandingScreen

@Composable
fun DurenNavHost(
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isAuthed by sessionViewModel.isAuthenticated.collectAsStateWithLifecycle()

    // Whenever auth state flips, snap to the matching graph.
    LaunchedEffect(isAuthed) {
        val target = if (isAuthed) MainGraph else AuthGraph
        navController.navigate(target) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isAuthed) MainGraph else AuthGraph
    ) {
        composable<AuthGraph> {
            // Signed-out visitors land on the brand screen first; "Get started"
            // reveals the auth form. Survives config changes via rememberSaveable.
            var showAuth by rememberSaveable { mutableStateOf(false) }
            if (showAuth) {
                AuthScreen(onAuthenticated = { /* SessionViewModel re-emits, LaunchedEffect navigates */ })
            } else {
                LandingScreen(onGetStarted = { showAuth = true })
            }
        }
        composable<MainGraph> {
            MainScaffold(
                onSignedOut = { /* SessionViewModel re-emits, LaunchedEffect navigates */ }
            )
        }
    }
}
