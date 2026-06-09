package com.duren.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.mood.model.Mood
import com.duren.app.data.profile.model.Profile
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
    val myMood by viewModel.myMood.collectAsStateWithLifecycle()

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
                        // Presence aura — a soft ring around the avatar. Tinted by
                        // tonight's Mood Canvas when shown, otherwise teal (F12).
                        val auraColor = remember(myMood, p.showMoodCanvas) {
                            val m = myMood
                            if (p.showMoodCanvas && m != null && m.isSet)
                                Color(android.graphics.Color.parseColor(Mood.hexFor(m.mood)))
                            else DurenColors.AccentTeal
                        }
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .clip(CircleShape)
                                .border(2.dp, auraColor.copy(alpha = 0.55f), CircleShape),
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

                        Spacer(Modifier.height(DurenSpacing.space6))
                        PresenceControls(
                            profile = p,
                            mood = myMood,
                            onSetMood = viewModel::setMood,
                            onSetBanked = viewModel::setBanked,
                            onClearBanked = viewModel::clearBanked
                        )

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

/**
 * Tonight's presence: a Mood Canvas picker (F12) and a Banked away-note (F11). Both
 * are quiet and opt-in, and live only on your own profile.
 */
@Composable
private fun PresenceControls(
    profile: Profile,
    mood: Mood?,
    onSetMood: (Int) -> Unit,
    onSetBanked: (String) -> Unit,
    onClearBanked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mood Canvas — five dots, tonight's choice ringed.
        Text(text = "How's tonight?", fontSize = 12.sp, color = DurenColors.TextMuted)
        Spacer(Modifier.height(DurenSpacing.space2))
        Row(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space3)) {
            (1..5).forEach { m ->
                val selected = mood?.mood == m
                val swatch = Color(android.graphics.Color.parseColor(Mood.hexFor(m)))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) Modifier.border(2.dp, DurenColors.TextPrimary, CircleShape)
                            else Modifier
                        )
                        .clickable { onSetMood(m) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(swatch)
                    )
                }
            }
        }
        if (mood?.isSet == true) {
            Spacer(Modifier.height(DurenSpacing.space1))
            Text(text = Mood.labelFor(mood.mood), fontSize = 11.sp, color = DurenColors.TextSecondary)
        }

        Spacer(Modifier.height(DurenSpacing.space5))

        // Banked Status — an away note that clears itself.
        if (profile.isBanked) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💤 ${profile.bankedStatus}",
                    fontSize = 13.sp,
                    color = DurenColors.TextSecondary
                )
                TextButton(onClick = onClearBanked) {
                    Text("I'm back", color = DurenColors.AccentTeal)
                }
            }
        } else {
            var editing by remember { mutableStateOf(false) }
            var note by remember { mutableStateOf("") }
            if (editing) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 150) note = it },
                    placeholder = { Text("Banked. Back at 11PM…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(DurenSpacing.space2))
                Row(horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2)) {
                    TextButton(onClick = { editing = false }) {
                        Text("Cancel", color = DurenColors.TextMuted)
                    }
                    TextButton(onClick = {
                        if (note.isNotBlank()) {
                            onSetBanked(note)
                            editing = false
                        }
                    }) {
                        Text("Set", color = DurenColors.AccentTeal)
                    }
                }
            } else {
                TextButton(onClick = { editing = true }) {
                    Text("Set an away note", color = DurenColors.TextMuted)
                }
            }
        }
    }
}
