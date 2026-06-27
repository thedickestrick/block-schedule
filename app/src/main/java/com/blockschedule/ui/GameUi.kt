package com.blockschedule.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockschedule.game.GameState
import kotlin.random.Random

/** Top card: progress ring for the day + level/points + streak. */
@Composable
fun GameHeader(done: Int, total: Int, game: GameState) {
    val fraction = if (total > 0) done.toFloat() / total else 0f
    val animated by animateFloatAsState(fraction, tween(600), label = "ring")
    val allDone = total > 0 && done == total
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allDone) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                Canvas(Modifier.size(64.dp)) {
                    val stroke = 9.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke)
                    )
                    drawArc(
                        color = ringColor, startAngle = -90f, sweepAngle = animated * 360f,
                        useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    "$done/$total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    if (allDone) "All done today! 🎉" else "Level ${game.level}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { game.levelProgress.toFloat() / game.pointsPerLevel },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${game.points} pts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", fontSize = 22.sp)
                Text(
                    "${game.streak}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "day streak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Confetto(
    val x: Float, val startY: Float, val color: Color,
    val size: Float, val drift: Float, val speed: Float
)

/** A short confetti burst, re-triggered each time [trigger] changes (and is > 0). */
@Composable
fun ConfettiOverlay(trigger: Int) {
    if (trigger == 0) return
    val colors = listOf(
        Color(0xFF3B5BDB), Color(0xFFE8590C), Color(0xFF2B8A3E),
        Color(0xFFC2255C), Color(0xFFFFD43B), Color(0xFF9C36B5)
    )
    val pieces = remember(trigger) {
        List(70) {
            Confetto(
                x = Random.nextFloat(),
                startY = -Random.nextFloat() * 0.3f,
                color = colors[Random.nextInt(colors.size)],
                size = 8f + Random.nextFloat() * 10f,
                drift = (Random.nextFloat() - 0.5f) * 0.3f,
                speed = 0.8f + Random.nextFloat() * 0.5f
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1900, easing = LinearEasing))
    }
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        pieces.forEach { c ->
            val y = (c.startY + p * c.speed) * size.height * 1.25f
            val x = (c.x + c.drift * p) * size.width
            val alpha = (1f - p).coerceIn(0f, 1f)
            drawRect(
                color = c.color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(c.size, c.size * 1.6f)
            )
        }
    }
}

/** Floating message (e.g. "+10") that rises and fades near the top. */
@Composable
fun FloatingPoints(message: String?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(400))
        ) {
            Card(
                modifier = Modifier.padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    message ?: "",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
