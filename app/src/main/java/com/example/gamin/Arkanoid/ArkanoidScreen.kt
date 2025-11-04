package com.example.gamin.Arkanoid

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*

private const val PADDLE_HEIGHT = 20f
private const val PADDLE_WIDTH = 150f
private const val PADDLE_Y_OFFSET = 60f

private const val BALL_SIZE = 30f
private const val BALL_SPEED = 700f
private const val INITIAL_BALL_ANGLE = 60.0

private const val BRICK_ROWS = 5
private const val BRICK_COLS = 8
private const val BRICK_PADDING = 5f
private const val BRICK_HEIGHT = 40f
private const val BRICK_SCORE = 10

private const val SCORE_PANEL_HEIGHT = 90f
private const val TOP_PADDING_OFFSET = SCORE_PANEL_HEIGHT + 10f

sealed class GameState {
    object Ready : GameState()
    object Playing : GameState()
    object GameOver : GameState()
    object Win : GameState()
}

data class PaddleState(val x: Float, val lives: Int = 3)
data class BallState(val x: Float, val y: Float, val velocityX: Float, val velocityY: Float)
data class BrickState(val rect: Rect, val isDestroyed: Boolean = false, val color: Color)

fun createBricks(gameWidth: Float): List<BrickState> {
    val totalBrickWidth = gameWidth - BRICK_PADDING * (BRICK_COLS + 1)
    val brickWidth = totalBrickWidth / BRICK_COLS
    val colors = listOf(Color.Red, Color(0xFF757575), Color.Yellow, Color.Green, Color.Blue)

    val bricks = mutableListOf<BrickState>()
    for (row in 0 until BRICK_ROWS) {
        val brickY = TOP_PADDING_OFFSET + BRICK_PADDING + row * (BRICK_HEIGHT + BRICK_PADDING)
        for (col in 0 until BRICK_COLS) {
            val brickX = BRICK_PADDING + col * (brickWidth + BRICK_PADDING)
            bricks.add(
                BrickState(
                    rect = Rect(
                        left = brickX,
                        top = brickY,
                        right = brickX + brickWidth,
                        bottom = brickY + BRICK_HEIGHT
                    ),
                    color = colors[row % colors.size]
                )
            )
        }
    }
    return bricks
}

fun createInitialBall(gameWidth: Float, gameHeight: Float): BallState {
    val angleRadians = Math.toRadians(INITIAL_BALL_ANGLE)
    return BallState(
        x = gameWidth / 2,
        y = gameHeight - PADDLE_Y_OFFSET - PADDLE_HEIGHT - BALL_SIZE / 2 - 1f - TOP_PADDING_OFFSET,
        velocityX = BALL_SPEED * cos(angleRadians).toFloat(),
        velocityY = -BALL_SPEED * sin(angleRadians).toFloat()
    )
}

@SuppressLint("ContextCastToActivity", "UnusedBoxWithConstraintsScope")
@Composable
fun ArkanoidScreen() {
    val context = LocalContext.current
    val activity = (LocalContext.current as? Activity)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gameHeight = constraints.maxHeight.toFloat()
        val gameWidth = constraints.maxWidth.toFloat()

        var paddleState by remember { mutableStateOf(PaddleState(x = gameWidth / 2)) }
        var ballState by remember {
            mutableStateOf(createInitialBall(gameWidth, gameHeight).copy(velocityX = 0f, velocityY = 0f))
        }
        var bricks by remember { mutableStateOf(createBricks(gameWidth)) }
        var gameState by remember { mutableStateOf<GameState>(GameState.Ready) }
        var score by remember { mutableIntStateOf(0) }
        var currentLives by remember { mutableIntStateOf(3) }

        val fullReset: () -> Unit = {
            paddleState = PaddleState(x = gameWidth / 2)
            bricks = createBricks(gameWidth)
            score = 0
            currentLives = 3
            gameState = GameState.Ready
            ballState = createInitialBall(gameWidth, gameHeight).copy(velocityX = 0f, velocityY = 0f)
        }

        val resetBall: () -> Unit = {
            ballState = createInitialBall(gameWidth, gameHeight).copy(velocityX = 0f, velocityY = 0f)
            gameState = GameState.Ready
        }

        val lastUpdateTime = rememberUpdatedState(System.nanoTime())
        LaunchedEffect(gameState, gameHeight, gameWidth) {
            if (gameState != GameState.Playing) return@LaunchedEffect
            var lastTime = lastUpdateTime.value

            while (gameState == GameState.Playing) {
                val currentTime = System.nanoTime()
                val dt = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                var newBallX = ballState.x + ballState.velocityX * dt
                var newBallY = ballState.y + ballState.velocityY * dt
                var currentVx = ballState.velocityX
                var currentVy = ballState.velocityY

                val ballRect = Rect(
                    newBallX - BALL_SIZE / 2, newBallY - BALL_SIZE / 2,
                    newBallX + BALL_SIZE / 2, newBallY + BALL_SIZE / 2
                )

                if (newBallX - BALL_SIZE / 2 < 0f) {
                    currentVx *= -1; newBallX = BALL_SIZE / 2
                } else if (newBallX + BALL_SIZE / 2 > gameWidth) {
                    currentVx *= -1; newBallX = gameWidth - BALL_SIZE / 2
                }
                if (newBallY - BALL_SIZE / 2 < SCORE_PANEL_HEIGHT) {
                    currentVy *= -1
                    newBallY = SCORE_PANEL_HEIGHT + BALL_SIZE / 2
                }
                if (newBallY + BALL_SIZE / 2 > gameHeight - PADDLE_Y_OFFSET - TOP_PADDING_OFFSET) {
                    currentLives--
                    if (currentLives <= 0) gameState = GameState.GameOver
                    else {
                        resetBall(); return@LaunchedEffect
                    }
                }

                val paddleRect = Rect(
                    paddleState.x - PADDLE_WIDTH / 2,
                    gameHeight - PADDLE_Y_OFFSET - PADDLE_HEIGHT - TOP_PADDING_OFFSET,
                    paddleState.x + PADDLE_WIDTH / 2,
                    gameHeight - PADDLE_Y_OFFSET - TOP_PADDING_OFFSET
                )
                if (ballRect.overlaps(paddleRect) && currentVy > 0) {
                    currentVy *= -1
                    newBallY = paddleRect.top - BALL_SIZE / 2 - 1f
                    val hitPoint = newBallX - paddleState.x
                    val normalizedHit = (hitPoint / (PADDLE_WIDTH / 2)).coerceIn(-1f, 1f)
                    val maxAngleRadians = Math.toRadians(80.0)
                    val newAngle = maxAngleRadians * normalizedHit
                    val speed = sqrt(currentVx * currentVx + currentVy * currentVy)
                    currentVx = (speed * sin(newAngle)).toFloat()
                    currentVy = -(speed * cos(newAngle)).toFloat()
                }

                var updatedBricks = bricks.toMutableList()
                for (i in updatedBricks.indices) {
                    val brick = updatedBricks[i]
                    if (!brick.isDestroyed && ballRect.overlaps(brick.rect)) {
                        score += BRICK_SCORE
                        updatedBricks[i] = brick.copy(isDestroyed = true)
                        currentVy *= -1
                        break
                    }
                }
                bricks = updatedBricks
                if (bricks.all { it.isDestroyed }) gameState = GameState.Win

                ballState = ballState.copy(x = newBallX, y = newBallY, velocityX = currentVx, velocityY = currentVy)
                delay(16)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF001F3F))))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (paddleState.x + dragAmount).coerceIn(
                            PADDLE_WIDTH / 2, gameWidth - PADDLE_WIDTH / 2
                        )
                        paddleState = paddleState.copy(x = newX)
                    }
                }
                .pointerInput(gameState) {
                    detectTapGestures {
                        if (gameState == GameState.Ready) {
                            val initialBall = createInitialBall(gameWidth, gameHeight)
                            ballState = initialBall.copy(
                                x = paddleState.x,
                                y = initialBall.y,
                                velocityX = initialBall.velocityX,
                                velocityY = initialBall.velocityY
                            )
                            gameState = GameState.Playing
                        } else if (gameState == GameState.GameOver || gameState == GameState.Win) fullReset()
                    }
                }
        ) {
            bricks.filter { !it.isDestroyed }.forEach { brick ->
                drawRoundRect(
                    color = brick.color,
                    topLeft = brick.rect.topLeft,
                    size = brick.rect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    paddleState.x - PADDLE_WIDTH / 2,
                    gameHeight - PADDLE_Y_OFFSET - PADDLE_HEIGHT - TOP_PADDING_OFFSET
                ),
                size = Size(PADDLE_WIDTH, PADDLE_HEIGHT),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )

            drawCircle(Color.Yellow, BALL_SIZE / 2, Offset(ballState.x, ballState.y))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((SCORE_PANEL_HEIGHT / 2).dp)
                .background(Color(0xAA202020))
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Score: $score | Lives: $currentLives",
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White)
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (gameState) {
                GameState.GameOver ->
                    Text("GAME OVER! 😭", style = MaterialTheme.typography.headlineLarge.copy(color = Color.Red))
                GameState.Ready ->
                    Text(
                        "KÉO ĐỂ DI CHUYỂN, TAP ĐỂ BẮT ĐẦU!",
                        style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
                    )
                GameState.Win ->
                    Text("CHIẾN THẮNG! 🎉", style = MaterialTheme.typography.headlineLarge.copy(color = Color.Green))
                else -> {}
            }
        }
    }
}
