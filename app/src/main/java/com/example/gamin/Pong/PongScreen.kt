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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// LƯU Ý: Hằng số PADDLE_HEIGHT và GOAL_ZONE_WIDTH/PADDLE_WIDTH
// được import từ PongLogic.kt, đảm bảo chúng đã được định nghĩa ở đó.

@SuppressLint("ContextCastToActivity")
@Composable
fun PongScreen() {
    val activity = (LocalContext.current as? Activity)

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    // State của game sẽ được khởi tạo lại khi canvas có kích thước
    var state by remember(canvasWidth, canvasHeight) {
        mutableStateOf(initializePongGame(canvasWidth, canvasHeight))
    }

    // --- Game Loop ---
    val lastUpdateTime = rememberUpdatedState(System.nanoTime())
    LaunchedEffect(state.status, canvasWidth, canvasHeight) {
        // CHỈ CHẠY KHI:
        // 1. Đang chơi (Playing)
        // 2. Canvas đã có kích thước (không phải 0)
        if (state.status != PongGameStatus.Playing || canvasHeight == 0f || canvasWidth == 0f) {
            return@LaunchedEffect
        }

        var lastTime = lastUpdateTime.value
        while (state.status == PongGameStatus.Playing) {
            val currentTime = System.nanoTime()
            val dt = (currentTime - lastTime) / 1_000_000_000f // Delta time in seconds
            lastTime = currentTime

            // Cập nhật vật lý và AI (với kích thước canvas chính xác)
            state = updateGamePhysics(state, dt, canvasWidth, canvasHeight)

            delay(16) // ~60 FPS
        }
    }

    // --- Xử lý Input ---
    val inputModifier = Modifier
        .fillMaxSize()
        // Xử lý vuốt (di chuyển thanh trượt)
        .pointerInput(canvasHeight) { // Phụ thuộc vào canvasHeight
            detectDragGestures { change, dragAmount ->
                change.consume()
                // Cập nhật vị trí Y của thanh trượt player
                val newY = (state.playerPaddleY + dragAmount.y)
                    .coerceIn(0f, canvasHeight - PADDLE_HEIGHT)
                state = state.copy(playerPaddleY = newY)
            }
        }
        // Xử lý nhấn (bắt đầu game)
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
            }
        }

    // --- Giao diện (UI) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // THAY ĐỔI: Tô nền đen cho toàn bộ
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
                color = Color.White // Màu trắng cho điểm số
            )
        }

        // THAY ĐỔI: Thêm đường phân cách
        Divider(color = Color.White.copy(alpha = 0.5f), thickness = 1.dp)

        // Khu vực chơi game (Canvas)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth() // Cần fillMaxWidth để onGloballyPositioned đo đúng
                .onGloballyPositioned { coordinates ->
                    canvasHeight = coordinates.size.height.toFloat()
                    canvasWidth = coordinates.size.width.toFloat()
                }
        ) {
            // Chỉ vẽ khi canvas đã có kích thước
            if (canvasHeight > 0f && canvasWidth > 0f) {
                Canvas(modifier = inputModifier.background(Color.Black)) { // Background đen cho Canvas
                    val gap = 20f
                    val dashHeight = 40f
                    val dashWidth = 4f
                    val centerX = size.width / 2 - dashWidth / 2
                    val borderWidth = 5f // Độ dày viền

                    // --- 1. Vẽ Viền Sân và Goal Zones ---
                    // Viền trên
                    drawRect(color = Color.White, topLeft = Offset(0f, 0f), size = Size(size.width, borderWidth))
                    // Viền dưới
                    drawRect(color = Color.White, topLeft = Offset(0f, size.height - borderWidth), size = Size(size.width, borderWidth))

                    // Viền trái (Goal Zone - MÀU ĐỎ)
                    // GOAL_ZONE_WIDTH được định nghĩa trong PongLogic.kt
                    drawRect(color = Color.Red.copy(alpha = 0.5f), topLeft = Offset(0f, 0f), size = Size(GOAL_ZONE_WIDTH, size.height))
                    // Viền phải (Goal Zone - MÀU ĐỎ)
                    drawRect(color = Color.Red.copy(alpha = 0.5f), topLeft = Offset(size.width - GOAL_ZONE_WIDTH, 0f), size = Size(GOAL_ZONE_WIDTH, size.height))


                    // 2. Vẽ đường gạch giữa
                    var currentY = borderWidth // Bắt đầu từ sau viền trên
                    while (currentY < size.height - borderWidth) { // Dừng trước viền dưới
                        drawRect(
                            color = Color.White.copy(alpha = 0.5f),
                            topLeft = Offset(centerX, currentY),
                            size = Size(dashWidth, dashHeight)
                        )
                        currentY += dashHeight + gap
                    }

                    // 3. Vẽ thanh trượt Player (Bắt đầu sau Goal Zone)
                    // PADDLE_WIDTH được định nghĩa trong PongLogic.kt
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(GOAL_ZONE_WIDTH, state.playerPaddleY), // Vị trí X = GOAL_ZONE_WIDTH
                        size = Size(PADDLE_WIDTH, PADDLE_HEIGHT)
                    )

                    // 4. Vẽ thanh trượt AI (Kết thúc trước Goal Zone)
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
            }
        }
    }
}
