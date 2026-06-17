package com.duren.app.feature.whisper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duren.app.data.ember.model.Whisper
import com.duren.app.ui.components.DurenAvatar
import com.duren.app.ui.components.DurenIcon
import com.duren.app.ui.theme.DurenSpacing

/**
 * Inline whisper (comment) thread shown under an expanded ember.
 *
 * Each card gets its own [WhisperViewModel] keyed by [emberId], so streams only
 * run while a thread is open. Whispers nest: tap "Reply" on any whisper to thread
 * a reply beneath it (the tree is rebuilt on-device from each whisper's parent
 * link). Long-press one of your own to delete it.
 *
 * When [forceAnonymous] every whisper here is posted faceless no matter what —
 * confessions keep their room safe. The repository enforces the same rule, so this
 * is just the honest UI for it.
 *
 * [emberAuthorId] marks the soul who lit the ember with a small "OP" badge when they
 * whisper back. Pass it **only** when that author is already public (a Named ember) —
 * leave it null for anonymous or confess embers, or the badge would quietly reveal
 * who posted them. An anonymous whisper never gets the badge either, for the same reason.
 */
@Composable
fun WhisperThread(
    emberId: String,
    modifier: Modifier = Modifier,
    forceAnonymous: Boolean = false,
    emberAuthorId: String? = null,
    viewModel: WhisperViewModel = hiltViewModel(key = "whisper_$emberId")
) {
    viewModel.bind(emberId)
    val whispers by viewModel.whispers.collectAsStateWithLifecycle()
    val myUid = viewModel.currentUserId

    var input by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<Whisper?>(null) }

    // Rebuild the reply tree on-device. Roots are whispers with no parent (or whose
    // parent has already burned away); everything else hangs under its parent id.
    val byParent = remember(whispers) { whispers.groupBy { it.parentWhisperId } }
    val presentIds = remember(whispers) { whispers.mapTo(HashSet()) { it.id } }
    val roots = remember(whispers) {
        whispers.filter { it.parentWhisperId == null || it.parentWhisperId !in presentIds }
    }

    Column(modifier = modifier) {
        if (whispers.isEmpty()) {
            Text(
                text = "No whispers yet. Say something.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = DurenSpacing.space2)
            )
        } else {
            roots.forEach { root ->
                WhisperNode(
                    whisper = root,
                    childrenOf = byParent,
                    depth = 0,
                    myUid = myUid,
                    emberAuthorId = emberAuthorId,
                    onReply = { replyTarget = it },
                    onDelete = { viewModel.delete(it) }
                )
            }
        }

        Spacer(Modifier.height(DurenSpacing.space2))

        // Reply context — shows who you're answering, with a way to back out.
        replyTarget?.let { target ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DurenSpacing.space1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Replying to ${displayNameOf(target)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DurenIcon(
                    icon = DurenIcon.Close,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { replyTarget = null }
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 500) input = it },
            placeholder = { Text(if (replyTarget != null) "Add a reply…" else "Add a whisper…") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (forceAnonymous) {
                // Locked — a confession keeps every whisperer faceless.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DurenIcon(
                        icon = DurenIcon.Mask,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(DurenSpacing.space1))
                    Text(
                        text = "Whispers here stay anonymous",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                FilterChip(
                    selected = anonymous,
                    onClick = { anonymous = !anonymous },
                    label = { Text("Anonymous") }
                )
            }
            TextButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        viewModel.add(text, anonymous || forceAnonymous, replyTarget?.id)
                        input = ""
                        replyTarget = null
                    }
                },
                enabled = input.isNotBlank()
            ) {
                Text(if (replyTarget != null) "Reply" else "Send")
            }
        }
    }
}

/**
 * One whisper plus its replies, drawn recursively. Nested replies sit behind a
 * faint thread line so the conversation reads as a tree, not a flat pile.
 */
@Composable
private fun WhisperNode(
    whisper: Whisper,
    childrenOf: Map<String?, List<Whisper>>,
    depth: Int,
    myUid: String?,
    emberAuthorId: String?,
    onReply: (Whisper) -> Unit,
    onDelete: (String) -> Unit
) {
    val children = childrenOf[whisper.id].orEmpty()
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        if (depth > 0) {
            Box(
                modifier = Modifier
                    .padding(start = DurenSpacing.space1, end = DurenSpacing.space2)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            WhisperRow(
                whisper = whisper,
                isMine = whisper.authorId == myUid,
                // OP only when the ember's author is publicly known and this whisper
                // isn't faceless — never let the badge out a hidden poster.
                isOp = emberAuthorId != null &&
                    whisper.authorId == emberAuthorId &&
                    !whisper.isAnonymous,
                onReply = { onReply(whisper) },
                onDelete = { onDelete(whisper.id) }
            )
            children.forEach { child ->
                WhisperNode(
                    whisper = child,
                    childrenOf = childrenOf,
                    depth = depth + 1,
                    myUid = myUid,
                    emberAuthorId = emberAuthorId,
                    onReply = onReply,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun WhisperRow(
    whisper: Whisper,
    isMine: Boolean,
    isOp: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    val name = displayNameOf(whisper)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DurenSpacing.space2)
            .pointerInput(isMine) {
                if (isMine) detectTapGestures(onLongPress = { onDelete() })
            },
        verticalAlignment = Alignment.Top
    ) {
        if (whisper.isAnonymous) {
            // No avatar for anonymous souls — keep them faceless.
            Spacer(Modifier.width(4.dp))
        } else {
            DurenAvatar(
                avatarUrl = whisper.authorAvatarUrl,
                fallbackColorHex = whisper.authorAvatarColor,
                size = 28.dp,
                contentDescription = "Avatar for $name"
            )
        }
        Spacer(Modifier.width(DurenSpacing.space2))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isOp) {
                    Spacer(Modifier.width(DurenSpacing.space2))
                    OpBadge()
                }
            }
            Text(
                text = whisper.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(DurenSpacing.space1))
            Text(
                text = "Reply",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onReply() }
            )
        }
    }
}

/** The "OP" tag — the soul who lit the ember, replying in their own thread. */
@Composable
private fun OpBadge() {
    Text(
        text = "OP",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

/** How a whisper signs itself — "A Soul" when anonymous, else a name/handle. */
private fun displayNameOf(whisper: Whisper): String = when {
    whisper.isAnonymous -> "A Soul"
    whisper.authorName.isNotBlank() -> whisper.authorName
    whisper.authorUsername.isNotBlank() -> "@${whisper.authorUsername}"
    else -> "Someone"
}
