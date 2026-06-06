package com.duren.app.feature.nav

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes. Each destination is a @Serializable
 * object/data class; navController.navigate(StateTab) is the call form.
 */
@Serializable
object AuthGraph

@Serializable
object MainGraph

@Serializable
object AuthRoute

@Serializable
object StateTab

@Serializable
object TribesTab

@Serializable
object ComposeTab

@Serializable
object NestTab

@Serializable
object PresenceTab

@Serializable
object SettingsRoute

@Serializable
object SearchRoute

@Serializable
object CreateTribeRoute

@Serializable
data class TribeDetailRoute(val tribeId: String)
