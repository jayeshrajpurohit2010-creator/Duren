package com.duren.app.feature.compose

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.core.DomainError
import com.duren.app.data.ember.EmberRepository
import com.duren.app.data.ember.model.PostMode
import com.duren.app.data.tribe.TribeRepository
import com.duren.app.data.tribe.model.Tribe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PostState {
    data object Idle : PostState
    data object Posting : PostState
    data class Error(val message: String) : PostState
    data object Posted : PostState
}

@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val emberRepository: EmberRepository,
    tribeRepository: TribeRepository
) : ViewModel() {

    val myTribes: StateFlow<List<Tribe>> = tribeRepository.observeMyTribes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state: MutableStateFlow<PostState> = MutableStateFlow(PostState.Idle)
    val state: StateFlow<PostState> = _state.asStateFlow()

    fun post(
        text: String,
        tribe: Tribe?,
        mode: PostMode,
        mediaUri: Uri?,
        isFragment: Boolean = false,
        isPoll: Boolean = false
    ) {
        viewModelScope.launch {
            _state.value = PostState.Posting
            val result = emberRepository.createEmber(
                text = text,
                tribeId = tribe?.id,
                tribeName = tribe?.name ?: "",
                mode = mode,
                mediaUri = mediaUri,
                isFragment = isFragment,
                isPoll = isPoll
            )
            _state.value = result.fold(
                onSuccess = { PostState.Posted },
                onFailure = { error ->
                    PostState.Error(
                        (error as? DomainError)?.message ?: "Something went wrong."
                    )
                }
            )
        }
    }

    fun reset() {
        _state.value = PostState.Idle
    }
}
