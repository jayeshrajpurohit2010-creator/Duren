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
import kotlinx.coroutines.flow.update
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
     * Pending optimistic echo intents: emberId → the state the user just chose
     * (true = echoed, false = un-echoed). An entry lives only until a server
     * snapshot hydrates that ember with a matching [Ember.echoedByMe], at which
     * point it is dropped so the persisted value takes over. This survives feed
     * snapshots (which only carry the raw count, not per-user echo state) and
     * avoids the heart flickering back between RPC-success and snapshot arrival.
     */
    private val pendingEchoIntents = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    /** Ember ids the user just deleted — hidden immediately, before the snapshot catches up. */
    private val deletedIds = MutableStateFlow<Set<String>>(emptySet())

    /** The signed-in uid, so the UI can show Delete only on the user's own embers. */
    val currentUserId: String? get() = emberRepository.currentUserId

    /**
     * Hot flow of hydrated ember lists — [Ember.echoedByMe] resolved in parallel
     * (off the main thread, in this coroutine) for every incoming snapshot.
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
                    // Rank by heat at snapshot time (stable between snapshots so the
                    // list doesn't reshuffle under the user on every echo tap).
                    emit(rankByHeat(hydrated))
                }
            }
        }

    /**
     * The single source of truth for the feed UI.
     * Starts as [FeedUiState.Loading]; transitions to [Empty] or [Content] once
     * the first hydrated snapshot arrives.
     */
    val uiState: StateFlow<FeedUiState> = combine(
        hydratedList,
        pendingEchoIntents,
        deletedIds
    ) { rawList, intents, deleted ->
        // Drop embers the user just deleted (until the snapshot stops returning them).
        val list = if (deleted.isEmpty()) rawList else rawList.filterNot { it.id in deleted }
        // Reconcile: drop any pending intent the server has now caught up to, so
        // we never double-apply an override on top of an already-correct snapshot.
        if (intents.isNotEmpty()) {
            val reconciled = intents.filterNot { (id, desired) ->
                list.firstOrNull { it.id == id }?.echoedByMe == desired
            }
            if (reconciled.size != intents.size) {
                pendingEchoIntents.value = reconciled
            }
        }

        val merged = list.map { ember ->
            val desired = intents[ember.id]
            when {
                desired == null || desired == ember.echoedByMe -> ember
                else -> {
                    // Apply optimistic override and nudge the count so the number
                    // moves with the heart until the snapshot confirms it.
                    val delta = if (desired) 1 else -1
                    ember.copy(
                        echoedByMe = desired,
                        echoCount = (ember.echoCount + delta).coerceAtLeast(0)
                    )
                }
            }
        }
        if (merged.isEmpty()) FeedUiState.Empty
        else FeedUiState.Content(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState.Loading
    )

    /**
     * Phase-1 client-side heat ranking — a faithful slice of the Algorithm Spec's
     * scoring layer, runnable without Cloud Functions. Order embers by engagement
     * lifted by recency and damped by age (classic gravity decay), so a fresh post
     * still surfaces while a post that's "catching fire" outranks an older quiet one.
     * Cold marks subtract from heat (light anti-gaming). The full 7-layer,
     * personalized, server-side system replaces this in Phase 4.
     *
     *   heat = (echoes − coldMarks, floored at 0, +1) / (ageHours + 2) ^ GRAVITY
     */
    private fun rankByHeat(embers: List<Ember>): List<Ember> {
        val now = System.currentTimeMillis()
        return embers.sortedByDescending { ember ->
            val createdMs = ember.createdAt?.toDate()?.time ?: now
            val ageHours = (now - createdMs).coerceAtLeast(0L) / 3_600_000.0
            val engagement = (ember.echoCount - ember.coldMarkCount).coerceAtLeast(0) + 1
            engagement.toDouble() / Math.pow(ageHours + 2.0, HEAT_GRAVITY)
        }
    }

    /** Increase the Firestore limit to fetch the next page of embers. */
    fun loadMore() {
        limit.value += 20L
    }

    /**
     * Toggle echo for an ember. The heart flips immediately via [pendingEchoIntents];
     * the Firestore snapshot reconciles the persisted state shortly after, at which
     * point the local intent is dropped (see the reconcile step in [uiState]).
     */
    fun echo(emberId: String) {
        // Desired = opposite of the currently displayed state (intent override
        // if one is pending, otherwise the last hydrated value).
        val currentlyEchoed = when (val state = uiState.value) {
            is FeedUiState.Content -> state.embers.firstOrNull { it.id == emberId }?.echoedByMe
            else -> null
        } ?: false
        val desired = !currentlyEchoed

        pendingEchoIntents.update { it + (emberId to desired) }

        viewModelScope.launch {
            val result = emberRepository.toggleEcho(emberId)
            result.onSuccess { nowEchoed ->
                // If the server landed on a different state than we predicted
                // (e.g. a stale local read), correct the intent so the heart
                // settles on the truth instead of the snapshot reconciling it.
                pendingEchoIntents.update { it + (emberId to nowEchoed) }
            }.onFailure {
                // Revert: drop the optimistic intent so the hydrated value shows.
                pendingEchoIntents.update { it - emberId }
            }
        }
    }

    /** Quietly cold-marks an ember with the given reason. Fire and forget. */
    fun coldMark(emberId: String, reason: String) {
        viewModelScope.launch {
            emberRepository.coldMark(emberId, reason)
        }
    }

    /**
     * Delete one of the user's own embers. Hidden from the feed instantly; if the
     * server delete fails (e.g. rules not yet deployed) the id is un-hidden so the
     * ember reappears rather than silently lying that it's gone.
     */
    fun deleteEmber(emberId: String) {
        deletedIds.update { it + emberId }
        viewModelScope.launch {
            emberRepository.deleteEmber(emberId).onFailure {
                deletedIds.update { ids -> ids - emberId }
            }
        }
    }

    private companion object {
        // Higher = recency wins harder over raw echo count. 1.5 ≈ Hacker-News gravity.
        const val HEAT_GRAVITY = 1.5
    }
}
