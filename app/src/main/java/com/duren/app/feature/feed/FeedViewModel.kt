package com.duren.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
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

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Content(val embers: List<Ember>) : FeedUiState
}

/**
 * Drives The Clearing feed with infinite scroll, optimistic echo toggles,
 * and cold-mark delegation to [EmberRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val emberRepository: EmberRepository
) : ViewModel() {

    /** Current Firestore limit; bumped by [loadMore]. */
    private val limit = MutableStateFlow(20L)

    /**
     * Set of ember IDs whose echo state the user has toggled locally but whose
     * server snapshot hasn't reflected yet. XOR-ed with the hydrated [echoedByMe]
     * so the heart flips instantly on tap.
     */
    private val optimisticEchoToggles = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Hot flow of hydrated ember lists — [Ember.echoedByMe] resolved in parallel
     * for every incoming snapshot.
     */
    private val hydratedList: Flow<List<Ember>> = limit
        .flatMapLatest { currentLimit ->
            flow {
                emberRepository.observeFeed(currentLimit).collect { rawEmbers ->
                    val hydrated = coroutineScope {
                        rawEmbers.map { ember ->
                            async { ember.copy(echoedByMe = emberRepository.hasEchoed(ember.id)) }
                        }.map { it.await() }
                    }
                    emit(hydrated)
                }
            }
        }

    /**
     * The single source of truth for the feed UI.
     * Starts as [FeedUiState.Loading]; transitions to [Empty] or [Content] once
     * the first hydrated snapshot arrives.
     */
    val uiState: StateFlow<FeedUiState> = combine(hydratedList, optimisticEchoToggles) { list, toggles ->
        // Apply optimistic overrides: flip echoedByMe for any id in the toggle set
        val merged = list.map { ember ->
            if (ember.id in toggles) ember.copy(echoedByMe = !ember.echoedByMe) else ember
        }
        if (merged.isEmpty()) FeedUiState.Empty
        else FeedUiState.Content(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState.Loading
    )

    /** Increase the Firestore limit to fetch the next page of embers. */
    fun loadMore() {
        limit.value += 20L
    }

    /**
     * Toggle echo for an ember. The heart flips immediately via [optimisticEchoToggles];
     * the Firestore snapshot reconciles the persisted state shortly after, at which
     * point the local override is cleared so the server value takes over.
     */
    fun echo(emberId: String) {
        // Optimistic: add to toggle set so the XOR flips the heart immediately
        optimisticEchoToggles.value = optimisticEchoToggles.value + emberId

        viewModelScope.launch {
            val result = emberRepository.toggleEcho(emberId)
            // Server confirmed: clear the local override — the snapshot now has the real state
            if (result.isSuccess) {
                optimisticEchoToggles.value = optimisticEchoToggles.value - emberId
            }
            // Server failed: revert by removing the toggle (XOR back to original)
            if (result.isFailure) {
                optimisticEchoToggles.value = optimisticEchoToggles.value - emberId
            }
        }
    }

    /** Quietly cold-marks an ember with the given reason. Fire and forget. */
    fun coldMark(emberId: String, reason: String) {
        viewModelScope.launch {
            emberRepository.coldMark(emberId, reason)
        }
    }
}
