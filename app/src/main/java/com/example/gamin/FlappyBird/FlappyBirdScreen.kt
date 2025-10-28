package com.example.gamin.FlappyBird

import android.annotation.SuppressLint
import android.app.Activity // THAY ĐỔI: Thêm import Activity
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
// THAY ĐỔI: Thêm import
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
// THAY ĐỔI: Thêm import
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
// THAY ĐỔI: Thêm import
import androidx.compose.ui.unit.dp
import com.example.gamin.R // THAY ĐỔI: Thêm import R (quan trọng)
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- Thông số Game Cố định ---
private const val BIRD_SIZE = 40f
private const val GRAVITY = 1000f // gia tốc trọng trường (pixels/s^2)
private const val JUMP_VELOCITY = -500f // vận tốc khi nhảy (pixels/s)
private const val PIPE_WIDTH = 100f
private const val PIPE_GAP = 200f
private const val PIPE_SPEED = 300f // tốc độ di chuyển của ống (pixels/s)

// --- Trạng thái Game (State Machine) ---
sealed class GameState {
    object Ready : GameState()    // Màn hình "Tap to Start"
    object Playing : GameState()  // Đang chơi
    object Crashing : GameState() // Đang rơi sau va chạm
    object GameOver : GameState() // Đã chạm đất, hiển thị menu
}

// --- Data Classes ---
data class BirdState(
    val y: Float,
    val velocity: Float,
    val rotation: Float = 0f // Thêm góc xoay
)

data class PipeState(
    val x: Float,
    val gapY: Float,
    var scored: Boolean = false // Thêm cờ để kiểm tra đã tính điểm chưa
)

// THÊM DÒNG NÀY ĐỂ TẮT CẢNH BÁO
@SuppressLint("UnusedBoxWithConstraintsScope", "ContextCastToActivity")
@Suppress("BoxWithConstraintsScopeIsNotUsed")
@Composable
fun FlappyBirdScreen() {
    var birdState by remember { mutableStateOf(BirdState(y = 500f, velocity = 0f)) }
    var pipes by remember { mutableStateOf(emptyList<PipeState>()) }
    var score by remember { mutableIntStateOf(0) }
    var gameState by remember { mutableStateOf<GameState>(GameState.Ready) }
    var groundOffset by remember { mutableFloatStateOf(0f) }

    // THAY ĐỔI: Chỉ tải ảnh chim
    val context = LocalContext.current
    val birdBitmap = remember {
        ImageBitmap.imageResource(context.resources, R.drawable.ic_bird) // Sử dụng tên ic_bird
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gameHeight = constraints.maxHeight.toFloat()
        val gameWidth = constraints.maxWidth.toFloat()
        val birdX = gameWidth / 4 // Vị trí X cố định của chim

        // --- Hàm Reset Game ---
        val resetGame: () -> Unit = {
            birdState = BirdState(y = gameHeight / 2, velocity = 0f, rotation = 0f)
            pipes = listOf(PipeState(x = gameWidth * 1.5f, gapY = gameHeight / 2))
            score = 0
            gameState = GameState.Ready
        }

        // Khởi tạo ống khi game sẵn sàng (nếu chưa có)
        LaunchedEffect(gameState, gameWidth, gameHeight) {
            if (gameState == GameState.Ready && pipes.isEmpty()) {
                pipes = listOf(PipeState(x = gameWidth * 1.5f, gapY = gameHeight / 2))
            }
        }

        // --- Xử lý Input (Nhấn) ---
        val tapAction: () -> Unit = {
            when (gameState) {
                GameState.Ready -> {
                    gameState = GameState.Playing // Bắt đầu chơi
                    birdState = birdState.copy(velocity = JUMP_VELOCITY) // Nhảy lần đầu
                }
                GameState.Playing -> {
                    birdState = birdState.copy(velocity = JUMP_VELOCITY) // Nhảy
                }
                GameState.GameOver -> {
                    resetGame() // Chơi lại
                }
                GameState.Crashing -> { /* Không làm gì khi đang rơi */ }
            }
        }

        // --- Game Loop ---
        val lastUpdateTime = rememberUpdatedState(System.nanoTime())
        LaunchedEffect(gameState, gameHeight, gameWidth) {
            if (gameState != GameState.Playing && gameState != GameState.Crashing) {
                return@LaunchedEffect
            }
            var lastTime = lastUpdateTime.value
            while (gameState == GameState.Playing || gameState == GameState.Crashing) {
                val currentTime = System.nanoTime()
                val dt = (currentTime - lastTime) / 1_000_000_000f // Delta time in seconds
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

        // --- Giao diện (Layered UI) ---
        val fullModifier = Modifier
            .fillMaxSize()
            .pointerInput(gameState) {
                detectTapGestures { tapAction() }
            }

        // --- 1. Khu vực Game (Canvas) ---
        Canvas(
            modifier = fullModifier
                .background(Color(0xFF7DE6F5))
        ) {
            val birdY = birdState.y.coerceIn(0f, gameHeight - BIRD_SIZE)

            // --- 1.1. Vẽ Ống (ĐÃ ĐƯA LẠI VỀ CODE VẼ) ---
            pipes.forEach { pipe ->
                val pipeColor = Color(0xFF4CAF50)
                val pipeShadow = Color(0xFF2E7D32)

                // Ống trên
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
                // Ống dưới
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

            // --- 1.2. Vẽ Chim (VỚI XOAY, DÙNG ẢNH) ---
            withTransform({
                rotate(degrees = birdState.rotation, pivot = Offset(birdX, birdY))
            }) {
                // Vẽ ảnh chim
                drawImage(
                    image = birdBitmap,
                    dstOffset = IntOffset( // Dịch chuyển về top-left để (birdX, birdY) là tâm
                        (birdX - BIRD_SIZE / 2).toInt(),
                        (birdY - BIRD_SIZE / 2).toInt()
                    ),
                    dstSize = IntSize(BIRD_SIZE.toInt(), BIRD_SIZE.toInt())
                )
            }

            // --- 1.3. Game Over Overlay (Mờ) ---
            if (gameState == GameState.Crashing || gameState == GameState.GameOver) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset.Zero,
                    size = size
                )
            }
        }

        // --- 2. Score và Nút (Nằm trên cùng) ---
        // THAY ĐỔI: Lấy Activity context
        val activity = (LocalContext.current as? Activity)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val buttonModifier = Modifier.width(120.dp) // Đặt chiều rộng cố định cho nút

            // THAY ĐỔI: Nút Quay lại Menu
            Button(
                onClick = { activity?.finish() }, // Đóng Activity hiện tại
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = buttonModifier
            ) {
                Text("Quay lại")
            }

            // Giữ Score ở giữa
            Text("Score: $score", style = MaterialTheme.typography.headlineMedium.copy(color = Color.White))

            // THAY ĐỔI: Nút Chơi lại (đặt trong Box để giữ cân bằng)
            Box(modifier = buttonModifier) { // Box giữ chỗ
                if (gameState == GameState.GameOver) {
                    Button(
                        onClick = tapAction,
                        modifier = Modifier.fillMaxWidth() // Làm nút đầy Box
                    ) {
                        Text("Chơi Lại")
                    }
                }
            }
        }

        // --- 3. Trạng thái Game Overlay (Nằm giữa) ---
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


        // --- 4. Đất (Nằm dưới cùng) ---
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

