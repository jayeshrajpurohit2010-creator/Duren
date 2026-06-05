package com.duren.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.theme.DurenAvatarColors
import com.duren.app.ui.theme.DurenSpacing

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val myEmbers by viewModel.myEmbers.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(DurenSpacing.space6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val p = profile
            if (p == null) {
                ShimmerBox(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.height(DurenSpacing.space4))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(20.dp))
                Spacer(Modifier.height(DurenSpacing.space2))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(DurenAvatarColors.colorForHex(p.avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = p.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(Modifier.height(DurenSpacing.space4))
                Text(
                    text = p.displayName.ifBlank { p.username },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "@${p.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (p.bio.isNotBlank()) {
                    Spacer(Modifier.height(DurenSpacing.space2))
                    Text(
                        text = p.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(DurenSpacing.space2))
                Text(
                    text = p.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(DurenSpacing.space8))
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Spacer(Modifier.size(DurenSpacing.space2))
                    Text("Settings", fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(DurenSpacing.space3))
                OutlinedButton(
                    onClick = {
                        viewModel.signOut()
                        onSignedOut()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Sign out")
                }

                // --- Your embers section ---
                Spacer(Modifier.height(DurenSpacing.space8))
                HorizontalDivider()
                Spacer(Modifier.height(DurenSpacing.space4))
                Text(
                    text = "Your embers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start)
                )
                Spacer(Modifier.height(DurenSpacing.space3))
                if (myEmbers.isEmpty()) {
                    Text(
                        text = "You haven't shared an ember yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    myEmbers.forEach { ember ->
                        EmberCard(
                            ember = ember,
                            onEcho = {},
                            onColdMark = {}
                        )
                        Spacer(Modifier.height(DurenSpacing.space3))
                    }
                }
            }
        }
    }
}
