package com.duren.app.feature.tribes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.tribe.TribeRepository
import com.duren.app.data.tribe.model.Tribe
import com.duren.app.feature.nav.TribeDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One tribe's detail: a header (name, member count, join/leave) plus that tribe's
 * ember feed. Echo/cold-mark behaviour mirrors [com.duren.app.feature.feed.FeedViewModel].
 */
data class TribeDetailUiState(
    val tribe: Tribe? = null,
    val embers: List<Ember> = emptyList(),
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

    val uiState: StateFlow<TribeDetailUiState> =
        combine(tribeFlow, hydratedEmbers, optimisticEchoToggles) { tribe, embers, toggles ->
            val merged = embers.map { ember ->
                if (ember.id in toggles) ember.copy(echoedByMe = !ember.echoedByMe) else ember
            }
            TribeDetailUiState(tribe = tribe, embers = merged, loading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TribeDetailUiState()
        )

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
}
