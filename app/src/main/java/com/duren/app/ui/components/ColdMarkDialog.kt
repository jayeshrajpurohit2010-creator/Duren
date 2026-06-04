package com.duren.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.duren.app.ui.theme.DurenSpacing

private val COLD_MARK_REASONS = listOf(
    "Spam",
    "Harassment",
    "Hate",
    "Off-tribe",
    "Other"
)

/**
 * A calm, non-punitive dialog for flagging an ember.
 * Selecting a reason immediately submits and dismisses — no extra confirmation step.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColdMarkDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cold Mark",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    text = "This quietly flags the ember for review. Choose the reason that fits best.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(DurenSpacing.space3))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DurenSpacing.space2),
                    verticalArrangement = Arrangement.spacedBy(DurenSpacing.space2)
                ) {
                    COLD_MARK_REASONS.forEach { reason ->
                        FilterChip(
                            selected = selected == reason,
                            onClick = {
                                selected = reason
                                onSubmit(reason)
                                onDismiss()
                            },
                            label = {
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
