package com.duren.app.feature.signal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.profile.ProfileRepository
import com.duren.app.data.signal.SignalRepository
import com.duren.app.data.signal.model.Signal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Your Signals (notifications), with the actor's profile hydrated for display. */
@HiltViewModel
class SignalViewModel @Inject constructor(
    private val signalRepository: SignalRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val signals: StateFlow<List<Signal>> = signalRepository.observeSignals()
        .map { list ->
            list.map { it.copy(fromProfile = profileRepository.getProfile(it.fromUserId)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Mark everything read — called once when the screen opens. */
    fun markAllRead() = viewModelScope.launch { signalRepository.markAllRead() }
}
