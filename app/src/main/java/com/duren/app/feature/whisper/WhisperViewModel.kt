package com.duren.app.feature.whisper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.Whisper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs one ember's inline whisper (comment) thread. Obtained per-card via
 * `hiltViewModel(key = emberId)`, then [bind] points it at that ember so the
 * thread only streams while it's expanded.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WhisperViewModel @Inject constructor(
    private val emberRepository: EmberRepository
) : ViewModel() {

    private val emberId = MutableStateFlow<String?>(null)

    val currentUserId: String? get() = emberRepository.currentUserId

    val whispers: StateFlow<List<Whisper>> = emberId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else emberRepository.observeWhispers(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun bind(id: String) {
        if (emberId.value != id) emberId.value = id
    }

    fun add(text: String, isAnonymous: Boolean) {
        val id = emberId.value ?: return
        viewModelScope.launch { emberRepository.addWhisper(id, text, isAnonymous) }
    }

    fun delete(whisperId: String) {
        val id = emberId.value ?: return
        viewModelScope.launch { emberRepository.deleteWhisper(id, whisperId) }
    }
}
