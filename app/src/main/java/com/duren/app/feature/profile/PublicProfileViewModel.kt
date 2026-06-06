package com.duren.app.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import com.duren.app.feature.nav.PublicProfileRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Read-only view of another user's profile + their active embers. */
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    profileRepository: ProfileRepository,
    emberRepository: EmberRepository
) : ViewModel() {

    private val userId: String = savedStateHandle.toRoute<PublicProfileRoute>().userId

    val profile: StateFlow<Profile?> = profileRepository.observeProfile(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val embers: StateFlow<List<Ember>> = emberRepository.observeByAuthor(userId, 50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
