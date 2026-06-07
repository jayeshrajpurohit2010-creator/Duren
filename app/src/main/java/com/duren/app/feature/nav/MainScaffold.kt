package com.duren.app.feature.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duren.app.ui.components.DurenIcon
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.duren.app.feature.compose.ComposeScreen
import com.duren.app.feature.dm.ChatListScreen
import com.duren.app.feature.dm.ChatScreen
import com.duren.app.feature.feed.FeedScreen
import com.duren.app.feature.mynest.MyNestScreen
import com.duren.app.feature.mynest.NestFeedScreen
import com.duren.app.feature.nest.NestScreen
import com.duren.app.feature.profile.ProfileScreen
import com.duren.app.feature.profile.PublicProfileScreen
import com.duren.app.feature.search.SearchScreen
import com.duren.app.feature.settings.SettingsScreen
import com.duren.app.feature.signal.SignalScreen
import com.duren.app.feature.tribes.CreateTribeScreen
import com.duren.app.feature.tribes.TribeDetailScreen
import com.duren.app.feature.tribes.TribesScreen

private data class TabSpec<T : Any>(
    val route: T,
    val label: String,
    val icon: DurenIcon
)

@Composable
fun MainScaffold(onSignedOut: () -> Unit) {
    val tabsNav = rememberNavController()

    val tabs = listOf(
        // Duren's own hand-drawn glyphs (design export `Icon.*`), not Material.
        TabSpec(StateTab, "Clearing", DurenIcon.Ember),
        TabSpec(TribesTab, "Tribes", DurenIcon.Tribe),
        TabSpec(ComposeTab, "Compose", DurenIcon.Plus),
        TabSpec(NestTab, "Nest", DurenIcon.Nest),
        TabSpec(PresenceTab, "Presence", DurenIcon.Presence)
    )

    val current by tabsNav.currentBackStackEntryAsState()
    val destination = current?.destination
    // Full-screen routes pushed over the tabs hide the bottom bar.
    val onFullScreen = destination?.hierarchy?.any {
        it.hasRoute(SettingsRoute::class) ||
            it.hasRoute(SearchRoute::class) ||
            it.hasRoute(PublicProfileRoute::class) ||
            it.hasRoute(NestRoute::class) ||
            it.hasRoute(SignalRoute::class) ||
            it.hasRoute(ChatListRoute::class) ||
            it.hasRoute(ChatRoute::class) ||
            it.hasRoute(LanternsRoute::class) ||
            it.hasRoute(CreateTribeRoute::class) ||
            it.hasRoute(TribeDetailRoute::class)
    } == true
    val showBottomBar = !onFullScreen

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
                            icon = { DurenIcon(tab.icon, size = 24.dp) },
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
            composable<StateTab> {
                FeedScreen(
                    onOpenSearch = { tabsNav.navigate(SearchRoute) },
                    onOpenSignal = { tabsNav.navigate(SignalRoute) },
                    onOpenMessages = { tabsNav.navigate(ChatListRoute) }
                )
            }
            composable<TribesTab> {
                TribesScreen(
                    onCreateTribe = { tabsNav.navigate(CreateTribeRoute) },
                    onOpenTribe = { tribeId -> tabsNav.navigate(TribeDetailRoute(tribeId)) }
                )
            }
            composable<ComposeTab> {
                ComposeScreen(
                    onPosted = {
                        tabsNav.navigate(StateTab) {
                            popUpTo(StateTab) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable<NestTab> {
                NestFeedScreen(
                    onOpenProfile = { userId -> tabsNav.navigate(PublicProfileRoute(userId)) },
                    onOpenChat = { userId -> tabsNav.navigate(ChatRoute(userId)) },
                    onOpenMessages = { tabsNav.navigate(ChatListRoute) },
                    onOpenRequests = { tabsNav.navigate(NestRoute) },
                    onOpenLanterns = { tabsNav.navigate(LanternsRoute) },
                    onOpenSearch = { tabsNav.navigate(SearchRoute) }
                )
            }
            composable<PresenceTab> {
                ProfileScreen(
                    onSignedOut = onSignedOut,
                    onOpenSettings = { tabsNav.navigate(SettingsRoute) },
                    onOpenNest = { tabsNav.navigate(NestRoute) }
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { tabsNav.popBackStack() },
                    onSignedOut = onSignedOut
                )
            }
            composable<SearchRoute> {
                SearchScreen(
                    onBack = { tabsNav.popBackStack() },
                    onOpenProfile = { userId -> tabsNav.navigate(PublicProfileRoute(userId)) }
                )
            }
            composable<PublicProfileRoute> {
                PublicProfileScreen(
                    onBack = { tabsNav.popBackStack() },
                    onOpenChat = { userId -> tabsNav.navigate(ChatRoute(userId)) }
                )
            }
            composable<NestRoute> {
                MyNestScreen(
                    onBack = { tabsNav.popBackStack() },
                    onOpenProfile = { userId -> tabsNav.navigate(PublicProfileRoute(userId)) },
                    onOpenChat = { userId -> tabsNav.navigate(ChatRoute(userId)) }
                )
            }
            composable<SignalRoute> {
                SignalScreen(
                    onBack = { tabsNav.popBackStack() },
                    onOpenProfile = { userId -> tabsNav.navigate(PublicProfileRoute(userId)) },
                    onOpenChat = { userId -> tabsNav.navigate(ChatRoute(userId)) }
                )
            }
            composable<ChatListRoute> {
                ChatListScreen(
                    onBack = { tabsNav.popBackStack() },
                    onOpenChat = { userId -> tabsNav.navigate(ChatRoute(userId)) }
                )
            }
            composable<ChatRoute> {
                ChatScreen(onBack = { tabsNav.popBackStack() })
            }
            composable<LanternsRoute> {
                NestScreen(onBack = { tabsNav.popBackStack() })
            }
            composable<CreateTribeRoute> {
                CreateTribeScreen(
                    onBack = { tabsNav.popBackStack() },
                    onCreated = { tabsNav.popBackStack() }
                )
            }
            composable<TribeDetailRoute> {
                TribeDetailScreen(
                    onBack = { tabsNav.popBackStack() }
                )
            }
        }
    }
}
