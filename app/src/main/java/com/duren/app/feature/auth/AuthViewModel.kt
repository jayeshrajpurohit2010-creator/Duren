package com.duren.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duren.app.core.DomainError
import com.duren.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { SIGN_IN, SIGN_UP }

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

data class AuthFormState(
    val mode: AuthMode = AuthMode.SIGN_UP,
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val password: String = "",
    val usernameAvailable: Boolean? = null,
    val ui: AuthUiState = AuthUiState.Idle
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _form = MutableStateFlow(AuthFormState())
    val form: StateFlow<AuthFormState> = _form.asStateFlow()

    fun setMode(mode: AuthMode) = _form.update { it.copy(mode = mode, ui = AuthUiState.Idle) }
    fun setEmail(v: String) = _form.update { it.copy(email = v) }
    fun setUsername(v: String) = _form.update {
        it.copy(username = v.lowercase(), usernameAvailable = null)
    }
    fun setDisplayName(v: String) = _form.update { it.copy(displayName = v) }
    fun setPassword(v: String) = _form.update { it.copy(password = v) }
    fun clearError() = _form.update {
        if (it.ui is AuthUiState.Error) it.copy(ui = AuthUiState.Idle) else it
    }

    fun checkUsername() {
        val username = _form.value.username
        if (username.isBlank()) return
        viewModelScope.launch {
            val available = authRepository.isUsernameAvailable(username)
            _form.update { it.copy(usernameAvailable = available) }
        }
    }

    fun submit() {
        val state = _form.value
        _form.update { it.copy(ui = AuthUiState.Loading) }
        viewModelScope.launch {
            val result = when (state.mode) {
                AuthMode.SIGN_UP -> authRepository.signUp(
                    email = state.email,
                    password = state.password,
                    username = state.username,
                    displayName = state.displayName
                )
                AuthMode.SIGN_IN -> authRepository.signIn(state.email, state.password)
            }
            result.onSuccess {
                _form.update { it.copy(ui = AuthUiState.Success) }
            }.onFailure { error ->
                val message = (error as? DomainError)?.message
                    ?: DomainError.Unknown.message
                    ?: "Something went wrong."
                _form.update { it.copy(ui = AuthUiState.Error(message)) }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _form.value.email
        if (email.isBlank()) return
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
            _form.update {
                it.copy(ui = AuthUiState.Error("If that email exists, a reset link is on the way."))
            }
        }
    }
}
