package com.duren.app.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.auth.AuthRepository
import com.duren.app.data.media.MediaUploadRepository
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import com.duren.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val mediaUploadRepository: MediaUploadRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    val profile: StateFlow<Profile?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else profileRepository.observeProfile(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True while a picked avatar photo is being compressed + saved. */
    private val _avatarUploading = MutableStateFlow(false)
    val avatarUploading: StateFlow<Boolean> = _avatarUploading.asStateFlow()

    /** Compress the picked photo to a small data URI and save it as the avatar. */
    fun setAvatarPhoto(uri: Uri) = viewModelScope.launch {
        _avatarUploading.value = true
        mediaUploadRepository.uploadAvatar(uri)
            .onSuccess { dataUri -> settingsRepository.updateAvatarUrl(dataUri) }
        _avatarUploading.value = false
    }

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

    /** True once a password-reset email has been dispatched, so the UI can confirm it. */
    private val _passwordResetSent = MutableStateFlow(false)
    val passwordResetSent: StateFlow<Boolean> = _passwordResetSent.asStateFlow()

    /** Email a password-reset link to the signed-in user's address. */
    fun sendPasswordReset(email: String) = viewModelScope.launch {
        authRepository.sendPasswordReset(email)
        _passwordResetSent.value = true
    }

    fun acknowledgePasswordReset() {
        _passwordResetSent.value = false
    }

    /** True while the account deletion is in flight (re-auth + Firestore + auth delete). */
    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    /** Set once deletion completes so the UI can route back to the auth screen. */
    private val _accountDeleted = MutableStateFlow(false)
    val accountDeleted: StateFlow<Boolean> = _accountDeleted.asStateFlow()

    /** Non-null when the last delete attempt failed (e.g. wrong password). */
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    fun deleteAccount(password: String) = viewModelScope.launch {
        _deleting.value = true
        _deleteError.value = null
        authRepository.deleteAccount(password)
            .onSuccess { _accountDeleted.value = true }
            .onFailure {
                _deleteError.value =
                    "Couldn't delete your account. Check your password and try again."
            }
        _deleting.value = false
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun signOut() = authRepository.signOut()
}
