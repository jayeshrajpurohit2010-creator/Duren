package com.duren.app.feature.tribes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.tribe.TribeRepository
import com.duren.app.data.tribe.model.Bulletin
import com.duren.app.data.tribe.model.Tribe
import com.duren.app.feature.nav.TribeDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One tribe's detail: a header (name, member count, join/leave) plus that tribe's
 * ember feed. Echo/cold-mark behaviour mirrors [com.duren.app.feature.feed.FeedViewModel].
 */
data class TribeDetailUiState(
    val tribe: Tribe? = null,
    val embers: List<Ember> = emptyList(),
    val bulletins: List<Bulletin> = emptyList(),
    val presentCount: Int = 0,
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TribeDetailViewModel @Inject constructor(
    private val emberRepository: EmberRepository,
    private val tribeRepository: TribeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tribeId: String = savedStateHandle.toRoute<TribeDetailRoute>().tribeId

    private val limit = MutableStateFlow(20L)
    private val optimisticEchoToggles = MutableStateFlow<Set<String>>(emptySet())

    private val tribeFlow: Flow<Tribe?> = tribeRepository.observeTribe(tribeId)

    private val hydratedEmbers: Flow<List<Ember>> = limit
        .flatMapLatest { currentLimit ->
            flow {
                emberRepository.observeTribeEmbers(tribeId, currentLimit).collect { rawEmbers ->
                    val hydrated = coroutineScope {
                        rawEmbers.map { ember ->
                            async { ember.copy(echoedByMe = emberRepository.hasEchoed(ember.id)) }
                        }.map { it.await() }
                    }
                    emit(hydrated)
                }
            }
        }

    private val bulletinsFlow: Flow<List<Bulletin>> = tribeRepository.observeBulletins(tribeId)
    private val presentCountFlow: Flow<Int> = tribeRepository.observePresentCount(tribeId)

    val uiState: StateFlow<TribeDetailUiState> =
        combine(
            tribeFlow,
            hydratedEmbers,
            optimisticEchoToggles,
            bulletinsFlow,
            presentCountFlow
        ) { tribe, embers, toggles, bulletins, present ->
            val merged = embers
                .map { ember ->
                    if (ember.id in toggles) ember.copy(echoedByMe = !ember.echoedByMe) else ember
                }
                // Floating Lantern: a live pin floats to the top (stable sort keeps
                // newest-first within each group). F19.
                .sortedByDescending { it.pinnedNow() }
            TribeDetailUiState(
                tribe = tribe,
                embers = merged,
                bulletins = bulletins,
                presentCount = present,
                loading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TribeDetailUiState()
        )

    init {
        // Presence Beacon (F33): announce I'm here on enter, then re-light the beacon
        // every minute while this screen lives. The loop ends with the ViewModel; the
        // 2-minute presence window lets a stale heartbeat lapse on its own.
        viewModelScope.launch {
            while (isActive) {
                tribeRepository.heartbeat(tribeId)
                delay(60_000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Best-effort: drop my beacon as I leave. viewModelScope is already cancelled here,
        // so fire-and-forget on a detached IO scope; the 2-min window covers it if this misses.
        val repo = tribeRepository
        val id = tribeId
        CoroutineScope(Dispatchers.IO).launch { repo.clearPresence(id) }
    }

    fun loadMore() {
        limit.value += 20L
    }

    fun toggleMembership() {
        val tribe = uiState.value.tribe ?: return
        viewModelScope.launch {
            if (tribe.isMember) {
                tribeRepository.leaveTribe(tribe.id)
            } else {
                tribeRepository.joinTribe(tribe.id, tribe.name)
            }
        }
    }

    fun echo(emberId: String) {
        optimisticEchoToggles.value = optimisticEchoToggles.value + emberId
        viewModelScope.launch {
            // Snapshot reconciles the real state shortly after; clear the local
            // override whether the write succeeded or failed.
            emberRepository.toggleEcho(emberId)
            optimisticEchoToggles.value = optimisticEchoToggles.value - emberId
        }
    }

    fun coldMark(emberId: String, reason: String) {
        viewModelScope.launch {
            emberRepository.coldMark(emberId, reason)
        }
    }

    /** The signed-in uid — the screen compares it to tribe.createdBy to show Keeper tools. */
    val currentUserId: String? get() = emberRepository.currentUserId

    /** Cast a yes/no vote on a poll ember (F18). */
    fun votePoll(emberId: String, yes: Boolean) {
        viewModelScope.launch { emberRepository.votePoll(emberId, yes) }
    }

    /** Keeper pins/unpins this ember to the top of the tribe for an hour (F19). */
    fun togglePin(ember: Ember) {
        viewModelScope.launch { emberRepository.setPinned(ember.id, tribeId, !ember.pinnedNow()) }
    }

    /** Keeper blesses/un-blesses this ember as wisdom — gold, 30-day life (F23). */
    fun toggleWisdom(ember: Ember) {
        viewModelScope.launch { emberRepository.setWisdom(ember.id, tribeId, !ember.isWisdom) }
    }

    /** Keeper pins a notice to the Bulletin Board — title + body, 24h, max 5 (F21). */
    fun addBulletin(title: String, text: String, emoji: String) {
        viewModelScope.launch { tribeRepository.addBulletin(tribeId, title, text, emoji) }
    }

    /** Keeper takes a notice down early (F21). */
    fun deleteBulletin(bulletinId: String) {
        viewModelScope.launch { tribeRepository.deleteBulletin(tribeId, bulletinId) }
    }
}
