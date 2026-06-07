package com.duren.app.feature.mynest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.nest.NestRepository
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.profile.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val profileRepository: ProfileRepository
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

    /** Embers authored by Nest members, newest first, with my echo state hydrated. */
    val feed: StateFlow<List<Ember>> = memberIds
        .flatMapLatest { ids -> emberRepository.observeFromAuthors(ids, FEED_LIMIT) }
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

    private companion object {
        const val FEED_LIMIT = 60L
    }
}
