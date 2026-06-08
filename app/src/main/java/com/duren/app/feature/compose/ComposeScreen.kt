package com.duren.app.feature.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.duren.app.data.ember.model.PostMode
import com.duren.app.data.tribe.model.Tribe
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    onPosted: () -> Unit,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val postState by viewModel.state.collectAsStateWithLifecycle()
    val myTribes by viewModel.myTribes.collectAsStateWithLifecycle()

    // Local compose-field state
    var bodyText by rememberSaveable { mutableStateOf("") }
    var selectedTribe by remember { mutableStateOf<Tribe?>(null) }
    var selectedMode by remember { mutableStateOf(PostMode.Named) }
    // Fragment mode: hide the body past ~100 chars until a reader echoes.
    var fragment by rememberSaveable { mutableStateOf(false) }
    // Uri is Parcelable, so rememberSaveable keeps the captured photo across rotation.
    var mediaUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // Camera-first: the composer opens straight to the camera (BeReal-style). The
    // user captures a moment or skips to the text/gallery form.
    var cameraOpen by rememberSaveable { mutableStateOf(true) }

    // Photo picker launcher
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) mediaUri = uri }

    if (cameraOpen) {
        CameraCapture(
            onCaptured = { uri ->
                mediaUri = uri
                cameraOpen = false
            },
            onSkip = { cameraOpen = false }
        )
        return
    }

    // React to PostState.Posted
    LaunchedEffect(postState) {
        if (postState is PostState.Posted) {
            viewModel.reset()
            // Clear fields before navigating away
            bodyText = ""
            mediaUri = null
            selectedTribe = null
            selectedMode = PostMode.Named
            fragment = false
            cameraOpen = true // next ember starts at the camera again
            onPosted()
        }
    }

    val isPosting = postState is PostState.Posting
    val canPost = !isPosting && (bodyText.isNotBlank() || mediaUri != null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share an ember") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DurenSpacing.space4, vertical = DurenSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(DurenSpacing.space4)
        ) {

            // Body text field
            OutlinedTextField(
                value = bodyText,
                onValueChange = { if (it.length <= 500) bodyText = it },
                label = { Text("What's alive right now?") },
                supportingText = { Text("${bodyText.length}/500") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Tribe selector
            Column(verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                Text(
                    text = "Post to",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
                ) {
                    // "The Clearing" chip — tribe = null (global feed)
                    FilterChip(
                        selected = selectedTribe == null,
                        onClick = { selectedTribe = null },
                        label = { Text("The Clearing") }
                    )
                    myTribes.forEach { tribe ->
                        FilterChip(
                            selected = selectedTribe?.id == tribe.id,
                            onClick = { selectedTribe = tribe },
                            label = { Text(tribe.name) }
                        )
                    }
                }
            }

            // Mode selector
            Column(verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                Text(
                    text = "Post as",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
                ) {
                    PostMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = {
                                Text(
                                    when (mode) {
                                        PostMode.Named -> "Named"
                                        PostMode.Anonymous -> "Anonymous"
                                        PostMode.Confess -> "Confess"
                                    }
                                )
                            }
                        )
                    }
                }
                if (selectedMode.isMasked) {
                    Text(
                        text = "Posted without your name or avatar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fragment toggle — only meaningful once the body runs long.
            Column(verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                FilterChip(
                    selected = fragment,
                    onClick = { fragment = !fragment },
                    label = { Text("Fragment") }
                )
                if (fragment) {
                    Text(
                        text = "Hidden past 100 characters until someone echoes to reveal the rest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Media section
            Column(verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                if (mediaUri == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                        OutlinedButton(
                            onClick = { cameraOpen = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Camera")
                        }
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            DurenIcon(
                                DurenIcon.Plus,
                                size = 18.dp,
                                modifier = Modifier.padding(end = DurenSpacing.space2)
                            )
                            Text("Gallery")
                        }
                    }
                } else {
                    Box {
                        AsyncImage(
                            model = mediaUri,
                            contentDescription = "Selected image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(DurenShapes.medium)
                        )
                        SmallFloatingActionButton(
                            onClick = { mediaUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(DurenSpacing.space2),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove image",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Locked lifespan label — static, no picker (PRD §3.3)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FADES IN 48h",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = DurenShapes.pill
                        )
                        .padding(
                            horizontal = DurenSpacing.space4,
                            vertical = DurenSpacing.space2
                        )
                )
                Text(
                    text = "Catches fire? Reaches 72h",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DurenSpacing.space2)
                )
            }

            // Error message
            if (postState is PostState.Error) {
                Text(
                    text = (postState as PostState.Error).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(DurenSpacing.space2))

            // Send up button
            Button(
                onClick = {
                    viewModel.post(bodyText, selectedTribe, selectedMode, mediaUri, fragment)
                },
                enabled = canPost,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isPosting) "Sending…" else "Send up",
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(DurenSpacing.space6))
        }
    }
}
