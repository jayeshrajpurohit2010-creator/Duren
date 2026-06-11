package com.duren.app.feature.tribes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.core.DomainError
import com.duren.app.data.tribe.TribeRepository
import com.duren.app.data.tribe.model.Tribe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TribesUiState {
    data object Loading : TribesUiState
    data object Empty : TribesUiState
    data class Content(val tribes: List<Tribe>) : TribesUiState
}

/** State of the "join with a code" dialog (F37). */
sealed interface JoinByCodeState {
    data object Idle : JoinByCodeState
    data object Checking : JoinByCodeState
    data class Error(val message: String) : JoinByCodeState
    data class Joined(val tribeId: String) : JoinByCodeState
}

@HiltViewModel
class TribesViewModel @Inject constructor(
    private val tribeRepository: TribeRepository
) : ViewModel() {

    init {
        // Bootstrap the pre-seeded tribe catalog the first time anyone opens Discover
        // on a fresh app. Idempotent — a no-op once any tribe exists.
        viewModelScope.launch { tribeRepository.seedDefaultTribes() }
    }

    val uiState: StateFlow<TribesUiState> = tribeRepository.observeTribes()
        .map { tribes ->
            if (tribes.isEmpty()) TribesUiState.Empty
            else TribesUiState.Content(tribes)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TribesUiState.Loading
        )

    fun toggleMembership(tribe: Tribe) {
        viewModelScope.launch {
            if (tribe.isMember) {
                tribeRepository.leaveTribe(tribe.id)
            } else {
                tribeRepository.joinTribe(tribe.id, tribe.name)
            }
        }
    }

    private val _joinByCode = MutableStateFlow<JoinByCodeState>(JoinByCodeState.Idle)
    val joinByCode: StateFlow<JoinByCodeState> = _joinByCode

    /** Six digits, one door (F37). On success the screen navigates into the tribe. */
    fun joinByCode(code: String) {
        _joinByCode.value = JoinByCodeState.Checking
        viewModelScope.launch {
            tribeRepository.joinByCode(code)
                .onSuccess { _joinByCode.value = JoinByCodeState.Joined(it) }
                .onFailure { e ->
                    val msg = (e as? DomainError)?.message ?: "Something went wrong. Try again."
                    _joinByCode.value = JoinByCodeState.Error(msg ?: "Something went wrong. Try again.")
                }
        }
    }

    /** Reset the dialog state after navigation or dismissal. */
    fun clearJoinByCode() {
        _joinByCode.value = JoinByCodeState.Idle
    }
}
