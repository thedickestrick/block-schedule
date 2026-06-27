package com.blockschedule.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hand-drawn kawaii animal illustrations (Compose Canvas) used instead of plain emoji for the
 * "today's crew" and the streak buddy. Crisp at any size, themeable, and far cuter than a glyph.
 * Anything without art falls back to its emoji.
 */

private val EYE = Color(0xFF3A3A3A)
private val BLUSH = Color(0xFFFF9AA2)
private val WHITE = Color(0xFFFFFFFF)

// --- tiny fraction-based drawing helpers (coords are 0..1 of the canvas) ---

private fun DrawScope.circle(cx: Float, cy: Float, r: Float, color: Color) =
    drawCircle(color, r * size.width, Offset(cx * size.width, cy * size.height))

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) =
    drawOval(color, Offset((cx - rx) * size.width, (cy - ry) * size.height), Size(2 * rx * size.width, 2 * ry * size.height))

private fun DrawScope.tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, color: Color) {
    val p = Path().apply {
        moveTo(ax * size.width, ay * size.height)
        lineTo(bx * size.width, by * size.height)
        lineTo(cx * size.width, cy * size.height)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.smile(cx: Float, cy: Float, r: Float, color: Color, weight: Float = 0.035f) {
    drawArc(
        color, 20f, 140f, false,
        topLeft = Offset((cx - r) * size.width, (cy - r) * size.height),
        size = Size(2 * r * size.width, 2 * r * size.height),
        style = Stroke(weight * size.width)
    )
}

private fun DrawScope.eyes(lx: Float, rx: Float, y: Float, r: Float = 0.052f) {
    circle(lx, y, r, EYE); circle(rx, y, r, EYE)
    circle(lx + 0.012f, y - 0.018f, r * 0.34f, WHITE)
    circle(rx + 0.012f, y - 0.018f, r * 0.34f, WHITE)
}

private fun DrawScope.cheeks(lx: Float, rx: Float, y: Float, r: Float = 0.05f) {
    circle(lx, y, r, BLUSH.copy(alpha = 0.7f)); circle(rx, y, r, BLUSH.copy(alpha = 0.7f))
}

// --- the animals ---

private fun DrawScope.cat() {
    val c = Color(0xFFF6C177)
    tri(0.26f, 0.30f, 0.30f, 0.05f, 0.46f, 0.22f, c)
    tri(0.74f, 0.30f, 0.70f, 0.05f, 0.54f, 0.22f, c)
    tri(0.30f, 0.27f, 0.33f, 0.10f, 0.43f, 0.22f, BLUSH)
    tri(0.70f, 0.27f, 0.67f, 0.10f, 0.57f, 0.22f, BLUSH)
    circle(0.5f, 0.55f, 0.34f, c)
    eyes(0.38f, 0.62f, 0.54f); cheeks(0.3f, 0.7f, 0.64f)
    tri(0.47f, 0.62f, 0.53f, 0.62f, 0.5f, 0.67f, BLUSH)
    smile(0.5f, 0.66f, 0.06f, EYE)
}

private fun DrawScope.dog() {
    val c = Color(0xFFCB9A66); val ear = Color(0xFF9E7448)
    oval(0.22f, 0.45f, 0.12f, 0.2f, ear); oval(0.78f, 0.45f, 0.12f, 0.2f, ear)
    circle(0.5f, 0.55f, 0.34f, c)
    oval(0.5f, 0.6f, 0.2f, 0.16f, Color(0xFFEAD3B5))
    eyes(0.39f, 0.61f, 0.5f); cheeks(0.3f, 0.7f, 0.62f)
    circle(0.5f, 0.6f, 0.055f, EYE)
    smile(0.5f, 0.62f, 0.06f, EYE)
}

private fun DrawScope.fox() {
    val c = Color(0xFFF0833C); val w = Color(0xFFFDF1E5)
    tri(0.24f, 0.34f, 0.26f, 0.04f, 0.48f, 0.26f, c)
    tri(0.76f, 0.34f, 0.74f, 0.04f, 0.52f, 0.26f, c)
    circle(0.5f, 0.55f, 0.34f, c)
    tri(0.3f, 0.55f, 0.7f, 0.55f, 0.5f, 0.86f, w)
    eyes(0.38f, 0.62f, 0.52f); cheeks(0.29f, 0.71f, 0.64f)
    circle(0.5f, 0.66f, 0.045f, EYE)
}

private fun DrawScope.bunny() {
    val c = Color(0xFFF3E9DC)
    oval(0.38f, 0.22f, 0.07f, 0.2f, c); oval(0.62f, 0.22f, 0.07f, 0.2f, c)
    oval(0.38f, 0.24f, 0.035f, 0.13f, BLUSH); oval(0.62f, 0.24f, 0.035f, 0.13f, BLUSH)
    circle(0.5f, 0.58f, 0.32f, c)
    eyes(0.39f, 0.61f, 0.56f); cheeks(0.3f, 0.7f, 0.66f)
    tri(0.47f, 0.64f, 0.53f, 0.64f, 0.5f, 0.69f, BLUSH)
    smile(0.5f, 0.68f, 0.05f, EYE)
}

private fun DrawScope.bear() {
    val c = Color(0xFFB07A52)
    circle(0.28f, 0.28f, 0.13f, c); circle(0.72f, 0.28f, 0.13f, c)
    circle(0.28f, 0.28f, 0.06f, BLUSH); circle(0.72f, 0.28f, 0.06f, BLUSH)
    circle(0.5f, 0.56f, 0.34f, c)
    oval(0.5f, 0.62f, 0.17f, 0.13f, Color(0xFFE3C7A6))
    eyes(0.39f, 0.61f, 0.5f); cheeks(0.3f, 0.7f, 0.6f)
    circle(0.5f, 0.6f, 0.05f, EYE); smile(0.5f, 0.62f, 0.055f, EYE)
}

private fun DrawScope.panda() {
    val c = WHITE; val b = Color(0xFF3A3A3A)
    circle(0.28f, 0.26f, 0.12f, b); circle(0.72f, 0.26f, 0.12f, b)
    circle(0.5f, 0.56f, 0.34f, c)
    oval(0.38f, 0.54f, 0.09f, 0.11f, b); oval(0.62f, 0.54f, 0.09f, 0.11f, b)
    circle(0.38f, 0.55f, 0.045f, WHITE); circle(0.62f, 0.55f, 0.045f, WHITE)
    circle(0.39f, 0.56f, 0.025f, b); circle(0.61f, 0.56f, 0.025f, b)
    circle(0.5f, 0.66f, 0.04f, b); smile(0.5f, 0.68f, 0.05f, b)
}

private fun DrawScope.pig() {
    val c = Color(0xFFF4A0BE)
    tri(0.3f, 0.34f, 0.34f, 0.16f, 0.46f, 0.32f, c)
    tri(0.7f, 0.34f, 0.66f, 0.16f, 0.54f, 0.32f, c)
    circle(0.5f, 0.56f, 0.34f, c)
    oval(0.5f, 0.62f, 0.13f, 0.1f, Color(0xFFE87FA6))
    circle(0.46f, 0.62f, 0.022f, Color(0xFFB85C80)); circle(0.54f, 0.62f, 0.022f, Color(0xFFB85C80))
    eyes(0.39f, 0.61f, 0.48f); cheeks(0.3f, 0.7f, 0.56f)
}

private fun DrawScope.frog() {
    val c = Color(0xFF86C543)
    circle(0.34f, 0.32f, 0.13f, c); circle(0.66f, 0.32f, 0.13f, c)
    circle(0.34f, 0.32f, 0.07f, WHITE); circle(0.66f, 0.32f, 0.07f, WHITE)
    circle(0.34f, 0.33f, 0.035f, EYE); circle(0.66f, 0.33f, 0.035f, EYE)
    circle(0.5f, 0.6f, 0.32f, c)
    cheeks(0.3f, 0.7f, 0.64f)
    smile(0.5f, 0.6f, 0.13f, EYE, 0.03f)
}

private fun DrawScope.chick() {
    val c = Color(0xFFFFD93B); val beak = Color(0xFFF5A623)
    circle(0.5f, 0.55f, 0.34f, c)
    circle(0.2f, 0.6f, 0.1f, c); circle(0.8f, 0.6f, 0.1f, c) // little wings
    eyes(0.4f, 0.6f, 0.5f); cheeks(0.3f, 0.7f, 0.6f)
    tri(0.45f, 0.6f, 0.55f, 0.6f, 0.5f, 0.68f, beak)
    tri(0.46f, 0.22f, 0.54f, 0.22f, 0.5f, 0.12f, c) // tuft
}

private fun DrawScope.penguin() {
    val b = Color(0xFF3A3A3A); val beak = Color(0xFFF5A623)
    circle(0.5f, 0.52f, 0.36f, b)
    oval(0.5f, 0.6f, 0.22f, 0.26f, WHITE)
    eyes(0.41f, 0.59f, 0.46f); cheeks(0.32f, 0.68f, 0.56f)
    tri(0.45f, 0.56f, 0.55f, 0.56f, 0.5f, 0.64f, beak)
}

private fun DrawScope.cow() {
    val c = WHITE; val b = Color(0xFF3A3A3A); val pink = Color(0xFFF6B7C8)
    oval(0.3f, 0.2f, 0.05f, 0.04f, Color(0xFFEAD9A8)); oval(0.7f, 0.2f, 0.05f, 0.04f, Color(0xFFEAD9A8))
    circle(0.24f, 0.4f, 0.1f, b); circle(0.76f, 0.4f, 0.1f, b)
    circle(0.5f, 0.55f, 0.34f, c)
    circle(0.34f, 0.42f, 0.08f, b)
    oval(0.5f, 0.66f, 0.16f, 0.12f, pink)
    circle(0.46f, 0.66f, 0.02f, Color(0xFFCE8195)); circle(0.54f, 0.66f, 0.02f, Color(0xFFCE8195))
    eyes(0.4f, 0.62f, 0.5f)
}

private fun DrawScope.egg() {
    val c = Color(0xFFFFF3D6); val shade = Color(0xFFEAD9A8)
    oval(0.5f, 0.55f, 0.3f, 0.38f, c)
    oval(0.4f, 0.5f, 0.08f, 0.12f, c.copy(alpha = 0.0f)) // (kept for spacing)
    oval(0.6f, 0.62f, 0.12f, 0.16f, shade.copy(alpha = 0.5f))
    circle(0.4f, 0.4f, 0.05f, WHITE.copy(alpha = 0.7f))
    circle(0.62f, 0.34f, 0.03f, Color(0xFFFFD93B)) // sparkle
}

/** emoji -> drawer. */
private val ANIMAL_DRAWERS: Map<String, DrawScope.() -> Unit> = mapOf(
    "🐱" to { cat() }, "😺" to { cat() }, "😸" to { cat() }, "🐈" to { cat() },
    "🐶" to { dog() }, "🐕" to { dog() }, "🦮" to { dog() }, "🐩" to { dog() },
    "🦊" to { fox() },
    "🐰" to { bunny() }, "🐇" to { bunny() },
    "🐻" to { bear() }, "🐨" to { bear() },
    "🐼" to { panda() },
    "🐷" to { pig() }, "🐽" to { pig() },
    "🐸" to { frog() },
    "🐤" to { chick() }, "🐥" to { chick() }, "🐣" to { chick() },
    "🐧" to { penguin() },
    "🐮" to { cow() },
    "🥚" to { egg() }
)

fun hasAnimalArt(emoji: String): Boolean = ANIMAL_DRAWERS.containsKey(emoji)

/** Draws the cute illustration for [emoji], or falls back to the emoji glyph. */
@Composable
fun CuteAnimal(emoji: String, size: Dp, modifier: Modifier = Modifier) {
    val drawer = ANIMAL_DRAWERS[emoji]
    if (drawer != null) {
        Canvas(modifier.size(size)) { drawer() }
    } else {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = (size.value * 0.82f).sp)
        }
    }
}
