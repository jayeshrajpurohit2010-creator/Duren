package com.duren.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.auth.AuthRepository
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    emberRepository: EmberRepository
) : ViewModel() {

    val profile: StateFlow<Profile?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else profileRepository.observeProfile(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myEmbers: StateFlow<List<Ember>> = emberRepository.observeMine(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun signOut() = authRepository.signOut()
}
