package com.duren.app.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.nest.NestRepository
import com.duren.app.data.nest.model.NestRelation
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import com.duren.app.feature.nav.PublicProfileRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read-only view of another user's profile + their active embers, with the Nest button. */
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    profileRepository: ProfileRepository,
    emberRepository: EmberRepository,
    private val nestRepository: NestRepository
) : ViewModel() {

    val userId: String = savedStateHandle.toRoute<PublicProfileRoute>().userId

    val profile: StateFlow<Profile?> = profileRepository.observeProfile(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val embers: StateFlow<List<Ember>> = emberRepository.observeByAuthor(userId, 50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val relation: StateFlow<NestRelation> = nestRepository.observeRelation(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NestRelation.None)

    fun addToNest() = viewModelScope.launch { nestRepository.sendRequest(userId) }
    fun cancelRequest() = viewModelScope.launch { nestRepository.cancelRequest(userId) }
    fun acceptRequest() = viewModelScope.launch { nestRepository.acceptRequest(userId) }
    fun declineRequest() = viewModelScope.launch { nestRepository.declineRequest(userId) }
}
