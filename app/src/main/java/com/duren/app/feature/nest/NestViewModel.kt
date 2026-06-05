package com.duren.app.feature.nest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.data.lantern.LanternRepository
import com.duren.app.data.lantern.model.Lantern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which set of lanterns the Nest is currently showing. */
enum class NestTab { Wandering, Yours }

sealed interface NestUiState {
    data object Loading : NestUiState
    data class Content(
        val tab: NestTab,
        val lanterns: List<Lantern>
    ) : NestUiState
}

/** State of the "light a lantern" surface. */
sealed interface LightState {
    data object Idle : LightState
    data object Saving : LightState
    data object Done : LightState
    data class Error(val message: String) : LightState
}

private const val FEED_LIMIT = 60L

@HiltViewModel
class NestViewModel @Inject constructor(
    private val lanternRepository: LanternRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(NestTab.Wandering)

    private val _lightState = MutableStateFlow<LightState>(LightState.Idle)
    val lightState: StateFlow<LightState> = _lightState.asStateFlow()

    val uiState: StateFlow<NestUiState> = combine(
        selectedTab,
        lanternRepository.observeDiscoverable(FEED_LIMIT),
        lanternRepository.observeMine(FEED_LIMIT)
    ) { tab, discoverable, mine ->
        val lanterns = if (tab == NestTab.Wandering) discoverable else mine
        NestUiState.Content(tab = tab, lanterns = lanterns)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NestUiState.Loading
    )

    fun selectTab(tab: NestTab) {
        selectedTab.value = tab
    }

    fun lightLantern(text: String) {
        if (_lightState.value is LightState.Saving) return
        _lightState.value = LightState.Saving
        viewModelScope.launch {
            val result = lanternRepository.lightLantern(text)
            _lightState.value = result.fold(
                onSuccess = { LightState.Done },
                onFailure = { LightState.Error(it.message ?: "Something went wrong. Try again.") }
            )
        }
    }

    fun resetLightState() {
        _lightState.value = LightState.Idle
    }
}
