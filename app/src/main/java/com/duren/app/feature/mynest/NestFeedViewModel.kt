package com.duren.app.feature.mynest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.nest.NestRepository
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import com.duren.app.data.smoke.SmokeSignalRepository
import com.duren.app.data.smoke.model.SmokeOutcome
import com.duren.app.data.smoke.model.SmokeSignal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The Nest tab, repaired to match the spec: your people up top + a feed of their
 * embers below. Pure friends-feed — Lanterns moved to their own screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NestFeedViewModel @Inject constructor(
    private val nestRepository: NestRepository,
    private val emberRepository: EmberRepository,
    private val profileRepository: ProfileRepository,
    private val smokeSignalRepository: SmokeSignalRepository
) : ViewModel() {

    val currentUserId: String? get() = emberRepository.currentUserId

    /** One shared member-id stream so we open a single Firestore listener, not two. */
    private val memberIds: StateFlow<List<String>> = nestRepository.observeMemberIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The current user's Nest members (avatar row). */
    val members: StateFlow<List<Profile>> = memberIds
        .map { ids -> ids.mapNotNull { profileRepository.getProfile(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** How many pending requests are waiting (for the "Requests" chip). */
    val requestCount: StateFlow<Int> = nestRepository.observeIncomingRequests()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Embers authored by Nest members (and you), newest first, with my echo state hydrated. */
    val feed: StateFlow<List<Ember>> = memberIds
        .flatMapLatest { ids ->
            // Include your own embers alongside your Nest's. Without this the Nest is
            // asymmetric — your people see what you post, but you don't — which read as
            // a bug. Self is always within the 30-author whereIn cap (it's listed first).
            val authors = (listOfNotNull(currentUserId) + ids).distinct()
            emberRepository.observeFromAuthors(authors, FEED_LIMIT)
        }
        .map { embers ->
            coroutineScope {
                embers.map { ember ->
                    async { ember.copy(echoedByMe = emberRepository.hasEchoed(ember.id)) }
                }.awaitAll()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun echo(emberId: String) = viewModelScope.launch { emberRepository.toggleEcho(emberId) }
    fun coldMark(emberId: String, reason: String) =
        viewModelScope.launch { emberRepository.coldMark(emberId, reason) }
    fun deleteEmber(emberId: String) = viewModelScope.launch { emberRepository.deleteEmber(emberId) }

    /** Smoke Signals sent up for me — they ride at the top of the Nest feed (F30). */
    val smokeSignals: StateFlow<List<SmokeSignal>> = smokeSignalRepository.observeIncoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // One-shot toast after a send attempt; the screen shows it then clears it.
    private val _smokeMessage = MutableStateFlow<String?>(null)
    val smokeMessage: StateFlow<String?> = _smokeMessage.asStateFlow()

    /** Send one signal up to the whole Nest — rationed to one a week (F30). */
    fun sendSmokeSignal(text: String) = viewModelScope.launch {
        _smokeMessage.value = when (smokeSignalRepository.send(text)) {
            SmokeOutcome.Sent -> "Your smoke is rising 💨"
            SmokeOutcome.OncePerWeek -> "One signal a week — yours is still in the sky."
            SmokeOutcome.EmptyNest -> "Your Nest is empty. There's no one to signal."
            SmokeOutcome.Failed -> "The smoke wouldn't rise. Try again."
        }
    }

    fun clearSmokeMessage() { _smokeMessage.value = null }

    private companion object {
        const val FEED_LIMIT = 60L
    }
}
