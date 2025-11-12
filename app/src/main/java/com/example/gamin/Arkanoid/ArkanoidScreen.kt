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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random // Đảm bảo đã import

// Data class để theo dõi hiệu ứng và thời gian
private data class ActiveEffect(
    val type: PowerUpType,
    val expirationTime: Long // Thời điểm hết hạn (ms)
)

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
        var paddleWidth by remember { mutableFloatStateOf(INITIAL_PADDLE_WIDTH) }

        var activeEffects by remember { mutableStateOf(emptySet<ActiveEffect>()) }

        var gameState by remember { mutableStateOf<GameState>(GameState.Ready) }
        var score by remember { mutableIntStateOf(0) }
        var lives by remember { mutableIntStateOf(3) }
        var wave by remember { mutableIntStateOf(1) }
        var countdown by remember { mutableIntStateOf(3) }

        var balls by remember {
            mutableStateOf(listOf(createInitialBall(gameWidth, gameHeight, INITIAL_BALL_SPEED).copy(velocityX = 0f, velocityY = 0f)))
        }
        var bricks by remember { mutableStateOf(createBrickPattern(gameWidth, wave = 1)) }
        var powerUps by remember { mutableStateOf(emptyList<PowerUpItem>()) }

        // === RESET FUNCTIONS ===
        val resetBallAndPaddle = {
            paddleWidth = INITIAL_PADDLE_WIDTH
            activeEffects = emptySet()
            val speed = INITIAL_BALL_SPEED
            balls = listOf(createInitialBall(gameWidth, gameHeight, speed).copy(velocityX = 0f, velocityY = 0f))
            powerUps = emptyList()
            gameState = GameState.Ready
        }

        val nextWave = {
            wave++
            bricks = createBrickPattern(gameWidth, wave)
            resetBallAndPaddle()
        }

        val restartGame = {
            score = 0
            lives = 3
            wave = 1
            bricks = createBrickPattern(gameWidth, wave)
            resetBallAndPaddle()
        }

        // === GAME LOOP ===
        LaunchedEffect(gameState) {
            if (gameState != GameState.Playing) return@LaunchedEffect
            var lastTime = System.nanoTime()

            while (gameState == GameState.Playing) {
                val now = System.nanoTime()
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                // 1. KIỂM TRA HIỆU ỨNG (Timer 5 giây)
                val currentTime = System.currentTimeMillis()
                val (expired, active) = activeEffects.partition { it.expirationTime < currentTime }

                if (expired.isNotEmpty()) {
                    activeEffects = active.toSet()
                    val hadPaddleGrow = expired.any { it.type == PowerUpType.PADDLE_GROW }
                    if (hadPaddleGrow && active.none { it.type == PowerUpType.PADDLE_GROW }) {
                        paddleWidth = INITIAL_PADDLE_WIDTH
                    }
                }

                // 2. CẬP NHẬT TRẠNG THÁI BÓNG
                var newBalls = balls.toMutableList()
                val ballsToRemove = mutableListOf<BallState>()

                for (i in newBalls.indices) {
                    val ball = newBalls[i]
                    var newX = ball.x + ball.velocityX * dt
                    var newY = ball.y + ball.velocityY * dt
                    var vx = ball.velocityX
                    var vy = ball.velocityY

                    if (newX - BALL_SIZE / 2 < 0) { vx *= -1; newX = BALL_SIZE / 2 }
                    if (newX + BALL_SIZE / 2 > gameWidth) { vx *= -1; newX = gameWidth - BALL_SIZE / 2 }
                    if (newY - BALL_SIZE / 2 < SCORE_PANEL_HEIGHT) { vy *= -1; newY = SCORE_PANEL_HEIGHT + BALL_SIZE / 2 }
                    if (newY + BALL_SIZE / 2 > gameHeight) {
                        ballsToRemove.add(ball)
                        continue
                    }

                    val paddleRect = Rect(
                        paddle.x - paddleWidth / 2,
                        gameHeight - PADDLE_Y_OFFSET - INITIAL_PADDLE_HEIGHT - TOP_PADDING_OFFSET,
                        paddle.x + paddleWidth / 2,
                        gameHeight - PADDLE_Y_OFFSET - TOP_PADDING_OFFSET
                    )
                    val ballRect = Rect(newX - BALL_SIZE / 2, newY - BALL_SIZE / 2, newX + BALL_SIZE / 2, newY + BALL_SIZE / 2)

                    if (ballRect.overlaps(paddleRect) && vy > 0) {
                        vy *= -1
                        newY = paddleRect.top - BALL_SIZE / 2 - 1
                        val hitPoint = newX - paddle.x
                        val normalized = (hitPoint / (paddleWidth / 2)).coerceIn(-1f, 1f)
                        val maxAngle = Math.toRadians(80.0)
                        val newAngle = maxAngle * normalized
                        val currentSpeed = sqrt(vx * vx + vy * vy)
                        val newSpeed = currentSpeed * 1.02f
                        vx = (newSpeed * sin(newAngle)).toFloat()
                        vy = -(newSpeed * cos(newAngle)).toFloat()
                    }
                    newBalls[i] = ball.copy(x = newX, y = newY, velocityX = vx, velocityY = vy)
                }
                newBalls.removeAll(ballsToRemove)

                // 3. KIỂM TRA MẠNG
                if (newBalls.isEmpty() && balls.isNotEmpty()) {
                    lives--
                    if (lives <= 0) {
                        gameState = GameState.GameOver
                    } else {
                        resetBallAndPaddle()
                    }
                    continue
                }

                // 4. CẬP NHẬT VA CHẠM GẠCH
                var newBricks = bricks.toMutableList()
                val bricksToDestroy = mutableSetOf<Int>()
                val newPowerUps = mutableListOf<PowerUpItem>()

                for (ballIndex in newBalls.indices) {
                    val ball = newBalls[ballIndex]
                    val ballRect = Rect(ball.x - BALL_SIZE / 2, ball.y - BALL_SIZE / 2, ball.x + BALL_SIZE / 2, ball.y + BALL_SIZE / 2)
                    var ballHitInThisFrame = false

                    for (brickIndex in newBricks.indices) {
                        if (ballHitInThisFrame) break
                        val brick = newBricks[brickIndex]
                        if (brick.isDestroyed || bricksToDestroy.contains(brickIndex)) continue

                        if (ballRect.overlaps(brick.rect)) {
                            ballHitInThisFrame = true
                            bricksToDestroy.add(brickIndex)
                            score += BRICK_SCORE * wave
                            newBalls[ballIndex] = ball.copy(velocityY = -ball.velocityY)

                            if (brick.type == BrickType.EXPLOSIVE) {
                                val hitRow = brickIndex / BRICK_COLS
                                val hitCol = brickIndex % BRICK_COLS
                                for (r in (hitRow - 1)..(hitRow + 1)) {
                                    for (c in (hitCol - 1)..(hitCol + 1)) {
                                        if (r == hitRow && c == hitCol) continue
                                        if (r in 0 until BRICK_ROWS && c in 0 until BRICK_COLS) {
                                            val neighborIndex = r * BRICK_COLS + c
                                            if (neighborIndex < newBricks.size && !newBricks[neighborIndex].isDestroyed) {
                                                bricksToDestroy.add(neighborIndex)
                                                score += (BRICK_SCORE * wave) / 2
                                            }
                                        }
                                    }
                                }
                            }

                            if (brick.powerUp != null) {
                                newPowerUps.add(PowerUpItem(
                                    x = brick.rect.center.x,
                                    y = brick.rect.center.y,
                                    type = brick.powerUp
                                ))
                            }
                        }
                    }
                }
                if (bricksToDestroy.isNotEmpty()) {
                    newBricks = newBricks.mapIndexed { index, brick ->
                        if (bricksToDestroy.contains(index)) brick.copy(isDestroyed = true) else brick
                    }.toMutableList()
                }

                // 5. CẬP NHẬT VẬT PHẨM
                var activePowerUps = powerUps.toMutableList()
                val powerUpsToRemove = mutableListOf<PowerUpItem>()
                activePowerUps.addAll(newPowerUps)

                val paddleRect = Rect(
                    paddle.x - paddleWidth / 2,
                    gameHeight - PADDLE_Y_OFFSET - INITIAL_PADDLE_HEIGHT - TOP_PADDING_OFFSET,
                    paddle.x + paddleWidth / 2,
                    gameHeight - PADDLE_Y_OFFSET - TOP_PADDING_OFFSET
                )

                for (item in activePowerUps) {
                    val newItemY = item.y + POWER_UP_SPEED * dt
                    if (newItemY > gameHeight) {
                        powerUpsToRemove.add(item)
                        continue
                    }
                    val itemRect = Rect(
                        item.x - POWER_UP_SIZE / 2, newItemY - POWER_UP_SIZE / 2,
                        item.x + POWER_UP_SIZE / 2, newItemY + POWER_UP_SIZE / 2
                    )

                    if (itemRect.overlaps(paddleRect)) {
                        powerUpsToRemove.add(item)
                        val expirationTime = System.currentTimeMillis() + POWER_UP_DURATION_MS

                        when (item.type) {
                            PowerUpType.MULTI_BALL -> {
                                val speed = INITIAL_BALL_SPEED
                                for (i in 0..4) {
                                    val randomAngleRad = Random.nextDouble(Math.PI / 6, 5 * Math.PI / 6).toFloat()
                                    newBalls.add(
                                        BallState(
                                            x = paddle.x,
                                            y = paddleRect.top - BALL_SIZE / 2,
                                            velocityX = speed * cos(randomAngleRad),
                                            velocityY = -speed * sin(randomAngleRad)
                                        )
                                    )
                                }
                            }
                            PowerUpType.PADDLE_GROW -> {
                                paddleWidth = (paddleWidth * 1.3f).coerceAtMost(gameWidth * 0.8f)
                                activeEffects = (activeEffects.filterNot { it.type == item.type } + ActiveEffect(item.type, expirationTime)).toSet()
                            }
                        }
                    } else {
                        activePowerUps[activePowerUps.indexOf(item)] = item.copy(y = newItemY)
                    }
                }
                activePowerUps.removeAll(powerUpsToRemove)

                balls = newBalls
                bricks = newBricks
                powerUps = activePowerUps

                // 6. KIỂM TRA QUA MÀN
                if (bricks.all { it.isDestroyed }) {
                    gameState = GameState.WaveClear
                    countdown = 3
                    continue
                }
                delay(16)
            }
        }

        // VÒNG LẶP ĐẾM NGƯỢC
        LaunchedEffect(gameState) {
            if (gameState != GameState.WaveClear) return@LaunchedEffect
            balls = emptyList()
            powerUps = emptyList()
            while (countdown > 0 && gameState == GameState.WaveClear) {
                delay(1000)
                countdown--
            }
            if (gameState == GameState.WaveClear) {
                nextWave()
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
                            x = (paddle.x + dragAmount).coerceIn(paddleWidth / 2, gameWidth - paddleWidth / 2)
                        )
                    }
                }
                .pointerInput(gameState) {
                    detectTapGestures {
                        when (gameState) {
                            GameState.Ready -> {
                                val speed = INITIAL_BALL_SPEED
                                val init = createInitialBall(gameWidth, gameHeight, speed)
                                balls = listOf(init.copy(x = paddle.x))
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
                if (brick.type == BrickType.EXPLOSIVE) {
                    drawCircle(Color.Red, radius = 10f, center = brick.rect.center)
                }
            }

            // paddle
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    paddle.x - paddleWidth / 2,
                    gameHeight - PADDLE_Y_OFFSET - INITIAL_PADDLE_HEIGHT - TOP_PADDING_OFFSET
                ),
                size = Size(paddleWidth, INITIAL_PADDLE_HEIGHT),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // balls
            balls.forEach { ball ->
                drawCircle(Color.Yellow, BALL_SIZE / 2, Offset(ball.x, ball.y))
            }

            // power-ups
            powerUps.forEach { item ->
                val color = when(item.type) {
                    PowerUpType.MULTI_BALL -> Color.Green
                    PowerUpType.PADDLE_GROW -> Color.Cyan
                }
                drawRect(color, topLeft = Offset(item.x - POWER_UP_SIZE / 2, item.y - POWER_UP_SIZE / 2), size = Size(POWER_UP_SIZE, POWER_UP_SIZE))
            }
        }

        // 1. HUD (Chỉ số)
        ArkanoidHud(
            score = score,
            lives = lives,
            wave = wave,
            onBackClick = { activity?.finish() }
        )

        // 2. Status Text (Thông báo)
        ArkanoidStatusText(
            gameState = gameState,
            countdown = countdown
        )
    }
}

/**
 * (YC4) Composable con để hiển thị HUD (Điểm, Mạng, Màn)
 */
@Composable
private fun BoxScope.ArkanoidHud( // <-- ĐÃ SỬA LỖI Ở ĐÂY
    score: Int,
    lives: Int,
    wave: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((SCORE_PANEL_HEIGHT / 2).dp)
            .background(Color(0xAA202020))
            .padding(horizontal = 16.dp)
            .align(Alignment.TopCenter), // <-- Dòng này đã hợp lệ
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("QUAY LẠI", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = "Score: $score | Lives: $lives | Wave: $wave",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
        )
    }
}

/**
 * (YC4) Composable con để hiển thị các thông báo trạng thái game
 */
@Composable
private fun BoxScope.ArkanoidStatusText(
    gameState: GameState,
    countdown: Int
) {
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
            GameState.WaveClear -> Text(
                "QUA MÀN!\nSANG MÀN TIẾP TRONG... ${countdown}s",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.Green, fontWeight = FontWeight.Bold),
                lineHeight = 40.sp
            )
            else -> {}
        }
    }
}