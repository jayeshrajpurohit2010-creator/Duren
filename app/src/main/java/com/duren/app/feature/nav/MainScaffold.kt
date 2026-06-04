package com.duren.app.feature.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.duren.app.feature.profile.ProfileScreen
import com.duren.app.feature.settings.SettingsScreen
import com.duren.app.feature.tabs.ComposeTabScreen
import com.duren.app.feature.tabs.NestTabScreen
import com.duren.app.feature.tabs.StateTabScreen
import com.duren.app.feature.tabs.TribesTabScreen

private data class TabSpec<T : Any>(
    val route: T,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScaffold(onSignedOut: () -> Unit) {
    val tabsNav = rememberNavController()

    val tabs = listOf(
        // Placeholder icons from icons-core; Phase 1 replaces these with custom
        // Duren icons (ic_campfire, ic_tribe, ic_compose, ic_nest, ic_presence).
        TabSpec(StateTab, "State", Icons.Outlined.Home),
        TabSpec(TribesTab, "Tribes", Icons.Outlined.Star),
        TabSpec(ComposeTab, "Compose", Icons.Outlined.Add),
        TabSpec(NestTab, "Nest", Icons.Outlined.Favorite),
        TabSpec(PresenceTab, "Presence", Icons.Outlined.Person)
    )

    val current by tabsNav.currentBackStackEntryAsState()
    val destination = current?.destination
    // Settings is a full-screen route pushed over the tabs — hide the bottom bar there.
    val showBottomBar = destination?.hierarchy?.any { it.hasRoute(SettingsRoute::class) } != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                tabsNav.navigate(tab.route) {
                                    popUpTo(StateTab) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = tabsNav,
            startDestination = StateTab,
            modifier = Modifier.padding(padding)
        ) {
            composable<StateTab> { StateTabScreen() }
            composable<TribesTab> { TribesTabScreen() }
            composable<ComposeTab> { ComposeTabScreen() }
            composable<NestTab> { NestTabScreen() }
            composable<PresenceTab> {
                ProfileScreen(
                    onSignedOut = onSignedOut,
                    onOpenSettings = { tabsNav.navigate(SettingsRoute) }
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { tabsNav.popBackStack() },
                    onSignedOut = onSignedOut
                )
            }
        }
    }
}
