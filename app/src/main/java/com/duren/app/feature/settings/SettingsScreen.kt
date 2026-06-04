package com.duren.app.feature.settings

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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.ui.theme.DurenAccent
import com.duren.app.ui.theme.DurenAvatarColors
import com.duren.app.ui.theme.DurenSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

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
            SectionHeader("Avatar color")
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
            Spacer(Modifier.height(DurenSpacing.space3))
            Button(
                onClick = { viewModel.saveAccount(displayName, bio, pronouns) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save account", fontWeight = FontWeight.Medium)
            }

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
            Spacer(Modifier.height(DurenSpacing.space6))
        }
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
            Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                tint = Color(0xFF1A1A1A)
            )
        }
    }
}
