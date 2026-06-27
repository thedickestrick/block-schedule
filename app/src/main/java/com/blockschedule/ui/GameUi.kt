package com.blockschedule.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockschedule.game.GameState
import kotlin.random.Random

/** Top card: progress ring for the day + level/points + streak. Tap to open achievements. */
@Composable
fun GameHeader(done: Int, total: Int, game: GameState, onClick: () -> Unit) {
    val fraction = if (total > 0) done.toFloat() / total else 0f
    val animated by animateFloatAsState(fraction, tween(600), label = "ring")
    val allDone = total > 0 && done == total
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
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

            // Streak buddy (evolves as the streak grows) + streak count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CuteAnimal(com.blockschedule.game.Buddy.formFor(game.streak).emoji, 40.dp)
                Text(
                    "🔥 ${game.streak}",
                    style = MaterialTheme.typography.titleMedium,
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

/** All the cute friends who show up to celebrate. */
val CUTE_ANIMALS = listOf(
    "🐱", "🐶", "🦊", "🐰", "🐮", "🐷", "🐭", "🐹", "🐻", "🐨", "🐯", "🦁",
    "🐸", "🐔", "🐥", "🐤", "🐧", "🦄", "🐴", "🐑", "🐐", "🦝", "🐼", "🦔", "🐣", "🐙"
)

fun randomAnimal(): String = CUTE_ANIMALS[Random.nextInt(CUTE_ANIMALS.size)]

private data class AnimalPiece(
    val emoji: String, val x: Float, val delay: Float,
    val size: Float, val drift: Float, val spin: Float
)

/**
 * A rain of cute animal emoji, re-triggered each time [trigger] changes. [count] controls how
 * many friends fall (a few for a single task, a whole parade for finishing the day).
 */
@Composable
fun AnimalShower(trigger: Int, count: Int, animals: List<String> = CUTE_ANIMALS) {
    if (trigger == 0) return
    val pool = animals.ifEmpty { CUTE_ANIMALS }
    val pieces = remember(trigger) {
        List(count) {
            AnimalPiece(
                emoji = pool[Random.nextInt(pool.size)],
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 0.25f,
                size = 26f + Random.nextFloat() * 18f,
                drift = (Random.nextFloat() - 0.5f) * 0.25f,
                spin = (Random.nextFloat() - 0.5f) * 90f
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(2200, easing = LinearEasing))
    }
    val p = progress.value
    if (p >= 1f) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        pieces.forEach { pc ->
            val prog = ((p - pc.delay) / (1f - pc.delay)).coerceIn(0f, 1f)
            val y = (-0.15f + prog * 1.25f) * h
            val x = (pc.x + pc.drift * kotlin.math.sin(prog * 6.28f)) * w
            Text(
                text = pc.emoji,
                fontSize = pc.size.sp,
                modifier = Modifier
                    .offset(x.dp, y.dp)
                    .graphicsLayer {
                        rotationZ = prog * pc.spin
                        alpha = (1f - prog).coerceIn(0f, 1f)
                    }
            )
        }
    }
}

/** Center popup when a cute badge is earned. */
@Composable
fun AchievementPopup(achievement: com.blockschedule.game.Achievement?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = achievement != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(400))
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(achievement?.emoji ?: "", fontSize = 56.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Badge unlocked!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        achievement?.title ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Center popup when the streak buddy evolves into a new animal. */
@Composable
fun EvolutionPopup(form: com.blockschedule.game.BuddyForm?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = form != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(400))
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CuteAnimal(form?.emoji ?: "🥚", 72.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your buddy evolved!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        form?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Sassy "are you really done or just being a brat?" check for speed-tappers. */
@Composable
fun BratDialog(visible: Boolean, onReallyDid: () -> Unit, onBusted: () -> Unit) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onReallyDid,
        icon = { Text("😼", fontSize = 48.sp) },
        title = { Text("Hold on a sec…") },
        text = {
            Text(
                "Whoa, that was fast! Did you REALLY finish all that, " +
                    "or are you just being a little brat? 😼"
            )
        },
        confirmButton = { TextButton(onClick = onReallyDid) { Text("I really did it! 😇") } },
        dismissButton = { TextButton(onClick = onBusted) { Text("…busted 🙈") } }
    )
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
