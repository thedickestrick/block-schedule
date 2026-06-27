package com.blockschedule.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockschedule.game.DancePlayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen dopamine dance party: flashing colors, a spinning disco ball, dancing animals
 * bopping to the beat, and the catchy tune. Shown for big wins (a task marked "party", or the
 * whole day finished). Plays music while visible; auto-closes after a loop or on tap.
 */
@Composable
fun DancePartyOverlay(message: String?, animals: List<String>, onDismiss: () -> Unit) {
    if (message == null) return
    val context = LocalContext.current

    DisposableEffect(message) {
        val sp = com.blockschedule.game.SpotifyPrefs(context)
        if (sp.isConfigured) {
            com.blockschedule.game.SpotifyController.playPlaylist(
                context, sp.clientId, sp.redirectUri, sp.playlistUri
            ) { ok -> if (!ok) DancePlayer.start(context) } // fall back to built-in tune
        } else {
            DancePlayer.start(context)
        }
        onDispose {
            com.blockschedule.game.SpotifyController.pause()
            DancePlayer.stop()
        }
    }
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(13_500)
        onDismiss()
    }

    val t = rememberInfiniteTransition(label = "party")
    val beat by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Restart), label = "beat"
    )
    val spin by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "spin"
    )
    val hue by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart), label = "hue"
    )
    val bg = Color.hsv(hue % 360f, 0.5f, 1f)
    val wob = sin(beat * 2 * PI).toFloat()

    val dancers = remember(message) {
        val pool = animals.ifEmpty { CUTE_ANIMALS }
        List(7) { pool[Random.nextInt(pool.size)] }
    }

    Box(
        Modifier.fillMaxSize().background(bg).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🪩", fontSize = 70.sp, modifier = Modifier.graphicsLayer { rotationZ = spin })
            Spacer(Modifier.height(10.dp))
            Text(
                "DANCE PARTY!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    rotationZ = wob * 4f
                    val s = 1f + 0.06f * wob; scaleX = s; scaleY = s
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                dancers.forEachIndexed { i, a ->
                    val b = sin((beat + i * 0.14f) * 2 * PI).toFloat()
                    CuteAnimal(
                        a, 46.dp,
                        modifier = Modifier.graphicsLayer {
                            translationY = -abs(b) * 42f
                            rotationZ = b * 16f
                            val s = 1f + 0.12f * b; scaleX = s; scaleY = s
                        }
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            Button(onClick = onDismiss) { Text("Yay! 🎉") }
            Spacer(Modifier.height(6.dp))
            Text(
                "tap anywhere to close",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
