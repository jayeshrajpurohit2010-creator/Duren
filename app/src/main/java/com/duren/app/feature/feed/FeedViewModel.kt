package com.duren.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Ember
import com.duren.app.data.nest.NestRepository
import com.duren.app.data.signal.SignalRepository
import com.duren.app.data.tribe.TribeRepository
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
 * The Clearing's discovery surfaces (design Screen 4). All four are client-side
 * re-sorts of the same live feed — no extra queries, no backend. Labels only:
 * the active tab is marked by a teal underline, not an emoji.
 */
enum class FeedTab(val label: String) {
    Campfire("The Campfire"),
    BurningNow("Burning Now"),
    AboutToFade("About to Fade"),
    ColdEmbers("Cold Embers")
}

/**
 * Drives The Clearing feed with infinite scroll, optimistic echo toggles,
 * and cold-mark delegation to [EmberRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val emberRepository: EmberRepository,
    private val tribeRepository: TribeRepository,
    private val nestRepository: NestRepository,
    signalRepository: SignalRepository
) : ViewModel() {

    /** Unread Signal count for the bell badge in the top bar. */
    val unreadSignals: StateFlow<Int> = signalRepository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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

    /** Which discovery tab is active. Pure client-side re-sort of the same feed. */
    private val selectedTab = MutableStateFlow(FeedTab.Campfire)
    val tab: StateFlow<FeedTab> = selectedTab

    fun selectTab(newTab: FeedTab) {
        selectedTab.value = newTab
    }

    /** The signed-in uid, so the UI can show Delete only on the user's own embers. */
    val currentUserId: String? get() = emberRepository.currentUserId

    /**
     * The user's affinity graph — tribes they've joined and the people in their Nest.
     * Embers from either get boosted up [rankByHeat], so joining a tribe actually
     * changes the home feed. Emits promptly (empty sets when you've joined nothing), so
     * the feed never waits on it — and the global feed still flows underneath, so a fresh
     * account staring at the Clearing is never empty.
     */
    private val affinity: Flow<Affinity> = combine(
        tribeRepository.observeMyTribeIds(),
        nestRepository.observeMemberIds()
    ) { tribeIds, nestIds -> Affinity(tribeIds, nestIds.toSet()) }

    /**
     * Echo-hydrated feed snapshots, unranked. [Ember.echoedByMe] is resolved in parallel
     * (off the main thread, in this coroutine) per snapshot. Hydration is the costly
     * part, so it stays keyed only on [limit]; an affinity change re-ranks the same
     * snapshot without re-hydrating it.
     */
    private val hydratedRaw: Flow<List<Ember>> = limit
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
     * The hydrated feed, heat-ranked with the affinity boost folded in. Ranked at
     * snapshot time (stable between snapshots so an echo tap never reshuffles the list
     * under the user's finger).
     */
    private val hydratedList: Flow<List<Ember>> =
        combine(hydratedRaw, affinity) { hydrated, aff -> rankByHeat(hydrated, aff) }

    /**
     * The single source of truth for the feed UI.
     * Starts as [FeedUiState.Loading]; transitions to [Empty] or [Content] once
     * the first hydrated snapshot arrives.
     */
    val uiState: StateFlow<FeedUiState> = combine(
        hydratedList,
        pendingEchoIntents,
        deletedIds,
        selectedTab
    ) { rawList, intents, deleted, currentTab ->
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

        // Order by the active tab using snapshot values (NOT the optimistic count),
        // so echo taps never reshuffle the list under the user's finger — only a
        // fresh snapshot re-orders. Campfire is already heat-ranked by [rankByHeat].
        val ordered = when (currentTab) {
            FeedTab.Campfire -> list
            FeedTab.BurningNow ->
                list.filter { it.echoCount > 0 }.sortedByDescending { it.echoCount }
            FeedTab.AboutToFade ->
                list.sortedBy { it.expiresAt?.seconds ?: Long.MAX_VALUE }
            FeedTab.ColdEmbers ->
                list.sortedWith(compareBy({ it.echoCount }, { it.createdAt?.seconds ?: 0L }))
        }

        val merged = ordered.map { ember ->
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
     * Phase-1 client-side heat ranking. The scoring itself lives in [FeedRanking] as a
     * pure, tested function; here we just feed it the clock and the affinity predicate.
     */
    private fun rankByHeat(embers: List<Ember>, affinity: Affinity): List<Ember> =
        FeedRanking.rank(embers, System.currentTimeMillis()) { affinity.covers(it) }

    /** A snapshot of what the user follows — joined tribes + Nest — for the feed boost. */
    private data class Affinity(val tribeIds: Set<String>, val nestIds: Set<String>) {
        fun covers(ember: Ember): Boolean =
            (ember.tribeId != null && ember.tribeId in tribeIds) || ember.authorId in nestIds
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

    /** Cast a yes/no vote on a poll ember (F18). The live tally arrives via snapshot. */
    fun votePoll(emberId: String, yes: Boolean) {
        viewModelScope.launch {
            emberRepository.votePoll(emberId, yes)
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

}
