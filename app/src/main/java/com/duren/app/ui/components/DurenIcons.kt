package com.duren.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Duren's own hand-drawn icon set, ported verbatim from the design export
 * (`primitives.jsx` → `Icon.*`). The brand deliberately does NOT use Material icons;
 * these are warm, slightly-organic line glyphs that match the Ember Mark.
 *
 * Each icon is the exact SVG path data from the export, parsed at runtime and drawn
 * in a 24×24 space scaled to fit — so they stay crisp at any size, carry no drawable
 * assets (keeps the APK lean — see [[duren-material-icons-core-only]]), and tint to
 * any colour via [tint] (the export's `currentColor`).
 */
enum class DurenIcon {
    Ember, EmberFilled, Reply, Whisper, More, Mask, Photo, Mic, Video, Plus,
    Lock, Check, Chevron, Bell, Search, Send, Tribe, Nest, Presence, Lantern,
    Settings, Crown
}

@Composable
fun DurenIcon(
    icon: DurenIcon,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current
) {
    val shapes = remember(icon) { shapesFor(icon) }
    Canvas(modifier = modifier.size(size)) {
        val factor = this.size.minDimension / 24f
        scale(factor, factor, pivot = Offset.Zero) {
            shapes.forEach { it.draw(this, tint) }
        }
    }
}

// ---- Shape model (a tiny subset of SVG: path / circle / rect / ellipse) ----

private sealed interface IconShape {
    fun draw(scope: DrawScope, color: Color)
}

/** An SVG <path d="…">. */
private class P(
    private val d: String,
    private val fill: Boolean = false,
    private val sw: Float = 1.6f,
    private val cap: StrokeCap = StrokeCap.Butt,
    private val join: StrokeJoin = StrokeJoin.Round
) : IconShape {
    private val path = PathParser().parsePathString(d).toPath()
    override fun draw(scope: DrawScope, color: Color) {
        if (fill) scope.drawPath(path, color, style = Fill)
        else scope.drawPath(path, color, style = Stroke(width = sw, cap = cap, join = join))
    }
}

/** An SVG <circle>. */
private class C(
    private val cx: Float, private val cy: Float, private val r: Float,
    private val fill: Boolean = false, private val sw: Float = 1.5f
) : IconShape {
    override fun draw(scope: DrawScope, color: Color) {
        if (fill) scope.drawCircle(color, radius = r, center = Offset(cx, cy))
        else scope.drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(width = sw))
    }
}

/** An SVG <rect rx>. */
private class R(
    private val x: Float, private val y: Float, private val w: Float, private val h: Float,
    private val rx: Float = 0f, private val sw: Float = 1.6f
) : IconShape {
    override fun draw(scope: DrawScope, color: Color) {
        scope.drawRoundRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(w, h),
            cornerRadius = CornerRadius(rx, rx),
            style = Stroke(width = sw)
        )
    }
}

/** A filled SVG <ellipse> (only ever filled in this set). */
private class E(
    private val cx: Float, private val cy: Float, private val rx: Float, private val ry: Float
) : IconShape {
    override fun draw(scope: DrawScope, color: Color) {
        scope.drawOval(color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))
    }
}

private val EMBER_D = "M12 3c1 4 5 5 5 10a5 5 0 0 1-10 0c0-3 2-5 3-7 1 1 1 2 2 2-.3-2 .5-3 0-5z"

private fun shapesFor(icon: DurenIcon): List<IconShape> = when (icon) {
    DurenIcon.Ember -> listOf(P(EMBER_D, sw = 1.6f))
    DurenIcon.EmberFilled -> listOf(P(EMBER_D, fill = true))
    DurenIcon.Reply -> listOf(P("M21 11c0-4-3-7-7-7H8L3 10l5 6h6c4 0 7-3 7-5z", sw = 1.6f))
    DurenIcon.Whisper -> listOf(
        P("M4 12c0-4 4-7 8-7s8 3 8 7-4 7-8 7c-1 0-2-.2-3-.5L4 20l1.5-3C4.5 16 4 14 4 12z", sw = 1.5f),
        C(9f, 12f, 1f, fill = true), C(13f, 12f, 1f, fill = true)
    )
    DurenIcon.More -> listOf(
        C(5f, 12f, 1.6f, fill = true), C(12f, 12f, 1.6f, fill = true), C(19f, 12f, 1.6f, fill = true)
    )
    DurenIcon.Mask -> listOf(
        P("M4 10c0-2 2-3 4-3h8c2 0 4 1 4 3v3c0 3-3 4-5 4-1 0-2-.5-3-1-1 .5-2 1-3 1-2 0-5-1-5-4v-3z", sw = 1.5f),
        E(8.5f, 11f, 1.4f, 1.8f), E(15.5f, 11f, 1.4f, 1.8f)
    )
    DurenIcon.Photo -> listOf(
        R(3f, 5f, 18f, 14f, rx = 2f, sw = 1.5f),
        C(9f, 11f, 1.5f, fill = true),
        P("M21 16l-5-4-8 7", sw = 1.5f)
    )
    DurenIcon.Mic -> listOf(
        R(9f, 3f, 6f, 11f, rx = 3f, sw = 1.6f),
        P("M5 11a7 7 0 0 0 14 0M12 18v3", sw = 1.6f, cap = StrokeCap.Round)
    )
    DurenIcon.Video -> listOf(
        R(2f, 6f, 14f, 12f, rx = 2f, sw = 1.6f),
        P("M16 10l6-3v10l-6-3z", sw = 1.6f)
    )
    DurenIcon.Plus -> listOf(P("M12 5v14M5 12h14", sw = 2f, cap = StrokeCap.Round))
    DurenIcon.Lock -> listOf(
        R(5f, 11f, 14f, 9f, rx = 2f, sw = 1.6f),
        P("M8 11V8a4 4 0 0 1 8 0v3", sw = 1.6f)
    )
    DurenIcon.Check -> listOf(P("M5 12l5 5L19 7", sw = 2f, cap = StrokeCap.Round))
    DurenIcon.Chevron -> listOf(P("M9 6l6 6-6 6", sw = 1.8f, cap = StrokeCap.Round))
    DurenIcon.Bell -> listOf(P("M6 16V11a6 6 0 1 1 12 0v5l2 2H4l2-2zM10 20a2 2 0 0 0 4 0", sw = 1.5f))
    DurenIcon.Search -> listOf(
        C(11f, 11f, 7f, sw = 1.6f),
        P("M16 16l4 4", sw = 1.6f, cap = StrokeCap.Round)
    )
    DurenIcon.Send -> listOf(P("M3 11l18-7-7 18-2-7-9-4z", sw = 1.6f))
    DurenIcon.Tribe -> listOf(
        P("M4 19l8-13 8 13H4z", sw = 1.6f),
        P("M9 19l3-5 3 5", sw = 1.4f)
    )
    DurenIcon.Nest -> listOf(
        P("M3 16c2-4 7-6 9-6s7 2 9 6", sw = 1.6f, cap = StrokeCap.Round),
        C(9f, 11f, 2f, sw = 1.4f), C(14f, 9f, 1.5f, sw = 1.4f), C(17f, 12f, 1.5f, sw = 1.4f)
    )
    DurenIcon.Presence -> listOf(
        C(12f, 8f, 4f, sw = 1.6f),
        P("M4 20c1-4 4-6 8-6s7 2 8 6", sw = 1.6f, cap = StrokeCap.Round)
    )
    DurenIcon.Lantern -> listOf(
        P("M9 3h6M8 5h8v3c0 3-2 5-4 5s-4-2-4-5V5zM12 13v4M9 21h6", sw = 1.5f, cap = StrokeCap.Round)
    )
    DurenIcon.Settings -> listOf(
        C(12f, 12f, 3f, sw = 1.6f),
        P(
            "M19 12c0 .5-.1 1-.2 1.5l2 1.5-2 3.5-2.3-1c-.7.6-1.5 1-2.4 1.3L14 21h-4l-.1-1.2c-.9-.3-1.7-.7-2.4-1.3l-2.3 1-2-3.5 2-1.5C5.1 13 5 12.5 5 12s.1-1 .2-1.5l-2-1.5 2-3.5 2.3 1c.7-.6 1.5-1 2.4-1.3L10 3h4l.1 1.2c.9.3 1.7.7 2.4 1.3l2.3-1 2 3.5-2 1.5c.1.5.2 1 .2 1.5z",
            sw = 1.4f
        )
    )
    DurenIcon.Crown -> listOf(P("M3 8l4 4 5-6 5 6 4-4-1 11H4L3 8z", sw = 1.4f))
}
