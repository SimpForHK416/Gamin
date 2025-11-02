package com.example.gamin.Pong

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@SuppressLint("ContextCastToActivity")
@Composable
// <-- ĐÃ SỬA: Nhận `gameMode` từ Activity -->
fun PongScreen(gameMode: GameMode) {
    val activity = (LocalContext.current as? Activity)

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    var state by remember(canvasWidth, canvasHeight) {
        // <-- ĐÃ SỬA: Truyền `gameMode` vào hàm khởi tạo -->
        mutableStateOf(initializePongGame(canvasWidth, canvasHeight, gameMode))
    }

    // --- Game Loop ---
    val lastUpdateTime = rememberUpdatedState(System.nanoTime())
    LaunchedEffect(state.status, canvasWidth, canvasHeight) {
        if (state.status != PongGameStatus.Playing || canvasHeight == 0f || canvasWidth == 0f) {
            return@LaunchedEffect
        }

        var lastTime = lastUpdateTime.value
        while (state.status == PongGameStatus.Playing) {
            val currentTime = System.nanoTime()
            val dt = (currentTime - lastTime) / 1_000_000_000f
            lastTime = currentTime

            state = updateGamePhysics(state, dt, canvasWidth, canvasHeight)

            delay(16) // ~60 FPS
        }
    }

    // --- Xử lý Input (ĐÃ CẬP NHẬT CHO PVP) ---
    val inputModifier = Modifier
        .fillMaxSize()
        // Xử lý vuốt (di chuyển thanh trượt)
        .pointerInput(canvasWidth, canvasHeight, state.gameMode) { // Phụ thuộc vào gameMode
            detectDragGestures { change, dragAmount ->
                change.consume()

                // <-- YÊU CẦU: Xử lý PVE/PVP -->
                if (state.gameMode == GameMode.PVE) {
                    // PVE: Chỉ di chuyển vợt trái
                    val newY = (state.playerPaddleY + dragAmount.y)
                        .coerceIn(0f, canvasHeight - PADDLE_HEIGHT)
                    state = state.copy(playerPaddleY = newY)
                } else {
                    // PVP: Kiểm tra vị trí chạm để di chuyển 2 vợt
                    if (change.position.x < canvasWidth / 2) {
                        // Vợt trái (Player 1)
                        val newY = (state.playerPaddleY + dragAmount.y)
                            .coerceIn(0f, canvasHeight - PADDLE_HEIGHT)
                        state = state.copy(playerPaddleY = newY)
                    } else {
                        // Vợt phải (Player 2)
                        val newY = (state.aiPaddleY + dragAmount.y) // <-- Điều khiển vợt AI
                            .coerceIn(0f, canvasHeight - PADDLE_HEIGHT)
                        state = state.copy(aiPaddleY = newY) // <-- Cập nhật vợt AI
                    }
                }
            }
        }
        // Xử lý nhấn (bắt đầu game / chơi lại)
        .pointerInput(state.status) {
            detectTapGestures {
                if (state.status == PongGameStatus.Ready) {
                    state = state.copy(
                        status = PongGameStatus.Playing,
                        ballVelocity = randomStartVelocity(
                            goingLeft = Random.nextBoolean()
                        )
                    )
                }

                // <-- YÊU CẦU: Chơi lại khi game kết thúc -->
                if (state.status == PongGameStatus.PlayerWins || state.status == PongGameStatus.AIWins) {
                    state = initializePongGame(canvasWidth, canvasHeight, state.gameMode)
                }
            }
        }

    // --- Giao diện (UI) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header (Nút quay lại và Điểm số)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { activity?.finish() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Quay lại")
            }
            Text(
                "${state.playerScore} - ${state.aiScore}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Divider(color = Color.White.copy(alpha = 0.5f), thickness = 1.dp)

        // Khu vực chơi game (Canvas)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    canvasHeight = coordinates.size.height.toFloat()
                    canvasWidth = coordinates.size.width.toFloat()
                }
        ) {
            if (canvasHeight > 0f && canvasWidth > 0f) {
                Canvas(modifier = inputModifier.background(Color.Black)) {
                    // (Code vẽ sân, vạch, vợt, bóng... giữ nguyên)

                    val gap = 20f
                    val dashHeight = 40f
                    val dashWidth = 4f
                    val centerX = size.width / 2 - dashWidth / 2
                    val borderWidth = 5f

                    // 1. Vẽ Viền Sân và Goal Zones
                    drawRect(color = Color.White, topLeft = Offset(0f, 0f), size = Size(size.width, borderWidth))
                    drawRect(color = Color.White, topLeft = Offset(0f, size.height - borderWidth), size = Size(size.width, borderWidth))
                    drawRect(color = Color.Red.copy(alpha = 0.5f), topLeft = Offset(0f, 0f), size = Size(GOAL_ZONE_WIDTH, size.height))
                    drawRect(color = Color.Red.copy(alpha = 0.5f), topLeft = Offset(size.width - GOAL_ZONE_WIDTH, 0f), size = Size(GOAL_ZONE_WIDTH, size.height))

                    // 2. Vẽ đường gạch giữa
                    var currentY = borderWidth
                    while (currentY < size.height - borderWidth) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.5f),
                            topLeft = Offset(centerX, currentY),
                            size = Size(dashWidth, dashHeight)
                        )
                        currentY += dashHeight + gap
                    }

                    // 3. Vẽ thanh trượt Player
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(GOAL_ZONE_WIDTH, state.playerPaddleY),
                        size = Size(PADDLE_WIDTH, PADDLE_HEIGHT)
                    )

                    // 4. Vẽ thanh trượt AI / Player 2
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(size.width - GOAL_ZONE_WIDTH - PADDLE_WIDTH, state.aiPaddleY),
                        size = Size(PADDLE_WIDTH, PADDLE_HEIGHT)
                    )

                    // 5. Vẽ bóng
                    drawCircle(
                        color = Color.White,
                        radius = BALL_RADIUS,
                        center = state.ballPosition
                    )
                }

                // Thông báo "Tap to Start"
                if (state.status == PongGameStatus.Ready) {
                    Text(
                        "TAP ĐỂ BẮT ĐẦU",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }

                // <-- YÊU CẦU: Hiển thị thông báo Thắng/Thua -->
                val (winText, winColor) = when (state.status) {
                    PongGameStatus.PlayerWins -> {
                        val player = if (state.gameMode == GameMode.PVE) "BẠN" else "PLAYER 1"
                        "$player THẮNG!\n(Tap để chơi lại)" to Color.Green
                    }
                    PongGameStatus.AIWins -> {
                        val opponent = if (state.gameMode == GameMode.PVE) "MÁY" else "PLAYER 2"
                        "$opponent THẮNG!\n(Tap để chơi lại)" to Color.Red
                    }
                    else -> "" to Color.Transparent
                }

                if (state.status == PongGameStatus.PlayerWins || state.status == PongGameStatus.AIWins) {
                    Text(
                        text = winText,
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 24.sp,
                        color = winColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}