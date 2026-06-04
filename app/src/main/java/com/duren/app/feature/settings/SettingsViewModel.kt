package com.duren.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.auth.AuthRepository
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import com.duren.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    val profile: StateFlow<Profile?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else profileRepository.observeProfile(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setAccent(hex: String) = viewModelScope.launch {
        settingsRepository.updateAccentColor(hex)
    }

    fun setLightMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateLightMode(enabled)
    }

    fun setAvatarColor(hex: String) = viewModelScope.launch {
        settingsRepository.updateAvatarColor(hex)
    }

    fun setPrivacy(
        showLantern: Boolean,
        showMoodCanvas: Boolean,
        allowAnonBox: Boolean,
        showTestimonials: Boolean
    ) = viewModelScope.launch {
        settingsRepository.updatePrivacy(showLantern, showMoodCanvas, allowAnonBox, showTestimonials)
    }

    fun saveAccount(displayName: String, bio: String, pronouns: String) = viewModelScope.launch {
        settingsRepository.updateAccount(displayName, bio, pronouns)
    }

    fun signOut() = authRepository.signOut()
}
