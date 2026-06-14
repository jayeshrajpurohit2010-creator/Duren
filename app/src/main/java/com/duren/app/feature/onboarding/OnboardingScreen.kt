package com.duren.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.tribe.model.Tribe
import com.duren.app.ui.components.FloatingEmbers
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing
import com.duren.app.ui.theme.LocalDurenColors

/**
 * First-run "Find your fire" — the screen that keeps Duren from opening like a cold,
 * algorithmic feed. Before the Clearing, a new soul picks the campfires that feel
 * like theirs; their picks are joined and the flow marks itself done. Self-dismisses
 * via the host once [OnboardingViewModel.needsOnboarding] flips false.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val colors = LocalDurenColors.current
    val tribes by viewModel.tribes.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val finishing by viewModel.finishing.collectAsStateWithLifecycle()

    // Seed the catalog only once the flow actually shows — keeps the cost off every
    // launch for souls who are already past onboarding.
    LaunchedEffect(Unit) { viewModel.ensureCatalog() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.BackgroundPrimary)
    ) {
        // Warmth behind the choice — the same drifting sparks as the Clearing, but
        // dark-only so the near-white light theme stays clean.
        if (colors.isDark) {
            FloatingEmbers(modifier = Modifier.fillMaxSize(), count = 12)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header — title, a line of why, and a quiet escape hatch.
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { viewModel.skip() },
                    enabled = !finishing,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = DurenSpacing.space3, end = DurenSpacing.space2)
                ) {
                    Text("Skip", color = colors.TextMuted)
                }
                Column(
                    modifier = Modifier.padding(
                        start = DurenSpacing.space5,
                        end = DurenSpacing.space5,
                        top = DurenSpacing.space8,
                        bottom = DurenSpacing.space3
                    )
                ) {
                    Text(
                        text = "FIND YOUR FIRE",
                        color = colors.AccentTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(DurenSpacing.space2))
                    Text(
                        text = "Pick the fires that feel like yours.",
                        color = colors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Spacer(Modifier.height(DurenSpacing.space2))
                    Text(
                        text = "These are where your embers land and your people gather. " +
                            "Join a few — you can always find more later.",
                        color = colors.TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = DurenSpacing.space4,
                    end = DurenSpacing.space4,
                    top = DurenSpacing.space2,
                    bottom = DurenSpacing.space4
                ),
                verticalArrangement = Arrangement.spacedBy(DurenSpacing.space3)
            ) {
                items(tribes, key = { it.id }) { tribe ->
                    TribePickCard(
                        tribe = tribe,
                        selected = tribe.id in selected,
                        onToggle = { viewModel.toggle(tribe.id) }
                    )
                }
            }

            // Footer CTA — joins the picks, or skips when nothing's chosen.
            val n = selected.size
            Button(
                onClick = { if (n == 0) viewModel.skip() else viewModel.enter() },
                enabled = !finishing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.AccentTeal,
                    contentColor = colors.OnAccent,
                    disabledContainerColor = colors.SurfaceElevated,
                    disabledContentColor = colors.TextDisabled
                ),
                shape = DurenShapes.pill,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DurenSpacing.space4,
                        end = DurenSpacing.space4,
                        bottom = DurenSpacing.space6,
                        top = DurenSpacing.space2
                    )
                    .height(54.dp)
            ) {
                Text(
                    text = when {
                        finishing -> "Lighting your fires…"
                        n == 0 -> "Skip for now"
                        n == 1 -> "Enter with 1 fire 🔥"
                        else -> "Enter with $n fires 🔥"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/** One pickable tribe — emoji, name, its campfire line, and a teal ring when chosen. */
@Composable
private fun TribePickCard(
    tribe: Tribe,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalDurenColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DurenShapes.medium)
            .background(if (selected) colors.AccentTeal.copy(alpha = 0.10f) else colors.SurfaceElevated)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.AccentTeal else colors.BorderDefault,
                shape = DurenShapes.medium
            )
            .clickable { onToggle() }
            .padding(DurenSpacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.BackgroundSecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = tribe.emoji.ifBlank { "🔥" }, fontSize = 22.sp)
        }
        Spacer(Modifier.size(DurenSpacing.space3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tribe.name,
                color = colors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (tribe.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tribe.description,
                    color = colors.TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(Modifier.size(DurenSpacing.space2))
        // A teal check when chosen; a hollow ring when not — no extra icon font needed.
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) colors.AccentTeal else colors.BackgroundSecondary)
                .border(
                    width = 1.dp,
                    color = if (selected) colors.AccentTeal else colors.BorderDefault,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text(text = "✓", color = colors.OnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
