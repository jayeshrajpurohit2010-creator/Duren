package com.duren.app.feature.nest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.lantern.model.Lantern
import com.duren.app.ui.animation.EmptyState
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.components.ExpiryTimer
import com.duren.app.ui.theme.DurenColors
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing
import androidx.compose.foundation.clickable

private const val MAX_LANTERN_TEXT = 280

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestScreen(
    viewModel: NestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lightState by viewModel.lightState.collectAsStateWithLifecycle()

    var showLightSheet by remember { mutableStateOf(false) }

    // When a lantern is successfully lit, close the surface and reset.
    LaunchedEffect(lightState) {
        if (lightState is LightState.Done) {
            showLightSheet = false
            viewModel.resetLightState()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nest") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showLightSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Light a lantern")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val tab = (uiState as? NestUiState.Content)?.tab ?: NestTab.Wandering
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DurenSpacing.space4, vertical = DurenSpacing.space2)
            ) {
                SegmentedButton(
                    selected = tab == NestTab.Wandering,
                    onClick = { viewModel.selectTab(NestTab.Wandering) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Wandering") }
                SegmentedButton(
                    selected = tab == NestTab.Yours,
                    onClick = { viewModel.selectTab(NestTab.Yours) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Yours") }
            }

            when (val state = uiState) {
                is NestUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = DurenSpacing.space4),
                        verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                    ) {
                        Spacer(Modifier.height(DurenSpacing.space3))
                        repeat(4) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                            )
                        }
                    }
                }

                is NestUiState.Content -> {
                    if (state.lanterns.isEmpty()) {
                        val (title, body) = if (state.tab == NestTab.Wandering) {
                            "Nothing adrift yet." to "Light a lantern. Someone wandering might find it."
                        } else {
                            "You haven't lit any lanterns." to "Set one adrift and let it wander."
                        }
                        EmptyState(title = title, body = body, emoji = "🏮")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(DurenSpacing.space4),
                            verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
                        ) {
                            items(state.lanterns, key = { it.id }) { lantern ->
                                val isWandering = state.tab == NestTab.Wandering
                                LanternCard(
                                    lantern = lantern,
                                    mine = !isWandering,
                                    onMarkFound = if (isWandering) {
                                        { viewModel.markFound(lantern.id) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLightSheet) {
        LightLanternDialog(
            lightState = lightState,
            onDismiss = {
                showLightSheet = false
                viewModel.resetLightState()
            },
            onLight = { viewModel.lightLantern(it) }
        )
    }
}

@Composable
private fun LanternCard(
    lantern: Lantern,
    mine: Boolean,
    onMarkFound: (() -> Unit)? = null
) {
    // Track whether the user has tapped this card in the current session so we
    // only call markFound once per tap (not on every recomposition).
    var tapped by remember(lantern.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onMarkFound != null) {
                    Modifier.clickable {
                        if (!tapped) {
                            tapped = true
                            onMarkFound()
                        }
                    }
                } else Modifier
            ),
        shape = DurenShapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DurenSpacing.space4)
        ) {
            // Soft "found in the dark" framing — never reveals the author.
            Text(
                text = if (mine) "🏮 your lantern" else "🏮 found in the dark",
                style = MaterialTheme.typography.labelSmall,
                color = DurenColors.TextMuted
            )

            Spacer(Modifier.height(DurenSpacing.space2))

            Text(
                text = lantern.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(DurenSpacing.space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExpiryTimer(expiresAt = lantern.expiresAt, extended = false)
                Text(
                    text = "🏮 found by ${lantern.foundCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DurenColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun LightLanternDialog(
    lightState: LightState,
    onDismiss: () -> Unit,
    onLight: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val isSaving = lightState is LightState.Saving
    val canLight = text.trim().isNotEmpty() && !isSaving

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Light a lantern") },
        text = {
            Column {
                Text(
                    text = "Anonymous. Set it adrift for someone wandering to find.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(DurenSpacing.space2))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_LANTERN_TEXT) text = it },
                    placeholder = { Text("Say something into the dark…") },
                    supportingText = { Text("${text.length}/$MAX_LANTERN_TEXT") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (lightState is LightState.Error) {
                    Spacer(Modifier.height(DurenSpacing.space1))
                    Text(
                        text = lightState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = DurenColors.SemanticError
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onLight(text) }, enabled = canLight) {
                Text(
                    text = if (isSaving) "Lighting…" else "Light it",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}
