package com.duren.app.feature.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.duren.app.data.ember.model.PostMode
import com.duren.app.data.tribe.model.SubEmber
import com.duren.app.data.tribe.model.Tribe
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.components.EmberGlyph
import com.duren.app.ui.components.FloatingEmbers
import com.duren.app.ui.theme.LocalDurenColors
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing
import com.duren.app.ui.theme.Temperature
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    onPosted: () -> Unit,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val postState by viewModel.state.collectAsStateWithLifecycle()
    val myTribes by viewModel.myTribes.collectAsStateWithLifecycle()
    val subEmbers by viewModel.subEmbers.collectAsStateWithLifecycle()

    // Local compose-field state
    var bodyText by rememberSaveable { mutableStateOf("") }
    var selectedTribe by remember { mutableStateOf<Tribe?>(null) }
    // Sub-Embers (F36): an optional topic thread within the picked tribe.
    var selectedTopic by remember { mutableStateOf<SubEmber?>(null) }
    var selectedMode by remember { mutableStateOf(PostMode.Named) }
    // Fragment mode: hide the body past ~100 chars until a reader echoes.
    var fragment by rememberSaveable { mutableStateOf(false) }
    // Quick Poll: the body becomes a yes/no question. Mutually exclusive with Fragment.
    var poll by rememberSaveable { mutableStateOf(false) }
    // Up to [maxPhotos] photos on one ember. Plain remember (not Saveable): re-picking
    // after a rotation is a fine trade for not hand-rolling a List<Uri> bundle saver.
    val maxPhotos = 4
    var mediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Camera-first: the composer opens straight to the camera (BeReal-style). The
    // user captures a moment or skips to the text/gallery form.
    var cameraOpen by rememberSaveable { mutableStateOf(true) }

    // After a post lands we hold on this screen for one beat so the ember can lift
    // off (A2) before we hand back to the feed.
    var celebrating by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxPhotos)
    ) { uris -> if (uris.isNotEmpty()) mediaUris = (mediaUris + uris).take(maxPhotos) }

    if (cameraOpen) {
        CameraCapture(
            onCaptured = { uri ->
                mediaUris = (mediaUris + uri).take(maxPhotos)
                cameraOpen = false
            },
            onSkip = { cameraOpen = false }
        )
        return
    }

    // React to PostState.Posted — clear the form, then let the ember lift off (A2).
    // The release overlay calls onPosted() once its animation finishes, so we don't
    // navigate away mid-celebration.
    LaunchedEffect(postState) {
        if (postState is PostState.Posted) {
            viewModel.reset()
            bodyText = ""
            mediaUris = emptyList()
            selectedTribe = null
            selectedTopic = null
            viewModel.selectTribe(null)
            selectedMode = PostMode.Named
            fragment = false
            poll = false
            celebrating = true
        }
    }

    val isPosting = postState is PostState.Posting
    // A poll needs its question typed; otherwise text or a photo is enough.
    val canPost = !isPosting && (bodyText.isNotBlank() || mediaUris.isNotEmpty()) && (!poll || bodyText.isNotBlank())

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = LocalDurenColors.current.BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Compose", color = LocalDurenColors.current.TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                label = { Text(if (poll) "Ask a yes / no question" else "What's alive right now?") },
                supportingText = { Text("${bodyText.length}/500") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Tribe selector
            Column(verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                Text(
                    text = "Post to",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalDurenColors.current.TextMuted
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
                ) {
                    // "The Clearing" chip — tribe = null (global feed)
                    FilterChip(
                        selected = selectedTribe == null,
                        onClick = {
                            selectedTribe = null
                            selectedTopic = null
                            viewModel.selectTribe(null)
                        },
                        label = { Text("The Clearing") }
                    )
                    myTribes.forEach { tribe ->
                        FilterChip(
                            selected = selectedTribe?.id == tribe.id,
                            onClick = {
                                selectedTribe = tribe
                                selectedTopic = null
                                viewModel.selectTribe(tribe.id)
                            },
                            label = { Text(tribe.name) }
                        )
                    }
                }
                // Sub-Embers (F36): once a tribe with topics is picked, the ember can
                // land in one of its threads. Optional — "the whole fire" is default.
                if (selectedTribe != null && subEmbers.isNotEmpty()) {
                    Text(
                        text = "Into",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalDurenColors.current.TextMuted
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
                    ) {
                        FilterChip(
                            selected = selectedTopic == null,
                            onClick = { selectedTopic = null },
                            label = { Text("The whole fire") }
                        )
                        subEmbers.forEach { topic ->
                            FilterChip(
                                selected = selectedTopic?.id == topic.id,
                                onClick = { selectedTopic = topic },
                                label = { Text("#${topic.name}") }
                            )
                        }
                    }
                }
            }

            // Mode selector — three pills. Selected = teal on near-black, never white.
            Column(verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                Text(
                    text = "Post as",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalDurenColors.current.TextMuted
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
                ) {
                    PostMode.entries.forEach { mode ->
                        PostAsPill(
                            text = when (mode) {
                                PostMode.Named -> "Named"
                                PostMode.Anonymous -> "Anonymous"
                                PostMode.Confess -> "Confess"
                            },
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                    }
                }
                if (selectedMode.isMasked) {
                    Text(
                        text = "Posted without your name or avatar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalDurenColors.current.TextMuted
                    )
                }
            }

            // Fragment + Poll toggles — two different shapes an ember can take.
            // They're mutually exclusive: a poll has no "rest" to hide.
            Column(verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                    FilterChip(
                        selected = fragment,
                        onClick = {
                            fragment = !fragment
                            if (fragment) poll = false
                        },
                        label = { Text("Fragment") }
                    )
                    FilterChip(
                        selected = poll,
                        onClick = {
                            poll = !poll
                            if (poll) fragment = false
                        },
                        label = { Text("Poll") }
                    )
                }
                if (fragment) {
                    Text(
                        text = "Hidden past 100 characters until someone echoes to reveal the rest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalDurenColors.current.TextMuted
                    )
                }
                if (poll) {
                    Text(
                        text = "A yes / no question. Everyone sees the split after they vote.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalDurenColors.current.TextMuted
                    )
                }
            }

            // Media section — up to [maxPhotos] photos on one ember.
            Column(verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                if (mediaUris.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
                    ) {
                        mediaUris.forEach { uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(DurenShapes.medium)
                                )
                                SmallFloatingActionButton(
                                    onClick = { mediaUris = mediaUris - uri },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(DurenSpacing.space1)
                                        .size(28.dp),
                                    containerColor = LocalDurenColors.current.SurfaceElevated
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove image",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (mediaUris.size < maxPhotos) {
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
                            Text(if (mediaUris.isEmpty()) "Gallery" else "Add more")
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
                    color = LocalDurenColors.current.AccentTeal
                )
                Text(
                    text = "Catches fire? Reaches 72h",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = LocalDurenColors.current.TextMuted,
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

            // Release this ember — full-width teal pill, near-black label.
            Button(
                onClick = {
                    viewModel.post(
                        bodyText, selectedTribe, selectedMode, mediaUris, fragment, poll,
                        subEmber = selectedTopic
                    )
                },
                enabled = canPost,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalDurenColors.current.AccentTeal,
                    contentColor = LocalDurenColors.current.OnAccent,
                    disabledContainerColor = LocalDurenColors.current.SurfaceElevated,
                    disabledContentColor = LocalDurenColors.current.TextDisabled
                ),
                shape = DurenShapes.pill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = when {
                        isPosting -> "Releasing…"
                        poll -> "Open this poll 🔥"
                        else -> "Release this ember 🔥"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(DurenSpacing.space6))
        }
    }

    // A2 — the ember lifts off. Painted above the whole composer (top bar included)
    // until it finishes, then it hands back to the feed.
    if (celebrating) {
        EmberReleaseOverlay(
            onDone = {
                celebrating = false
                cameraOpen = true // next ember starts at the camera again
                onPosted()
            }
        )
    }
    }
}

/**
 * A2 — the post-send moment. The ember just composed lifts off the fire: a blazing
 * flame rises up and out of frame with sparks trailing it, while "Your ember is
 * burning" settles in beneath. One 0→1 driver runs the whole beat; when it lands we
 * hand back to the feed via [onDone]. Self-contained Canvas + [EmberGlyph], no assets
 * — the same language as the launch reveal.
 */
@Composable
private fun EmberReleaseOverlay(onDone: () -> Unit) {
    val colors = LocalDurenColors.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
        onDone()
    }
    val p = progress.value

    // Scrim: in fast, hold, then clear at the very end as we leave.
    val scrim = when {
        p < 0.12f -> p / 0.12f
        p > 0.85f -> (1f - p) / 0.15f
        else -> 1f
    }.coerceIn(0f, 1f)

    // The ember rises, grows into a full flame, then fades as it leaves the top.
    val rise = -280f * p
    val emberScale = 0.7f + 0.5f * (p / 0.45f).coerceAtMost(1f)
    val emberAlpha = when {
        p < 0.10f -> p / 0.10f
        p > 0.72f -> (1f - p) / 0.28f
        else -> 1f
    }.coerceIn(0f, 1f)

    // The line settles under the ember and holds, lifting a touch with it (parallax).
    val textAlpha = ((p - 0.18f) / 0.20f).coerceIn(0f, 1f) * scrim

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.BackgroundPrimary.copy(alpha = 0.94f * scrim)),
        contentAlignment = Alignment.Center
    ) {
        // Sparks rising through the whole frame.
        FloatingEmbers(modifier = Modifier.fillMaxSize(), count = 14)

        // A soft glow swelling at the base where the ember lifts off, then fading.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val burst = sin(p * Math.PI.toFloat())
            val center = Offset(size.width * 0.5f, size.height * 0.56f)
            val r = size.minDimension * (0.10f + 0.30f * p)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ReleaseGlow.copy(alpha = 0.5f * burst), Color.Transparent),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmberGlyph(
                temperature = Temperature.Blazing,
                size = 96.dp,
                pulse = 1f + 0.15f * sin(p * Math.PI.toFloat()),
                modifier = Modifier.graphicsLayer {
                    translationY = rise.dp.toPx()
                    scaleX = emberScale
                    scaleY = emberScale
                    alpha = emberAlpha
                }
            )
            Spacer(Modifier.height(DurenSpacing.space6))
            Text(
                text = "Your ember is burning 🔥",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.TextPrimary,
                modifier = Modifier
                    .alpha(textAlpha)
                    .graphicsLayer { translationY = (rise * 0.25f).dp.toPx() }
            )
            Spacer(Modifier.height(DurenSpacing.space2))
            Text(
                text = "It fades in 48 hours",
                style = MaterialTheme.typography.bodySmall,
                color = colors.TextMuted,
                modifier = Modifier.alpha(textAlpha)
            )
        }
    }
}

private val ReleaseGlow = Color(0xFFFFA040)

/** A "Post as" choice. Selected fills teal with near-black text; the rest stay dark. */
@Composable
private fun PostAsPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(DurenShapes.pill)
            .background(if (selected) LocalDurenColors.current.AccentTeal else LocalDurenColors.current.SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = DurenSpacing.space4, vertical = DurenSpacing.space2)
    ) {
        Text(
            text = text,
            color = if (selected) LocalDurenColors.current.OnAccent else LocalDurenColors.current.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}
