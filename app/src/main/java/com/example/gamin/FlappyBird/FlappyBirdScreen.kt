package com.example.gamin.FlappyBird

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.gamin.R
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val BIRD_SIZE = 40f
private const val GRAVITY = 1000f
private const val JUMP_VELOCITY = -500f
private const val PIPE_WIDTH = 100f
private const val PIPE_GAP = 200f
private const val PIPE_SPEED = 300f

sealed class GameState {
    object Ready : GameState()
    object Playing : GameState()
    object Crashing : GameState()
    object GameOver : GameState()
}

data class BirdState(
    val y: Float,
    val velocity: Float,
    val rotation: Float = 0f
)

data class PipeState(
    val x: Float,
    val gapY: Float,
    var scored: Boolean = false
)

@SuppressLint("UnusedBoxWithConstraintsScope", "ContextCastToActivity")
@Suppress("BoxWithConstraintsScopeIsNotUsed")
@Composable
fun FlappyBirdScreen() {
    var birdState by remember { mutableStateOf(BirdState(y = 500f, velocity = 0f)) }
    var pipes by remember { mutableStateOf(emptyList<PipeState>()) }
    var score by remember { mutableIntStateOf(0) }
    var gameState by remember { mutableStateOf<GameState>(GameState.Ready) }
    var groundOffset by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val birdBitmap = remember {
        ImageBitmap.imageResource(context.resources, R.drawable.ic_bird)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gameHeight = constraints.maxHeight.toFloat()
        val gameWidth = constraints.maxWidth.toFloat()
        val birdX = gameWidth / 4

        val resetGame: () -> Unit = {
            birdState = BirdState(y = gameHeight / 2, velocity = 0f, rotation = 0f)
            pipes = listOf(PipeState(x = gameWidth * 1.5f, gapY = gameHeight / 2))
            score = 0
            gameState = GameState.Ready
        }

        LaunchedEffect(gameState, gameWidth, gameHeight) {
            if (gameState == GameState.Ready && pipes.isEmpty()) {
                pipes = listOf(PipeState(x = gameWidth * 1.5f, gapY = gameHeight / 2))
            }
        }

        val tapAction: () -> Unit = {
            when (gameState) {
                GameState.Ready -> {
                    gameState = GameState.Playing
                    birdState = birdState.copy(velocity = JUMP_VELOCITY)
                }
                GameState.Playing -> {
                    birdState = birdState.copy(velocity = JUMP_VELOCITY)
                }
                GameState.GameOver -> {
                    resetGame()
                }
                GameState.Crashing -> {}
            }
        }

        val lastUpdateTime = rememberUpdatedState(System.nanoTime())
        LaunchedEffect(gameState, gameHeight, gameWidth) {
            if (gameState != GameState.Playing && gameState != GameState.Crashing) {
                return@LaunchedEffect
            }
            var lastTime = lastUpdateTime.value
            while (gameState == GameState.Playing || gameState == GameState.Crashing) {
                val currentTime = System.nanoTime()
                val dt = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime
                if (gameState == GameState.Playing) {
                    groundOffset = (groundOffset - PIPE_SPEED * dt) % 100f
                }
                val newVelocity = birdState.velocity + GRAVITY * dt
                val newY = (birdState.y + birdState.velocity * dt).coerceIn(0f, gameHeight)
                val newRotation = (newVelocity / (JUMP_VELOCITY * -1.5f))
                    .coerceIn(-90f, 30f)
                birdState = birdState.copy(velocity = newVelocity, y = newY, rotation = newRotation)
                if (gameState == GameState.Playing) {
                    val newPipes = pipes.map { pipe ->
                        pipe.copy(x = pipe.x - PIPE_SPEED * dt)
                    }.filter { it.x > -PIPE_WIDTH }
                    if (newPipes.last().x < gameWidth - (PIPE_SPEED * 1.8f)) {
                        val newGapY = Random.nextInt(
                            (gameHeight * 0.2f).toInt(),
                            (gameHeight * 0.8f).toInt()
                        ).toFloat()
                        pipes = newPipes + PipeState(x = gameWidth + PIPE_WIDTH, gapY = newGapY)
                    } else {
                        pipes = newPipes
                    }
                    val birdRect = Rect(
                        birdX - BIRD_SIZE / 2,
                        birdState.y - BIRD_SIZE / 2,
                        birdX + BIRD_SIZE / 2,
                        birdState.y + BIRD_SIZE / 2
                    )
                    var collision = false
                    pipes.forEach { pipe ->
                        val topPipeRect = Rect(
                            pipe.x, 0f,
                            pipe.x + PIPE_WIDTH, pipe.gapY - PIPE_GAP / 2
                        )
                        val bottomPipeRect = Rect(
                            pipe.x, pipe.gapY + PIPE_GAP / 2,
                            pipe.x + PIPE_WIDTH, gameHeight
                        )
                        if (birdRect.overlaps(topPipeRect) || birdRect.overlaps(bottomPipeRect)) {
                            collision = true
                        }
                        if (!pipe.scored && pipe.x + PIPE_WIDTH < birdX) {
                            pipe.scored = true
                            score++
                        }
                    }
                    if (birdState.y + BIRD_SIZE > gameHeight) {
                        collision = true
                    }
                    if (birdState.y <= 0f) {
                        birdState = birdState.copy(y = 0f, velocity = 0f)
                    }
                    if (collision) {
                        gameState = GameState.Crashing
                    }
                }
                if (gameState == GameState.Crashing) {
                    if (birdState.y + BIRD_SIZE >= gameHeight) {
                        gameState = GameState.GameOver
                        birdState = birdState.copy(
                            y = gameHeight - BIRD_SIZE,
                            velocity = 0f,
                            rotation = -90f
                        )
                    }
                }
                delay(16)
            }
        }

        val fullModifier = Modifier
            .fillMaxSize()
            .pointerInput(gameState) {
                detectTapGestures { tapAction() }
            }

        Canvas(
            modifier = fullModifier
                .background(Color(0xFF7DE6F5))
        ) {
            val birdY = birdState.y.coerceIn(0f, gameHeight - BIRD_SIZE)

            pipes.forEach { pipe ->
                val pipeColor = Color(0xFF4CAF50)
                val pipeShadow = Color(0xFF2E7D32)

                drawRoundRect(
                    color = pipeColor,
                    topLeft = Offset(pipe.x, 0f),
                    size = Size(PIPE_WIDTH, pipe.gapY - PIPE_GAP / 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
                )
                drawRect(
                    color = pipeShadow,
                    topLeft = Offset(pipe.x, (pipe.gapY - PIPE_GAP / 2) - 10),
                    size = Size(PIPE_WIDTH, 10f)
                )
                drawRoundRect(
                    color = pipeColor,
                    topLeft = Offset(pipe.x, pipe.gapY + PIPE_GAP / 2),
                    size = Size(PIPE_WIDTH, gameHeight - (pipe.gapY + PIPE_GAP / 2)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
                )
                drawRect(
                    color = pipeShadow,
                    topLeft = Offset(pipe.x, pipe.gapY + PIPE_GAP / 2),
                    size = Size(PIPE_WIDTH, 10f)
                )
            }

            withTransform({
                rotate(degrees = birdState.rotation, pivot = Offset(birdX, birdY))
            }) {
                drawImage(
                    image = birdBitmap,
                    dstOffset = IntOffset(
                        (birdX - BIRD_SIZE / 2).toInt(),
                        (birdY - BIRD_SIZE / 2).toInt()
                    ),
                    dstSize = IntSize(BIRD_SIZE.toInt(), BIRD_SIZE.toInt())
                )
            }

            if (gameState == GameState.Crashing || gameState == GameState.GameOver) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset.Zero,
                    size = size
                )
            }
        }

        val activity = (LocalContext.current as? Activity)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val buttonModifier = Modifier.width(120.dp)
            Button(
                onClick = { activity?.finish() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = buttonModifier
            ) {
                Text("Quay lại")
            }

            Text("Score: $score", style = MaterialTheme.typography.headlineMedium.copy(color = Color.White))

            Box(modifier = buttonModifier) {
                if (gameState == GameState.GameOver) {
                    Button(
                        onClick = tapAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chơi Lại")
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (gameState == GameState.GameOver) {
                Text(
                    "GAME OVER! 💥",
                    style = MaterialTheme.typography.headlineLarge.copy(color = Color.Red)
                )
            } else if (gameState == GameState.Ready) {
                Text(
                    "TAP ĐỂ BẮT ĐẦU!",
                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFFD2B48C))
                .align(Alignment.BottomCenter)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0..size.width.toInt() step 100) {
                    drawRect(
                        color = Color(0xFFB8860B),
                        topLeft = Offset(i.toFloat() + groundOffset, 0f),
                        size = Size(50f, 60f)
                    )
                }
            }
        }
    }
}
