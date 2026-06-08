package com.duren.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.theme.DurenAccent
import com.duren.app.ui.theme.DurenAvatarColors
import com.duren.app.ui.theme.DurenColors
import com.duren.app.ui.theme.DurenSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val avatarUploading by viewModel.avatarUploading.collectAsStateWithLifecycle()
    val passwordResetSent by viewModel.passwordResetSent.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val accountDeleted by viewModel.accountDeleted.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    LaunchedEffect(accountDeleted) {
        if (accountDeleted) onSignedOut()
    }

    val context = LocalContext.current
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.setAvatarPhoto(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val p = profile
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        // Local edit state for the account section, seeded once the profile loads.
        var displayName by remember(p.uid) { mutableStateOf(p.displayName) }
        var bio by remember(p.uid) { mutableStateOf(p.bio) }
        var pronouns by remember(p.uid) { mutableStateOf(p.pronouns) }
        var signature by remember(p.uid) { mutableStateOf(p.signature) }

        // Banked Status local state — seeded from profile.
        var bankedNote by remember(p.uid) { mutableStateOf(p.bankedStatus) }
        val nowMillis = System.currentTimeMillis()
        val bankedActive = p.bankedUntil > nowMillis

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DurenSpacing.space4, vertical = DurenSpacing.space2)
        ) {
            // ===== Appearance =====
            SectionHeader("Appearance")
            Text(
                "Accent color",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DurenSpacing.space2))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space3),
                verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
            ) {
                DurenAccent.all.forEach { option ->
                    val selected = option.hex.equals(p.accentColor, ignoreCase = true)
                    Swatch(
                        color = option.color,
                        selected = selected,
                        onClick = { viewModel.setAccent(option.hex) }
                    )
                }
            }

            Spacer(Modifier.height(DurenSpacing.space4))
            RowToggle(
                label = "Light mode",
                checked = p.lightModeEnabled,
                onCheckedChange = { viewModel.setLightMode(it) }
            )

            SectionDivider()

            // ===== Avatar =====
            SectionHeader("Avatar")
            Row(verticalAlignment = Alignment.CenterVertically) {
                DurenAvatar(
                    avatarUrl = p.avatarUrl,
                    fallbackColorHex = p.avatarColor,
                    size = 64.dp,
                    contentDescription = "Your avatar"
                )
                Spacer(Modifier.size(DurenSpacing.space4))
                OutlinedButton(
                    onClick = {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !avatarUploading
                ) {
                    Text(if (avatarUploading) "Updating…" else "Change photo")
                }
            }
            Spacer(Modifier.height(DurenSpacing.space4))
            Text(
                "Or pick a color",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DurenSpacing.space2))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space3),
                verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
            ) {
                DurenAvatarColors.all.forEach { option ->
                    val selected = option.hex.equals(p.avatarColor, ignoreCase = true)
                    Swatch(
                        color = option.color,
                        selected = selected,
                        onClick = { viewModel.setAvatarColor(option.hex) }
                    )
                }
            }

            SectionDivider()

            // ===== Privacy =====
            SectionHeader("Privacy")
            RowToggle("Show Lantern", p.showLantern) {
                viewModel.setPrivacy(it, p.showMoodCanvas, p.allowAnonBox, p.showTestimonials)
            }
            RowToggle("Show Mood Canvas", p.showMoodCanvas) {
                viewModel.setPrivacy(p.showLantern, it, p.allowAnonBox, p.showTestimonials)
            }
            RowToggle("Allow Anonymous Box", p.allowAnonBox) {
                viewModel.setPrivacy(p.showLantern, p.showMoodCanvas, it, p.showTestimonials)
            }
            RowToggle("Show Testimonials", p.showTestimonials) {
                viewModel.setPrivacy(p.showLantern, p.showMoodCanvas, p.allowAnonBox, it)
            }

            SectionDivider()

            // ===== Account =====
            SectionHeader("Account")
            IdentityRow("Username", "@${p.username}")
            IdentityRow("Email", p.email)
            Spacer(Modifier.height(DurenSpacing.space3))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DurenSpacing.space2))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 150) bio = it },
                label = { Text("Bio") },
                supportingText = { Text("${bio.length}/150") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DurenSpacing.space2))
            OutlinedTextField(
                value = pronouns,
                onValueChange = { pronouns = it },
                label = { Text("Pronouns") },
                placeholder = { Text("she/her · he/him · they/them") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DurenSpacing.space2))
            OutlinedTextField(
                value = signature,
                onValueChange = { if (it.length <= 30) signature = it },
                label = { Text("Ember signature") },
                placeholder = { Text("still up at 3am") },
                supportingText = { Text("${signature.length}/30 · shown under your name on every ember") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DurenSpacing.space3))
            Button(
                onClick = { viewModel.saveAccount(displayName, bio, pronouns, signature) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save account", fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(DurenSpacing.space2))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Change password")
            }

            SectionDivider()

            // ===== Banked Status =====
            SectionHeader("Banked Status")
            Text(
                text = "Go away for a while. Your Nest sees your note and when you're back.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DurenSpacing.space3))
            if (bankedActive) {
                Text(
                    text = "🌙 You're banked until ${formatBankedUntil(p.bankedUntil)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DurenColors.AccentTeal,
                    modifier = Modifier.padding(bottom = DurenSpacing.space2)
                )
            }
            OutlinedTextField(
                value = bankedNote,
                onValueChange = { if (it.length <= 80) bankedNote = it },
                label = { Text("Away note") },
                placeholder = { Text("back at 11PM · in a meeting · taking a break") },
                supportingText = { Text("${bankedNote.length}/80") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DurenSpacing.space3))
            Text(
                text = "Back in…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DurenSpacing.space2))
            Row(
                horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2),
                modifier = Modifier.fillMaxWidth()
            ) {
                val durations = listOf("1h" to 60L, "3h" to 180L, "6h" to 360L, "Tomorrow" to 1440L)
                durations.forEach { (label, minutes) ->
                    OutlinedButton(
                        onClick = {
                            val untilMillis = System.currentTimeMillis() + minutes * 60_000L
                            viewModel.setBankedStatus(bankedNote, untilMillis)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(DurenSpacing.space2))
            if (bankedActive || p.bankedStatus.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        bankedNote = ""
                        viewModel.setBankedStatus("", 0L)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear banked status")
                }
            }

            SectionDivider()

            // ===== About =====
            SectionHeader("About")
            IdentityRow("Version", appVersion)
            Spacer(Modifier.height(DurenSpacing.space2))
            Text(
                "Ephemeral. Present. Belong.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionDivider()

            OutlinedButton(
                onClick = {
                    viewModel.signOut()
                    onSignedOut()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Log out")
            }

            SectionDivider()

            SectionHeader("Danger zone")
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Delete my account", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(DurenSpacing.space6))
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Change password") },
                text = { Text("We'll email a reset link to ${p.email}. Open it to set a new password.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.sendPasswordReset(p.email)
                        showResetDialog = false
                    }) { Text("Send link") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (passwordResetSent) {
            AlertDialog(
                onDismissRequest = viewModel::acknowledgePasswordReset,
                title = { Text("Check your inbox") },
                text = { Text("If ${p.email} is on file, a reset link is on its way. It can take a minute to arrive.") },
                confirmButton = {
                    TextButton(onClick = viewModel::acknowledgePasswordReset) { Text("Got it") }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!deleting) {
                        showDeleteDialog = false
                        deletePassword = ""
                        viewModel.clearDeleteError()
                    }
                },
                title = { Text("Delete your account?") },
                text = {
                    Column {
                        Text(
                            "This permanently deletes your presence and frees @${p.username}. " +
                                "Embers you've shared fade on their own. This can't be undone."
                        )
                        Spacer(Modifier.height(DurenSpacing.space3))
                        OutlinedTextField(
                            value = deletePassword,
                            onValueChange = { deletePassword = it },
                            label = { Text("Confirm your password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        val err = deleteError
                        if (err != null) {
                            Spacer(Modifier.height(DurenSpacing.space2))
                            Text(
                                err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteAccount(deletePassword) },
                        enabled = deletePassword.isNotBlank() && !deleting
                    ) {
                        Text(
                            if (deleting) "Deleting…" else "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            deletePassword = ""
                            viewModel.clearDeleteError()
                        },
                        enabled = !deleting
                    ) { Text("Cancel") }
                }
            )
        }
    }
}

/**
 * Returns a human-friendly label for a bankedUntil timestamp — e.g. "3h 20m" or
 * "tomorrow" (if > 12 h away). Purely presentational; displayed in Settings.
 */
private fun formatBankedUntil(untilMillis: Long): String {
    val remainMs = untilMillis - System.currentTimeMillis()
    if (remainMs <= 0) return "now"
    val totalMins = (remainMs / 60_000).toInt()
    return when {
        totalMins >= 60 * 20 -> "tomorrow"
        totalMins >= 60 -> "${totalMins / 60}h ${totalMins % 60}m"
        else -> "${totalMins}m"
    }
}

@Composable
private fun IdentityRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DurenSpacing.space1),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = DurenSpacing.space2)
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(DurenSpacing.space4))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    Spacer(Modifier.height(DurenSpacing.space3))
}

@Composable
private fun RowToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DurenSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            DurenIcon(
                DurenIcon.Check,
                size = 18.dp,
                tint = Color(0xFF1A1A1A)
            )
        }
    }
}
