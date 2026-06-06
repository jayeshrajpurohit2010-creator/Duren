package com.duren.app.feature.mynest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.nest.NestRepository
import com.duren.app.data.nest.model.NestRequest
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The current user's Nest: incoming requests to resolve + current members. */
@HiltViewModel
class MyNestViewModel @Inject constructor(
    private val nestRepository: NestRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val incoming: StateFlow<List<NestRequest>> = nestRepository.observeIncomingRequests()
        .map { requests ->
            requests.map { it.copy(fromProfile = profileRepository.getProfile(it.fromUserId)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val members: StateFlow<List<Profile>> = nestRepository.observeMemberIds()
        .map { ids -> ids.mapNotNull { profileRepository.getProfile(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun accept(fromUserId: String) = viewModelScope.launch { nestRepository.acceptRequest(fromUserId) }
    fun decline(fromUserId: String) = viewModelScope.launch { nestRepository.declineRequest(fromUserId) }
    fun remove(userId: String) = viewModelScope.launch { nestRepository.removeMember(userId) }
}
