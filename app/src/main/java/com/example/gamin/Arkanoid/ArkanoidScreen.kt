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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// ============ CONSTANTS ============
private const val PADDLE_HEIGHT = 20f
private const val PADDLE_WIDTH = 150f
private const val PADDLE_Y_OFFSET = 60f

private const val BALL_SIZE = 30f
private const val BALL_SPEED = 700f
private const val INITIAL_BALL_ANGLE = 60.0

private const val BRICK_ROWS = 6
private const val BRICK_COLS = 8
private const val BRICK_PADDING = 5f
private const val BRICK_HEIGHT = 40f
private const val BRICK_SCORE = 10

private const val SCORE_PANEL_HEIGHT = 90f
private const val TOP_PADDING_OFFSET = SCORE_PANEL_HEIGHT + 10f

// ============ DATA MODELS ============
sealed class GameState {
    object Ready : GameState()
    object Playing : GameState()
    object GameOver : GameState()
}

data class PaddleState(val x: Float)
data class BallState(val x: Float, val y: Float, val velocityX: Float, val velocityY: Float)
data class BrickState(val rect: Rect, val isDestroyed: Boolean = false, val color: Color)

// ============ WAVE GENERATOR ============
fun createBrickPattern(gameWidth: Float, wave: Int): List<BrickState> {
    val totalBrickWidth = gameWidth - BRICK_PADDING * (BRICK_COLS + 1)
    val brickWidth = totalBrickWidth / BRICK_COLS
    val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Magenta, Color(0xFFFFA500))

    val bricks = mutableListOf<BrickState>()
    val patternType = wave % 5 // chọn pattern theo wave (và random)
    for (row in 0 until BRICK_ROWS) {
        for (col in 0 until BRICK_COLS) {
            val brickX = BRICK_PADDING + col * (brickWidth + BRICK_PADDING)
            val brickY = TOP_PADDING_OFFSET + row * (BRICK_HEIGHT + BRICK_PADDING)
            val show = when (patternType) {
                0 -> true // full block
                1 -> (row + col) % 2 == 0 // checkerboard
                2 -> col == row || col == BRICK_COLS - row - 1 // X pattern
                3 -> row < BRICK_ROWS / 2 && col in (2..5) // upper block
                4 -> Random.nextBoolean() // random scattered
                else -> true
            }
            if (show) {
                bricks.add(
                    BrickState(
                        rect = Rect(
                            left = brickX,
                            top = brickY,
                            right = brickX + brickWidth,
                            bottom = brickY + BRICK_HEIGHT
                        ),
                        color = colors.random()
                    )
                )
            }
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

// ============ MAIN GAME ============

@SuppressLint("ContextCastToActivity", "UnusedBoxWithConstraintsScope")
@Composable
fun ArkanoidScreen() {
    val context = LocalContext.current
    val activity = (context as? Activity)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gameWidth = constraints.maxWidth.toFloat()
        val gameHeight = constraints.maxHeight.toFloat()

        var paddle by remember { mutableStateOf(PaddleState(gameWidth / 2)) }
        var ball by remember { mutableStateOf(createInitialBall(gameWidth, gameHeight).copy(velocityX = 0f, velocityY = 0f)) }
        var bricks by remember { mutableStateOf(createBrickPattern(gameWidth, wave = 1)) }
        var gameState by remember { mutableStateOf<GameState>(GameState.Ready) }

        var score by remember { mutableIntStateOf(0) }
        var lives by remember { mutableIntStateOf(3) }
        var wave by remember { mutableIntStateOf(1) }

        // === RESET FUNCTIONS ===
        val resetBall = {
            ball = createInitialBall(gameWidth, gameHeight).copy(velocityX = 0f, velocityY = 0f)
            gameState = GameState.Ready
        }

        val nextWave = {
            wave++
            bricks = createBrickPattern(gameWidth, wave)
            ball = createInitialBall(gameWidth, gameHeight).copy(velocityX = 0f, velocityY = 0f)
            gameState = GameState.Ready
        }

        val restartGame = {
            score = 0
            lives = 3
            wave = 1
            paddle = PaddleState(gameWidth / 2)
            bricks = createBrickPattern(gameWidth, wave)
            ball = createInitialBall(gameWidth, gameHeight).copy(velocityX = 0f, velocityY = 0f)
            gameState = GameState.Ready
        }

        // === GAME LOOP ===
        LaunchedEffect(gameState) {
            if (gameState != GameState.Playing) return@LaunchedEffect
            var lastTime = System.nanoTime()

            while (gameState == GameState.Playing) {
                val now = System.nanoTime()
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                var newX = ball.x + ball.velocityX * dt
                var newY = ball.y + ball.velocityY * dt
                var vx = ball.velocityX
                var vy = ball.velocityY

                // wall collision
                if (newX - BALL_SIZE / 2 < 0) { vx *= -1; newX = BALL_SIZE / 2 }
                if (newX + BALL_SIZE / 2 > gameWidth) { vx *= -1; newX = gameWidth - BALL_SIZE / 2 }
                if (newY - BALL_SIZE / 2 < SCORE_PANEL_HEIGHT) { vy *= -1; newY = SCORE_PANEL_HEIGHT + BALL_SIZE / 2 }

                // bottom = lose life
                if (newY + BALL_SIZE / 2 > gameHeight - PADDLE_Y_OFFSET - TOP_PADDING_OFFSET) {
                    lives--
                    if (lives <= 0) {
                        gameState = GameState.GameOver
                    } else {
                        resetBall()
                    }
                }

                val paddleRect = Rect(
                    paddle.x - PADDLE_WIDTH / 2,
                    gameHeight - PADDLE_Y_OFFSET - PADDLE_HEIGHT - TOP_PADDING_OFFSET,
                    paddle.x + PADDLE_WIDTH / 2,
                    gameHeight - PADDLE_Y_OFFSET - TOP_PADDING_OFFSET
                )

                val ballRect = Rect(
                    newX - BALL_SIZE / 2, newY - BALL_SIZE / 2,
                    newX + BALL_SIZE / 2, newY + BALL_SIZE / 2
                )

                // paddle collision
                if (ballRect.overlaps(paddleRect) && vy > 0) {
                    vy *= -1
                    newY = paddleRect.top - BALL_SIZE / 2 - 1
                    val hitPoint = newX - paddle.x
                    val normalized = (hitPoint / (PADDLE_WIDTH / 2)).coerceIn(-1f, 1f)
                    val maxAngle = Math.toRadians(80.0)
                    val newAngle = maxAngle * normalized
                    val speed = sqrt(vx * vx + vy * vy)
                    vx = (speed * sin(newAngle)).toFloat()
                    vy = -(speed * cos(newAngle)).toFloat()
                }

                // brick collision
                var newBricks = bricks.toMutableList()
                for (i in newBricks.indices) {
                    val b = newBricks[i]
                    if (!b.isDestroyed && ballRect.overlaps(b.rect)) {
                        newBricks[i] = b.copy(isDestroyed = true)
                        vy *= -1
                        score += BRICK_SCORE * wave
                        break
                    }
                }

                bricks = newBricks

                // next wave
                if (bricks.all { it.isDestroyed }) {
                    nextWave()
                    continue
                }

                ball = ball.copy(x = newX, y = newY, velocityX = vx, velocityY = vy)
                delay(16)
            }
        }

        // === DRAW ===
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF001F3F))))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        paddle = paddle.copy(
                            x = (paddle.x + dragAmount).coerceIn(PADDLE_WIDTH / 2, gameWidth - PADDLE_WIDTH / 2)
                        )
                    }
                }
                .pointerInput(gameState) {
                    detectTapGestures {
                        when (gameState) {
                            GameState.Ready -> {
                                val init = createInitialBall(gameWidth, gameHeight)
                                ball = init.copy(
                                    x = paddle.x,
                                    y = init.y,
                                    velocityX = init.velocityX,
                                    velocityY = init.velocityY
                                )
                                gameState = GameState.Playing
                            }
                            GameState.GameOver -> restartGame()
                            else -> {}
                        }
                    }
                }
        ) {
            // bricks
            bricks.filter { !it.isDestroyed }.forEach { brick ->
                drawRoundRect(
                    color = brick.color,
                    topLeft = brick.rect.topLeft,
                    size = brick.rect.size,
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            // paddle
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    paddle.x - PADDLE_WIDTH / 2,
                    gameHeight - PADDLE_Y_OFFSET - PADDLE_HEIGHT - TOP_PADDING_OFFSET
                ),
                size = Size(PADDLE_WIDTH, PADDLE_HEIGHT),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // ball
            drawCircle(Color.Yellow, BALL_SIZE / 2, Offset(ball.x, ball.y))
        }

        // === HUD ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((SCORE_PANEL_HEIGHT / 2).dp)
                .background(Color(0xAA202020))
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Score: $score | Lives: $lives | Wave: $wave",
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
            )
        }

        // === STATUS TEXT ===
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (gameState) {
                GameState.Ready -> Text(
                    "TAP ĐỂ BẮT ĐẦU | KÉO ĐỂ DI CHUYỂN",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                )
                GameState.GameOver -> Text(
                    "GAME OVER! TAP ĐỂ CHƠI LẠI 😭",
                    style = MaterialTheme.typography.headlineLarge.copy(color = Color.Red)
                )
                else -> {}
            }
        }
    }
}
