package com.duren.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.ui.animation.DurenToast
import com.duren.app.ui.theme.DurenSpacing

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.form.collectAsStateWithLifecycle()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.ui) {
        if (state.ui is AuthUiState.Success) onAuthenticated()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(DurenSpacing.space6)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.mode == AuthMode.SIGN_UP) "The campfire is lit." else "Welcome back.",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (state.mode == AuthMode.SIGN_UP) {
                    "A social network for people who show up, not perform."
                } else {
                    "The fire's been waiting."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DurenSpacing.space6))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::setEmail,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DurenSpacing.space3))

            if (state.mode == AuthMode.SIGN_UP) {
                OutlinedTextField(
                    value = state.username,
                    onValueChange = {
                        viewModel.setUsername(it)
                    },
                    label = { Text("Username (lowercase)") },
                    singleLine = true,
                    supportingText = {
                        when (state.usernameAvailable) {
                            true -> Text("Available")
                            false -> Text("That name's already at the fire.")
                            null -> Text("3–30 chars, a–z, 0–9, _")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = viewModel::checkUsername,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Check availability")
                }
                Spacer(Modifier.height(DurenSpacing.space3))

                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = viewModel::setDisplayName,
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(DurenSpacing.space3))
            }

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Hide" else "Show")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DurenSpacing.space5))

            Button(
                onClick = viewModel::submit,
                enabled = state.ui != AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.ui == AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(if (state.mode == AuthMode.SIGN_UP) "Light the fire" else "Sign in")
                }
            }
            Spacer(Modifier.height(DurenSpacing.space3))

            TextButton(
                onClick = {
                    viewModel.setMode(
                        if (state.mode == AuthMode.SIGN_UP) AuthMode.SIGN_IN else AuthMode.SIGN_UP
                    )
                }
            ) {
                Text(
                    if (state.mode == AuthMode.SIGN_UP) {
                        "Already have an account? Sign in"
                    } else {
                        "New here? Light a fire"
                    }
                )
            }

            if (state.mode == AuthMode.SIGN_IN) {
                TextButton(onClick = viewModel::sendPasswordReset) {
                    Text("Forgot password?")
                }
            }
        }

        val toastMsg = (state.ui as? AuthUiState.Error)?.message
        DurenToast(message = toastMsg, onDismiss = viewModel::clearError)
    }
}
