package com.duren.app.feature.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.ui.animation.ShimmerBox
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.components.EmberCard
import com.duren.app.ui.theme.DurenColors
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

/**
 * My Presence — a profile that reads like a campfire seen from a distance, not an
 * Instagram grid. Darkness canvas, a soft teal aura around the avatar, the signature
 * sitting just under the name, and the user's own embers below with no card framing.
 */
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNest: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val myEmbers by viewModel.myEmbers.collectAsStateWithLifecycle()

    Scaffold(containerColor = DurenColors.BackgroundPrimary) { padding ->
        val p = profile
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (p == null) {
                item {
                    Spacer(Modifier.height(DurenSpacing.space8))
                    ShimmerBox(modifier = Modifier.size(96.dp).clip(CircleShape))
                    Spacer(Modifier.height(DurenSpacing.space4))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(20.dp))
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DurenSpacing.space6),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(DurenSpacing.space8))
                        // Presence aura — a soft teal ring around the avatar.
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .clip(CircleShape)
                                .border(2.dp, DurenColors.AccentTeal.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            DurenAvatar(
                                avatarUrl = p.avatarUrl,
                                fallbackColorHex = p.avatarColor,
                                size = 92.dp,
                                contentDescription = "Avatar"
                            )
                        }
                        Spacer(Modifier.height(DurenSpacing.space4))
                        Text(
                            text = p.displayName.ifBlank { p.username },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DurenColors.TextPrimary
                        )
                        Text(
                            text = "@${p.username}",
                            fontSize = 14.sp,
                            color = DurenColors.TextSecondary
                        )
                        if (p.pronouns.isNotBlank()) {
                            Text(text = p.pronouns, fontSize = 12.sp, color = DurenColors.TextMuted)
                        }
                        // Ember signature — the tagline that rides under your name everywhere.
                        if (p.signature.isNotBlank()) {
                            Spacer(Modifier.height(DurenSpacing.space2))
                            Text(
                                text = p.signature,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                color = DurenColors.AccentTeal,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (p.bio.isNotBlank()) {
                            Spacer(Modifier.height(DurenSpacing.space3))
                            Text(
                                text = p.bio,
                                fontSize = 14.sp,
                                color = DurenColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(DurenSpacing.space8))
                        Button(
                            onClick = onOpenNest,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DurenColors.AccentTeal,
                                contentColor = DurenColors.OnAccent
                            ),
                            shape = DurenShapes.pill,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(DurenSpacing.space2))
                            Text("Your Nest", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(DurenSpacing.space3))
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = DurenShapes.pill,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            DurenIcon(DurenIcon.Settings, size = 20.dp)
                            Spacer(Modifier.size(DurenSpacing.space2))
                            Text("Settings", fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(DurenSpacing.space3))
                        OutlinedButton(
                            onClick = {
                                viewModel.signOut()
                                onSignedOut()
                            },
                            shape = DurenShapes.pill,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Sign out", color = DurenColors.TextSecondary)
                        }
                    }
                }

                // Section label — quiet, tracked, no divider or card behind it.
                item {
                    Spacer(Modifier.height(DurenSpacing.space8))
                    Text(
                        text = "YOUR EMBERS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp,
                        color = DurenColors.TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DurenSpacing.space6)
                    )
                    Spacer(Modifier.height(DurenSpacing.space5))
                }

                if (myEmbers.isEmpty()) {
                    item {
                        Text(
                            text = "You haven't lit an ember yet.",
                            fontSize = 14.sp,
                            color = DurenColors.TextMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DurenSpacing.space6)
                        )
                    }
                } else {
                    items(myEmbers, key = { it.id }) { ember ->
                        EmberCard(
                            ember = ember,
                            onEcho = {},
                            onColdMark = {},
                            interactive = false,
                            canDelete = true,
                            onDelete = { viewModel.deleteEmber(ember.id) }
                        )
                        Spacer(Modifier.height(DurenSpacing.space6))
                    }
                }
            }
        }
    }
}
