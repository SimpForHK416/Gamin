// Đặt trong thư mục: com.example.gamin/FlappyBird/FlappyBirdScreen.kt

package com.example.gamin.FlappyBird

import androidx.compose.animation.core.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- Thông số Game Cố định ---
private const val BIRD_SIZE = 40f
private const val GRAVITY = 1000f // gia tốc trọng trường (pixels/s^2)
private const val JUMP_VELOCITY = -500f // vận tốc khi nhảy (pixels/s)
private const val PIPE_WIDTH = 100f
private const val PIPE_GAP = 200f
private const val PIPE_SPEED = 300f // tốc độ di chuyển của ống (pixels/s)

// --- Trạng thái Game ---
data class BirdState(val y: Float, val velocity: Float)
data class PipeState(val x: Float, val gapY: Float)

@Composable
fun FlappyBirdScreen() {
    var birdState by remember { mutableStateOf(BirdState(y = 500f, velocity = 0f)) }
    var pipes by remember { mutableStateOf(listOf(PipeState(x = 1000f, gapY = 300f))) }
    var score by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var groundOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying && !isGameOver) {
            groundOffset = (groundOffset - PIPE_SPEED * 0.016f) % 100f
            delay(16)
        }
    }

    // --- Game Loop ---
    val lastUpdateTime = rememberUpdatedState(System.nanoTime())
    LaunchedEffect(isPlaying) {
        if (!isPlaying || isGameOver) return@LaunchedEffect

        var lastTime = lastUpdateTime.value
        while (isPlaying && !isGameOver) {
            val currentTime = System.nanoTime()
            val dt = (currentTime - lastTime) / 1_000_000_000f // Delta time in seconds
            lastTime = currentTime

            // 1. Cập nhật vị trí và vận tốc chim
            birdState = birdState.copy(
                velocity = birdState.velocity + GRAVITY * dt,
                y = birdState.y + birdState.velocity * dt
            )

            // 2. Cập nhật vị trí ống
            pipes = pipes.map { pipe ->
                pipe.copy(x = pipe.x - PIPE_SPEED * dt)
            }.filter { it.x > -PIPE_WIDTH }

            // 3. Thêm ống mới
            if (pipes.last().x < 500f) {
                val newGapY = Random.nextInt(200, 800).toFloat()
                pipes = pipes + PipeState(x = 1000f, gapY = newGapY)
            }

            // 4. Kiểm tra va chạm
            val gameHeight = 1000f // Chiều cao giả định
            val birdX = 300f // Chim cố định ở 1/4 màn hình (có thể chỉnh nếu muốn)

            val birdRect = Rect(
                birdX - BIRD_SIZE / 2,
                birdState.y - BIRD_SIZE / 2,
                birdX + BIRD_SIZE / 2,
                birdState.y + BIRD_SIZE / 2
            )

            pipes.forEach { pipe ->
                val topPipeRect = Rect(
                    pipe.x,
                    0f,
                    pipe.x + PIPE_WIDTH,
                    pipe.gapY - PIPE_GAP / 2
                )
                val bottomPipeRect = Rect(
                    pipe.x,
                    pipe.gapY + PIPE_GAP / 2,
                    pipe.x + PIPE_WIDTH,
                    gameHeight
                )

                if (birdRect.overlaps(topPipeRect) || birdRect.overlaps(bottomPipeRect)) {
                    isGameOver = true
                }

                // Tăng điểm khi vượt qua ống
                if (!isGameOver && pipe.x + PIPE_WIDTH < birdX && pipe.x + PIPE_WIDTH >= birdX - PIPE_SPEED * dt) {
                    score++
                }
            }

            // 5. Va chạm trần / sàn
            if (birdState.y < 0 || birdState.y + BIRD_SIZE > gameHeight) {
                isGameOver = true
            }

            delay(16)
        }
    }

    // --- Hành động nhảy ---
    val jump: () -> Unit = {
        if (isPlaying && !isGameOver) {
            birdState = birdState.copy(velocity = JUMP_VELOCITY)
        } else if (!isPlaying && !isGameOver) {
            isPlaying = true
        }
    }

    // --- Giao diện ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isGameOver) {
                detectTapGestures { jump() }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Score và Reset ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Score: $score", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = {
                birdState = BirdState(y = 500f, velocity = 0f)
                pipes = listOf(PipeState(x = 1000f, gapY = 300f))
                score = 0
                isGameOver = false
                isPlaying = false
            }) {
                Text(if (isGameOver) "Chơi Lại" else "Reset")
            }
        }

        // --- Khu vực Game ---
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF7DE6F5))
        ) {
            val gameHeight = size.height
            val birdX = size.width / 4
            val birdY = birdState.y.coerceIn(0f, gameHeight - BIRD_SIZE)

            // --- 1. Vẽ Ống ---
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

                // --- Hitbox ống (màu đỏ trong suốt để debug) ---
                drawRect(
                    color = Color.Red.copy(alpha = 0.3f),
                    topLeft = Offset(pipe.x, 0f),
                    size = Size(PIPE_WIDTH, pipe.gapY - PIPE_GAP / 2)
                )
                drawRect(
                    color = Color.Red.copy(alpha = 0.3f),
                    topLeft = Offset(pipe.x, pipe.gapY + PIPE_GAP / 2),
                    size = Size(PIPE_WIDTH, gameHeight - (pipe.gapY + PIPE_GAP / 2))
                )
            }

            // --- 2. Vẽ Chim ---
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = BIRD_SIZE / 2,
                center = Offset(birdX, birdY)
            )
            drawOval(
                color = Color(0xFFFBC02D),
                topLeft = Offset(birdX - 15, birdY - 10),
                size = Size(30f, 20f)
            )
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = Offset(birdX + 10, birdY - 8)
            )
            drawCircle(
                color = Color.Black,
                radius = 3f,
                center = Offset(birdX + 10, birdY - 8)
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(birdX + 20, birdY)
                    lineTo(birdX + 30, birdY + 5)
                    lineTo(birdX + 20, birdY + 10)
                    close()
                },
                color = Color(0xFFFF9800)
            )

            // --- Hitbox chim (màu đỏ trong suốt để debug) ---
            drawRect(
                color = Color.Red.copy(alpha = 0.3f),
                topLeft = Offset(birdX - BIRD_SIZE / 2, birdY - BIRD_SIZE / 2),
                size = Size(BIRD_SIZE, BIRD_SIZE)
            )

            // --- Game Over Overlay ---
            if (isGameOver) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset.Zero,
                    size = size
                )
            }
        }

        // --- Trạng thái Game Overlay ---
        if (isGameOver) {
            Text(
                "GAME OVER! 💥",
                modifier = Modifier.padding(top = 100.dp).offset(y = (-100).dp),
                style = MaterialTheme.typography.headlineLarge.copy(color = Color.Red)
            )
        } else if (!isPlaying) {
            Text(
                "TAP để BẮT ĐẦU!",
                modifier = Modifier.padding(top = 100.dp).offset(y = (-100).dp),
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
            )
        }

        // --- Đất ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFFD2B48C))
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
