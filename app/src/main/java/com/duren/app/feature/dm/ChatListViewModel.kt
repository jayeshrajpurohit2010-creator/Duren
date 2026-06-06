package com.duren.app.feature.dm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.dm.DmRepository
import com.duren.app.data.dm.model.ChatSummary
import com.duren.app.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The list of the current user's conversations, newest first, profiles hydrated. */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val dmRepository: DmRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val chats: StateFlow<List<ChatSummary>> = dmRepository.observeChats()
        .map { list ->
            list.map { it.copy(otherProfile = profileRepository.getProfile(it.otherUserId)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
