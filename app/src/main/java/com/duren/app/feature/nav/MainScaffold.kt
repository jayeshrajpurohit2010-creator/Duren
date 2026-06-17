package com.duren.app.feature.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import com.duren.app.ui.theme.LocalDurenColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.duren.app.ui.animation.DurenSprings
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

    // Dove Mode (F32): a private campfire — the chrome dissolves so only embers in the
    // dark remain, with one quiet dove to come back. Held here so the bottom bar can go.
    var doveMode by rememberSaveable { mutableStateOf(false) }

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
    // Dove Mode dissolves the bottom bar too — the whole point is no chrome.
    val showBottomBar = !onFullScreen && !doveMode

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Box {
                NavigationBar(
                    // Translucent dark, not solid black — the bar sits quietly over the
                    // darkness instead of fencing it off with a hard Material edge.
                    containerColor = LocalDurenColors.current.Glass,
                    tonalElevation = 0.dp
                ) {
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
                            icon = {
                                // The active tab's glyph springs up a touch — a small,
                                // physical "you are here" instead of a Material pill.
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selected) 1.2f else 1f,
                                    animationSpec = DurenSprings.Medium,
                                    label = "navIconScale"
                                )
                                DurenIcon(
                                    tab.icon,
                                    size = 24.dp,
                                    modifier = Modifier.scale(iconScale)
                                )
                            },
                            label = { Text(tab.label) },
                            // Teal where you are, near-invisible where you're not — and
                            // no Material "pill" highlight behind the active icon.
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalDurenColors.current.AccentTeal,
                                selectedTextColor = LocalDurenColors.current.AccentTeal,
                                unselectedIconColor = LocalDurenColors.current.TextDisabled,
                                unselectedTextColor = LocalDurenColors.current.TextDisabled,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
                    // Glass rim: a soft swept top edge so the frosted bar lifts off
                    // the content instead of dissolving into it (key in light mode).
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        LocalDurenColors.current.GlassBorder,
                                        Color.Transparent,
                                    )
                                )
                            )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = tabsNav,
            startDestination = StateTab,
            modifier = Modifier.padding(padding),
            // Every destination breathes in: a soft fade with a hair of upward drift, so
            // moving between tabs and into pushed screens feels lit, not snapped.
            enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 18 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 18 } }
        ) {
            composable<StateTab> {
                FeedScreen(
                    onOpenSearch = { tabsNav.navigate(SearchRoute) },
                    onOpenSignal = { tabsNav.navigate(SignalRoute) },
                    onOpenMessages = { tabsNav.navigate(ChatListRoute) },
                    doveMode = doveMode,
                    onToggleDove = { doveMode = !doveMode }
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
