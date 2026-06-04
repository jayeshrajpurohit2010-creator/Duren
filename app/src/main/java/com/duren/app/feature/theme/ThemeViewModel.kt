package com.duren.app.feature.theme

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.auth.AuthRepository
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.ui.theme.DurenAccent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Active theme derived from the signed-in user's settings. */
data class ThemeState(
    val darkTheme: Boolean = true,
    val accent: Color = DurenAccent.default.color
)

/**
 * Surfaces the current user's theme preferences (dark/light + accent) to the app
 * root. Before sign-in (or before the profile loads) it emits the defaults:
 * dark mode, teal accent.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThemeViewModel @Inject constructor(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    val themeState: StateFlow<ThemeState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(ThemeState())
            } else {
                profileRepository.observeProfile(user.uid).map { profile ->
                    if (profile == null) {
                        ThemeState()
                    } else {
                        ThemeState(
                            darkTheme = !profile.lightModeEnabled,
                            accent = DurenAccent.colorForHex(profile.accentColor)
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeState())
}
