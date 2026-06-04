package com.duren.app.feature.tribes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.ui.theme.DurenColors
import com.duren.app.ui.theme.DurenSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTribeScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateTribeViewModel = hiltViewModel()
) {
    val createState by viewModel.state.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val isSaving = createState is CreateState.Saving
    val isCreateEnabled = name.isNotBlank() && !isSaving

    LaunchedEffect(createState) {
        if (createState is CreateState.Done) {
            onCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New tribe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DurenSpacing.space4, vertical = DurenSpacing.space2)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("What's your tribe called?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(DurenSpacing.space3))

            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Genre") },
                placeholder = { Text("Anime, Gaming, K-pop…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(DurenSpacing.space3))

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 200) description = it },
                label = { Text("Description") },
                placeholder = { Text("What draws people to this fire?") },
                supportingText = { Text("${description.length}/200") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(DurenSpacing.space4))

            Button(
                onClick = { viewModel.create(name, description, genre) },
                enabled = isCreateEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isSaving) "Lighting the fire…" else "Create tribe",
                    fontWeight = FontWeight.Medium
                )
            }

            if (createState is CreateState.Error) {
                Spacer(Modifier.height(DurenSpacing.space2))
                Text(
                    text = (createState as CreateState.Error).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = DurenColors.SemanticError,
                    modifier = Modifier.padding(horizontal = DurenSpacing.space1)
                )
            }

            Spacer(Modifier.height(DurenSpacing.space6))
        }
    }
}
