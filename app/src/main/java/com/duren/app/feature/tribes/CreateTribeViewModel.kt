package com.duren.app.feature.tribes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.core.DomainError
import com.duren.app.data.tribe.TribeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CreateState {
    data object Idle : CreateState
    data object Saving : CreateState
    data class Error(val message: String) : CreateState
    data object Done : CreateState
}

@HiltViewModel
class CreateTribeViewModel @Inject constructor(
    private val tribeRepository: TribeRepository
) : ViewModel() {

    private val _state: MutableStateFlow<CreateState> = MutableStateFlow(CreateState.Idle)
    val state: StateFlow<CreateState> = _state.asStateFlow()

    fun create(name: String, description: String, genre: String) {
        viewModelScope.launch {
            _state.value = CreateState.Saving
            val result = tribeRepository.createTribe(name, description, genre)
            _state.value = result.fold(
                onSuccess = { CreateState.Done },
                onFailure = { error ->
                    CreateState.Error(
                        (error as? DomainError)?.message ?: "Something went wrong."
                    )
                }
            )
        }
    }
}
