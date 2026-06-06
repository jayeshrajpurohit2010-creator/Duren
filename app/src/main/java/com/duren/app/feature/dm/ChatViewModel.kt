package com.duren.app.feature.dm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duren.app.data.dm.DmRepository
import com.duren.app.data.dm.model.DmMessage
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import com.duren.app.feature.nav.ChatRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One live conversation — streams messages in realtime and sends 48h-expiring DMs. */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dmRepository: DmRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    private val otherUserId: String = savedStateHandle.toRoute<ChatRoute>().otherUserId
    private val chatId: String? = dmRepository.currentUserId?.let { dmRepository.chatIdFor(it, otherUserId) }

    val currentUserId: String? get() = dmRepository.currentUserId

    val otherProfile: StateFlow<Profile?> = profileRepository.observeProfile(otherUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val messages: StateFlow<List<DmMessage>> =
        (chatId?.let { dmRepository.observeMessages(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True once the other person has opened our latest message (double-tick). */
    val seenByOther: StateFlow<Boolean> = dmRepository.observeSeenByOther(otherUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun send(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch { dmRepository.sendMessage(otherUserId, body) }
    }

    /** Mark the thread seen (clears my unread + lets the other side see their ticks fill). */
    fun markSeen() = viewModelScope.launch { chatId?.let { dmRepository.markRead(it) } }
}
