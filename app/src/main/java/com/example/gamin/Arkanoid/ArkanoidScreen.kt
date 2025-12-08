package com.example.gamin.Arkanoid

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.gamin.Arkanoid.database.AppDatabase
import com.example.gamin.Arkanoid.database.ScoreRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

// --- CÁC HẰNG SỐ CẤU HÌNH TỐC ĐỘ ---
private const val SPEED_MULTIPLIER = 1.2f
private const val MAX_BALL_SPEED = 2500f

private data class ActiveEffect(
    val type: PowerUpType,
    val expirationTime: Long
)

@SuppressLint("ContextCastToActivity", "UnusedBoxWithConstraintsScope")
@Composable
fun ArkanoidScreen() {
    val context = LocalContext.current
    val activity = (context as? Activity)

    var showLevelSelect by remember { mutableStateOf(true) }
    var selectedWave by remember { mutableStateOf(1) }

    if (showLevelSelect) {
        LevelSelectScreen(
            onWaveSelected = { wave ->
                selectedWave = wave
                showLevelSelect = false
            }
        )
    } else {
        ArkanoidGameScreen(
            initialWave = selectedWave,
            onBackToLevelSelect = { showLevelSelect = true }
        )
    }
}

@Composable
private fun LevelSelectScreen(onWaveSelected: (Int) -> Unit) {
    val waves = (1..10).toList()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF001F3F)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "CHỌN MÀN CHƠI",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(waves) { wave ->
                    Button(
                        onClick = { onWaveSelected(wave) },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(vertical = 6.dp)
                    ) {
                        Text("Màn $wave")
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ArkanoidGameScreen(initialWave: Int, onBackToLevelSelect: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gameWidth = constraints.maxWidth.toFloat()
        val gameHeight = constraints.maxHeight.toFloat()
        val context = LocalContext.current // Lấy context

        // KHỞI TẠO DATABASE DAO
        val dbDao = remember { AppDatabase.getDatabase(context).scoreDao() }
        var showLeaderboard by remember { mutableStateOf<Int?>(null) }

        var paddle by remember { mutableStateOf(PaddleState(gameWidth / 2)) }
        var paddleWidth by remember { mutableFloatStateOf(INITIAL_PADDLE_WIDTH) }
        var activeEffects by remember { mutableStateOf(emptySet<ActiveEffect>()) }

        var gameState by remember { mutableStateOf<GameState>(GameState.Ready) }
        var score by remember { mutableIntStateOf(0) }
        var lives by remember { mutableIntStateOf(3) }
        var wave by remember { mutableIntStateOf(initialWave) }
        var countdown by remember { mutableIntStateOf(3) }

        var timeLeftSeconds by remember { mutableIntStateOf(180) }
        var _timeAccumulator by remember { mutableStateOf(0f) }


        // BỎ derivedStateOf timeSpent = remember { derivedStateOf { 180 - timeLeftSeconds } }

        var balls by remember {
            mutableStateOf(listOf(createInitialBall(gameWidth, gameHeight, INITIAL_BALL_SPEED).copy(velocityX = 0f, velocityY = 0f)))
        }

        var bricks by remember {
            mutableStateOf(mutableListOf<BrickState>().apply {
                addAll(createBrickPattern(gameWidth, wave))
                val normalIndices = this.indices.filter { idx -> this[idx].type == BrickType.NORMAL }
                val starCount = minOf(3, maxOf(2, (2 + Random.nextInt(2))))
                normalIndices.shuffled().take(starCount).forEach { idx ->
                    val b = this[idx]
                    this[idx] = b.copy(hasStar = true)
                }
            })
        }

        var powerUps by remember { mutableStateOf(emptyList<PowerUpItem>()) }
        var starsCollected by remember { mutableIntStateOf(0) }
        val REQUIRED_STARS = 2

        // Hàm reset vị trí bóng và thanh trượt (chạy khi mất 1 mạng)
        val resetBallAndPaddle = {
            paddleWidth = INITIAL_PADDLE_WIDTH
            activeEffects = emptySet()
            val speed = INITIAL_BALL_SPEED
            balls = listOf(createInitialBall(gameWidth, gameHeight, speed).copy(velocityX = 0f, velocityY = 0f))
            powerUps = emptyList()
            gameState = GameState.Ready
            // Không reset starsCollected ở đây để giữ lại số sao đã ăn trong lượt trước
            timeLeftSeconds = 180
            _timeAccumulator = 0f
        }

        // Hàm tạo lại toàn bộ màn chơi (chạy khi qua màn hoặc GAME OVER)
        val setupBricksForWave: (Int) -> Unit = { w ->
            val list = createBrickPattern(gameWidth, w).toMutableList()
            val normalIndices = list.indices.filter { idx -> list[idx].type == BrickType.NORMAL }
            val starCount = minOf(3, maxOf(2, (2 + Random.nextInt(2))))
            normalIndices.shuffled().take(starCount).forEach { idx ->
                val b = list[idx]
                list[idx] = b.copy(hasStar = true)
            }
            bricks = list
            starsCollected = 0
            timeLeftSeconds = 180
            _timeAccumulator = 0f
        }

        val nextWave = {
            wave++
            setupBricksForWave(wave)
            resetBallAndPaddle()
        }

        val restartLevel = {
            score = 0
            lives = 3
            wave = initialWave
            setupBricksForWave(wave)
            resetBallAndPaddle()
        }

        // HÀM LƯU ĐIỂM (Bỏ Time)
        val saveCurrentWaveScore: () -> Unit = {
            if (score > 0) {
                val waveIndex = wave.coerceIn(1, 10)
                var newRecord = ScoreRecord()

                newRecord = when(waveIndex) {
                    1 -> newRecord.copy(wave1Score = score)
                    2 -> newRecord.copy(wave2Score = score)
                    3 -> newRecord.copy(wave3Score = score)
                    4 -> newRecord.copy(wave4Score = score)
                    5 -> newRecord.copy(wave5Score = score)
                    6 -> newRecord.copy(wave6Score = score)
                    7 -> newRecord.copy(wave7Score = score)
                    8 -> newRecord.copy(wave8Score = score)
                    9 -> newRecord.copy(wave9Score = score)
                    10 -> newRecord.copy(wave10Score = score)
                    else -> newRecord
                }

                CoroutineScope(Dispatchers.IO).launch {
                    dbDao.insertScore(newRecord)
                }
            }
        }

        fun formatTime(s: Int): String {
            val mm = s / 60
            val ss = s % 60
            return String.format("%02d:%02d", mm, ss)
        }

        // Hàm tiện ích để tăng tốc độ vector
        fun boostVelocity(vx: Float, vy: Float): Pair<Float, Float> {
            val currentSpeed = sqrt(vx * vx + vy * vy)
            if (currentSpeed < MAX_BALL_SPEED) {
                return Pair(vx * SPEED_MULTIPLIER, vy * SPEED_MULTIPLIER)
            }
            return Pair(vx, vy)
        }

        LaunchedEffect(gameState) {
            if (gameState != GameState.Playing) return@LaunchedEffect
            var lastTime = System.nanoTime()

            while (gameState == GameState.Playing) {
                val now = System.nanoTime()
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                _timeAccumulator += dt
                if (_timeAccumulator >= 1f) {
                    val dec = floor(_timeAccumulator).toInt()
                    timeLeftSeconds = (timeLeftSeconds - dec).coerceAtLeast(0)
                    _timeAccumulator -= dec.toFloat()
                }
                if (timeLeftSeconds <= 0) {
                    saveCurrentWaveScore() // LƯU ĐIỂM KHI HẾT GIỜ
                    gameState = GameState.GameOver
                    break
                }

                val currentTime = System.currentTimeMillis()
                val (expired, active) = activeEffects.partition { it.expirationTime < currentTime }
                if (expired.isNotEmpty()) {
                    activeEffects = active.toSet()
                    val hadPaddleGrow = expired.any { it.type == PowerUpType.PADDLE_GROW }
                    if (hadPaddleGrow && active.none { it.type == PowerUpType.PADDLE_GROW }) {
                        paddleWidth = INITIAL_PADDLE_WIDTH
                    }
                }

                var newBalls = balls.toMutableList()
                val ballsToRemove = mutableListOf<BallState>()

                for (i in newBalls.indices) {
                    val ball = newBalls[i]
                    var newX = ball.x + ball.velocityX * dt
                    var newY = ball.y + ball.velocityY * dt
                    var vx = ball.velocityX
                    var vy = ball.velocityY

                    // === XỬ LÝ CHẠM TƯỜNG (CÓ TĂNG TỐC) ===
                    if (newX - BALL_SIZE / 2 < 0) {
                        vx = -vx
                        newX = BALL_SIZE / 2
                        val boosted = boostVelocity(vx, vy)
                        vx = boosted.first
                        vy = boosted.second
                    }
                    if (newX + BALL_SIZE / 2 > gameWidth) {
                        vx = -vx
                        newX = gameWidth - BALL_SIZE / 2
                        val boosted = boostVelocity(vx, vy)
                        vx = boosted.first
                        vy = boosted.second
                    }
                    if (newY - BALL_SIZE / 2 < SCORE_PANEL_HEIGHT) {
                        vy = -vy
                        newY = SCORE_PANEL_HEIGHT + BALL_SIZE / 2
                        val boosted = boostVelocity(vx, vy)
                        vx = boosted.first
                        vy = boosted.second
                    }
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

                    // === XỬ LÝ CHẠM THANH TRƯỢT (CÓ TĂNG TỐC) ===
                    if (ballRect.overlaps(paddleRect) && vy > 0) {
                        vy *= -1
                        newY = paddleRect.top - BALL_SIZE / 2 - 1
                        val hitPoint = newX - paddle.x
                        val normalized = (hitPoint / (paddleWidth / 2)).coerceIn(-1f, 1f)
                        val maxAngle = Math.toRadians(80.0)
                        val newAngle = maxAngle * normalized

                        val currentSpeed = sqrt(vx * vx + vy * vy)
                        // Tăng tốc 20%, giới hạn ở MAX_BALL_SPEED
                        val newSpeed = (currentSpeed * SPEED_MULTIPLIER).coerceAtMost(MAX_BALL_SPEED)

                        vx = (newSpeed * sin(newAngle)).toFloat()
                        vy = -(newSpeed * cos(newAngle)).toFloat()
                    }
                    newBalls[i] = ball.copy(x = newX, y = newY, velocityX = vx, velocityY = vy)
                }
                newBalls.removeAll(ballsToRemove)

                if (newBalls.isEmpty() && balls.isNotEmpty()) {
                    lives--
                    if (lives <= 0) {
                        saveCurrentWaveScore() // LƯU ĐIỂM KHI GAME OVER
                        gameState = GameState.GameOver
                    } else {
                        resetBallAndPaddle()
                    }
                    continue
                }

                var newBricks = bricks.toMutableList()
                val bricksToDestroy = mutableSetOf<Int>()
                val newPowerUps = mutableListOf<PowerUpItem>()

                newBricks = newBricks.map { b ->
                    if (b.type == BrickType.BOSS && b.isFlashing) b.copy(isFlashing = false) else b
                }.toMutableList()

                for (ballIndex in newBalls.indices) {
                    val ball = newBalls[ballIndex]
                    val ballRect = Rect(ball.x - BALL_SIZE / 2, ball.y - BALL_SIZE / 2, ball.x + BALL_SIZE / 2, ball.y + BALL_SIZE / 2)
                    var ballHitInThisFrame = false

                    for (brickIndex in newBricks.indices) {
                        if (ballHitInThisFrame) break
                        val brick = newBricks[brickIndex]
                        if (brick.isDestroyed || bricksToDestroy.contains(brickIndex)) continue

                        // === XỬ LÝ CHẠM GẠCH (CÓ TĂNG TỐC) ===
                        if (ballRect.overlaps(brick.rect)) {
                            ballHitInThisFrame = true
                            bricksToDestroy.add(brickIndex)
                            score += BRICK_SCORE * wave

                            var currentVx = ball.velocityX
                            var currentVy = -ball.velocityY // Đảo chiều Y cơ bản

                            // Tăng tốc khi chạm gạch
                            val boosted = boostVelocity(currentVx, currentVy)
                            currentVx = boosted.first
                            currentVy = boosted.second

                            newBalls[ballIndex] = ball.copy(velocityX = currentVx, velocityY = currentVy)

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

                            if (brick.type == BrickType.BOSS) {
                                if (brick.hitPoints > 1) {
                                    newBricks[brickIndex] = brick.copy(
                                        hitPoints = brick.hitPoints - 1,
                                        isFlashing = true
                                    )
                                    bricksToDestroy.remove(brickIndex)
                                } else {
                                    newBricks[brickIndex] = brick.copy(isDestroyed = true, isFlashing = false)
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
                    val newlyDestroyedStarCount = bricksToDestroy.count { idx ->
                        idx in bricks.indices && bricks[idx].hasStar && !bricks[idx].isDestroyed
                    }
                    if (newlyDestroyedStarCount > 0) {
                        starsCollected += newlyDestroyedStarCount
                    }

                    newBricks = newBricks.mapIndexed { index, brick ->
                        if (bricksToDestroy.contains(index)) brick.copy(isDestroyed = true) else brick
                    }.toMutableList()
                }

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

                if (bricks.all { it.isDestroyed }) {
                    saveCurrentWaveScore() // LƯU ĐIỂM KHI QUA MÀN
                    gameState = GameState.WaveClear
                    break
                }

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
                                timeLeftSeconds = 180
                                _timeAccumulator = 0f
                            }
                            GameState.GameOver -> {
                                lives = 3
                                score = 0
                                setupBricksForWave(wave)
                                resetBallAndPaddle()
                            }
                            else -> {}
                        }
                    }
                }
        ) {
            bricks.filter { !it.isDestroyed }.forEach { brick ->
                val brickColor = if (brick.type == BrickType.BOSS && brick.isFlashing) {
                    Color.Yellow
                } else if (brick.type == BrickType.BOSS) {
                    Color(0xFFFFA500)
                } else {
                    brick.color
                }

                drawRoundRect(
                    color = brickColor,
                    topLeft = brick.rect.topLeft,
                    size = brick.rect.size,
                    cornerRadius = CornerRadius(8f, 8f)
                )

                if (brick.type == BrickType.EXPLOSIVE) {
                    drawCircle(Color.Red, radius = 10f, center = brick.rect.center)
                }

                if (brick.hasStar) {
                    drawCircle(Color.Yellow, radius = 12f, center = brick.rect.center)
                }
            }

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    paddle.x - paddleWidth / 2,
                    gameHeight - PADDLE_Y_OFFSET - INITIAL_PADDLE_HEIGHT - TOP_PADDING_OFFSET
                ),
                size = Size(paddleWidth, INITIAL_PADDLE_HEIGHT),
                cornerRadius = CornerRadius(10f, 10f)
            )

            balls.forEach { ball ->
                drawCircle(Color.Yellow, BALL_SIZE / 2, Offset(ball.x, ball.y))
            }

            powerUps.forEach { item ->
                val color = when (item.type) {
                    PowerUpType.MULTI_BALL -> Color.Green
                    PowerUpType.PADDLE_GROW -> Color.Cyan
                }
                drawRect(color, topLeft = Offset(item.x - POWER_UP_SIZE / 2, item.y - POWER_UP_SIZE / 2), size = Size(POWER_UP_SIZE, POWER_UP_SIZE))
            }
        }

        val onShowLeaderboardClick: () -> Unit = {
            showLeaderboard = wave
            gameState = GameState.Paused // Tạm dừng game khi xem bảng điểm
        }

        ArkanoidHud(
            score = score,
            lives = lives,
            wave = wave,
            stars = starsCollected,
            timeText = formatTime(timeLeftSeconds),
            onBackClick = onBackToLevelSelect,
            onShowLeaderboardClick = onShowLeaderboardClick
        )

        ArkanoidStatusText(
            gameState = gameState,
            countdown = countdown
        )

        // LOGIC KHI KẾT THÚC MÀN CHƠI (WaveClear)
        if (gameState == GameState.WaveClear) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Kết thúc màn $wave", fontWeight = FontWeight.Bold) },
                text = { Text("Bạn thu thập $starsCollected ngôi sao trong màn này.\nĐiểm: $score") },

                // SỬ DỤNG confirmButton để chứa tất cả các nút
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                // GỌI HÀM QUAY LẠI CHỌN MÀN
                                onBackToLevelSelect()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("QUAY LẠI CHỌN MÀN")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            // Nút TIẾP TỤC (Tương đương confirmButton cũ)
                            Button(
                                onClick = {
                                    nextWave()
                                    gameState = GameState.Ready
                                },
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Text("TIẾP TỤC")
                            }

                            Button(
                                onClick = {
                                    showLeaderboard = wave
                                    gameState = GameState.Paused
                                },
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            ) {
                                Text("TOP SCORE")
                            }
                        }
                    }
                },
                dismissButton = {}
            )
        }

        // HIỂN THỊ MÀN HÌNH BẢNG ĐIỂM (Leaderboard)
        if (showLeaderboard != null) {
            LeaderboardScreen(
                wave = showLeaderboard!!,
                dbDao = dbDao,
                onDismiss = {
                    showLeaderboard = null
                    if (gameState == GameState.Paused) gameState = GameState.Ready
                }
            )
        }
    }
}

@Composable
private fun BoxScope.ArkanoidHud(
    score: Int,
    lives: Int,
    wave: Int,
    stars: Int,
    timeText: String,
    onBackClick: () -> Unit,
    onShowLeaderboardClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((SCORE_PANEL_HEIGHT / 2).dp)
            .background(Color(0xAA202020))
            .padding(horizontal = 16.dp)
            .align(Alignment.TopCenter),
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
        Button(
            onClick = onShowLeaderboardClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("TOP SCORE", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = "Score: $score | Lives: $lives | Wave: $wave | ⭐ $stars | ⏱ $timeText",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
        )
    }
}

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
                "QUA MÀN! ĐANG HIỂN THỊ KẾT QUẢ",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.Green, fontWeight = FontWeight.Bold),
                lineHeight = 40.sp
            )
            else -> {}
        }
    }
}