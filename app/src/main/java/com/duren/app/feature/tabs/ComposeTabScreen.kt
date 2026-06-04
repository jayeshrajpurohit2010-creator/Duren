package com.duren.app.feature.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.duren.app.ui.theme.DurenShapes
import com.duren.app.ui.theme.DurenSpacing

/**
 * Compose tab placeholder. A real composer ships in Phase 1; for now it shows the
 * empty-state copy plus the locked lifespan label from PRD v2.1 §3.3:
 * posts fade in 48h (no picker), reaching 72h only if a post catches fire.
 */
@Composable
fun ComposeTabScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DurenSpacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "✨", fontSize = 56.sp)
        Text(
            text = "Share an ember.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = DurenSpacing.space4)
        )
        Text(
            text = "When you're ready.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DurenSpacing.space2)
        )

        Spacer(Modifier.height(DurenSpacing.space6))

        // Locked lifespan label — static, no picker (§3.3).
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
                .padding(horizontal = DurenSpacing.space4, vertical = DurenSpacing.space2)
        )
        Text(
            text = "Catches fire? Reaches 72h",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DurenSpacing.space2)
        )
    }
}
