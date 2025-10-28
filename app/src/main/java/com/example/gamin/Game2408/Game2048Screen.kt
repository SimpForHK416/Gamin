package com.example.gamin.game2408

import android.app.Activity // THAY ĐỔI: Thêm import
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext // THAY ĐỔI: Thêm import
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.gamin.snake.DirectionButtons
import kotlinx.coroutines.delay

// GIẢ ĐỊNH: Các hàm logic này tồn tại trong project của bạn
// import com.example.gamin.game2408.logic.*

@Composable
fun Game2048Screen() {
    var state by remember { mutableStateOf(initialize2048Game()) }
    val density = LocalDensity.current.density
    val minDragDistance = 50 * density
    var dragAccumulator by remember { mutableStateOf(Offset.Zero) }
    var moveTrigger by remember { mutableStateOf(false) }

    // THAY ĐỔI: Lấy Activity context
    val activity = (LocalContext.current as? Activity)

    // --- LOGIC TRÌ HOÃN ĐỂ THÊM Ô MỚI ---
    LaunchedEffect(moveTrigger) {
        if (moveTrigger) {
            // Đợi animation di chuyển hoàn thành trước khi thêm ô mới
            delay(200L) // Thời gian này nên khớp với animation di chuyển
            state = addNewTile(state)
            moveTrigger = false
        }
    }

    // --- (SỬA LỖI) HÀM XỬ LÝ DI CHUYỂN TRUNG TÂM ---
// --- (SỬA LỖI) HÀM XỬ LÝ DI CHUYỂN TRUNG TÂM ---
    val handleMove = { direction: Direction ->
        // Chỉ thực hiện di chuyển nếu game chưa kết thúc
        if (!state.isGameOver && !state.hasWon) {
            val newState = move(state, direction)
            // Chỉ kích hoạt animation và thêm ô mới nếu bảng game có sự thay đổi
            if (newState != state) {
                state = newState
                moveTrigger = true
            }
        }
    }


    // --- Xử lý vuốt (Swipe) ---
    val swipeHandler = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { dragAccumulator = Offset.Zero },
            onDragEnd = {
                val (x, y) = dragAccumulator
                if (dragAccumulator.getDistance() > minDragDistance) {
                    val direction = when {
                        kotlin.math.abs(x) > kotlin.math.abs(y) && x > 0 -> Direction.RIGHT
                        kotlin.math.abs(x) > kotlin.math.abs(y) && x < 0 -> Direction.LEFT
                        kotlin.math.abs(y) > kotlin.math.abs(x) && y > 0 -> Direction.DOWN
                        kotlin.math.abs(y) > kotlin.math.abs(x) && y < 0 -> Direction.UP
                        else -> null
                    }
                    if (direction != null) {
                        handleMove(direction) // Sử dụng hàm xử lý trung tâm
                    }
                }
                dragAccumulator = Offset.Zero
            },
            onDrag = { change, dragAmount ->
                change.consume()
                dragAccumulator += dragAmount
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("2048", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // --- Điểm số ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Sắp xếp 3 mục
            verticalAlignment = Alignment.CenterVertically
        ) {
            // THAY ĐỔI: Thêm nút Quay lại
            Button(
                onClick = { activity?.finish() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Quay lại")
            }

            Text("Score: ${state.score}", style = MaterialTheme.typography.titleMedium)

            Button(onClick = {
                state = initialize2048Game()
                moveTrigger = false // Reset trigger khi bắt đầu game mới
            }) {
                Text("New Game")
            }
        }
        Spacer(Modifier.height(16.dp))

        // --- Bảng Game (Board) với Animation ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFBBADA0), RoundedCornerShape(8.dp))
                .padding(4.dp)
                .then(swipeHandler)
        ) {
            Game2048AnimatedBoard(state = state)
        }

        Spacer(Modifier.height(24.dp))

        // --- Trạng thái Game Over/Win ---
        if (state.isGameOver) {
            Text("GAME OVER!", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        } else if (state.hasWon) {
            Text("YOU WIN! 🎉", color = Color(0xFF4CAF50), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(16.dp))

        // --- (SỬA LỖI) Nút điều khiển ---
        // Sử dụng hàm handleMove để đảm bảo logic được thực thi đúng
        DirectionButtons(
            onUp = { handleMove(Direction.UP) },
            onDown = { handleMove(Direction.DOWN) },
            onLeft = { handleMove(Direction.LEFT) },
            onRight = { handleMove(Direction.RIGHT) }
        )
    }
}

@Composable
fun Game2048AnimatedBoard(state: Game2048State) {
    val spacing = 4.dp
    val density = LocalDensity.current
    var actualBoxWidth by remember { mutableStateOf(0) }

    val singleCellSize: Dp = remember(actualBoxWidth) {
        if (actualBoxWidth == 0) return@remember 0.dp

        val totalWidthDp = with(density) { actualBoxWidth.toDp() }
        val totalSpacing = spacing * (state.size - 1)

        (totalWidthDp - totalSpacing) / state.size
    }

    val cellAndSpacing = singleCellSize + spacing

    // Vẽ các ô nền trống
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                actualBoxWidth = coordinates.size.width
            }
    ) {
        repeat(state.size) { r ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                repeat(state.size) { c ->
                    Box(
                        modifier = Modifier
                            .size(singleCellSize)
                            .background(tileColors(0).first, RoundedCornerShape(4.dp))
                    )
                }
            }
            if (r < state.size - 1) Spacer(Modifier.height(spacing))
        }
    }

    // Vẽ các ô số (có hoạt ảnh)
    if (actualBoxWidth > 0) {
        state.tiles.forEach { tile ->
            val targetOffsetX = tile.col * cellAndSpacing
            val targetOffsetY = tile.row * cellAndSpacing

            // --- (SỬA LỖI ANIMATION) Chuyển từ `spring` sang `tween` để chuyển động mượt hơn ---
            val animationSpec: AnimationSpec<Dp> = tween(durationMillis = 150, easing = EaseOut)

            val animatedOffsetX by animateDpAsState(
                targetValue = targetOffsetX,
                animationSpec = animationSpec,
                label = "TileX"
            )
            val animatedOffsetY by animateDpAsState(
                targetValue = targetOffsetY,
                animationSpec = animationSpec,
                label = "TileY"
            )

            TileView(
                tile = tile,
                modifier = Modifier
                    .size(singleCellSize)
                    .offset(x = animatedOffsetX, y = animatedOffsetY)
            )
        }
    }
}

@Composable
fun TileView(tile: Tile, modifier: Modifier) {
    val (color, textColor) = tileColors(tile.value)
    val text = if (tile.value == 0) "" else tile.value.toString()

    // Hoạt ảnh xuất hiện của ô mới
    val animatedScale = remember { Animatable(if (tile.isNew) 0f else 1f) }

    LaunchedEffect(tile.id) {
        if (tile.isNew) {
            animatedScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = modifier
            .background(color, RoundedCornerShape(4.dp))
            .graphicsLayer(scaleX = animatedScale.value, scaleY = animatedScale.value),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Hàm cung cấp màu sắc cho các ô số (Giữ nguyên)
fun tileColors(value: Int): Pair<Color, Color> {
    val bg = when (value) {
        0 -> Color(0xFFCDC1B4)
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3E)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        512 -> Color(0xFFEDC850)
        1024 -> Color(0xFFEDC53F)
        2048 -> Color(0xFFEDC22E)
        else -> Color(0xFF3C3A32)
    }
    val text = if (value < 8 && value != 0) Color(0xFF776E65) else Color.White
    return Pair(bg, text)
}

