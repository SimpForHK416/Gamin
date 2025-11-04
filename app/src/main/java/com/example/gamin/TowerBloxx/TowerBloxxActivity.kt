package com.example.gamin.TowerBloxx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

data class FallingBlock(
    val rect: Rect,
    var currentY: Float
)

class TowerBloxxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TowerBloxxGame()
            }
        }
    }
}

suspend fun startBlockDrop(
    fallingBlock: FallingBlock,
    startY: Float,
    canvasHeight: Float
) {
    fallingBlock.currentY = startY
    while (fallingBlock.currentY < canvasHeight - fallingBlock.rect.height) {
        fallingBlock.currentY += 20f
        delay(10)
    }
    fallingBlock.currentY = canvasHeight - fallingBlock.rect.height
}

@Composable
fun TowerBloxxGame() {
    var blocks by remember { mutableStateOf(listOf<Rect>()) }
    var currentBlockX by remember { mutableStateOf(0f) }
    var currentBlockY by remember { mutableStateOf(0f) }
    var dropping by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var currentScore by remember { mutableStateOf(0) }
    var missedBlock by remember { mutableStateOf<FallingBlock?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val blockWidth = with(density) { 80.dp.toPx() }
    val blockHeight = with(density) { 40.dp.toPx() }

    val groundHeight = 100f // Thềm bê tông

    var swingAngle by remember { mutableStateOf(0f) }
    val swingAmplitude = with(density) { 100.dp.toPx() }
    val swingFrequency = 0.05f
    var swingCenter by remember { mutableStateOf(0f) }

    val restartGame: () -> Unit = {
        blocks = emptyList()
        currentBlockX = 0f
        currentBlockY = 0f
        dropping = false
        gameOver = false
        currentScore = 0
        missedBlock = null
        swingAngle = 0f
    }

    // Khối lắc lư qua lại
    LaunchedEffect(dropping, gameOver) {
        if (gameOver) return@LaunchedEffect
        while (swingCenter == 0f) delay(100)

        while (!dropping && !gameOver) {
            swingAngle += swingFrequency
            currentBlockX =
                swingCenter + swingAmplitude * sin(swingAngle) - blockWidth / 2f
            delay(16)
        }
    }

    // Logic thả khối
    LaunchedEffect(dropping) {
        if (dropping && !gameOver) {
            val targetY = with(density) { 800.dp.toPx() } - blocks.size * blockHeight - blockHeight - groundHeight
            currentBlockY = 0f

            while (currentBlockY < targetY) {
                currentBlockY += 20f
                if (currentBlockY > targetY) currentBlockY = targetY
                delay(10)
            }

            val previousBlock: Rect? = blocks.lastOrNull()
            val baseX = previousBlock?.left ?: (swingCenter - blockWidth / 2f)
            val baseWidth = previousBlock?.width ?: blockWidth

            val fallingRect =
                Rect(currentBlockX, targetY, currentBlockX + blockWidth, targetY + blockHeight)
            val overlap = baseWidth - abs(currentBlockX - baseX)

            if (overlap > 0) {
                // ✅ Không cắt khối nữa — block giữ nguyên kích thước
                val newBlock = Rect(currentBlockX, targetY, currentBlockX + blockWidth, targetY + blockHeight)
                blocks = blocks + newBlock
                currentScore++
            } else {
                // ❌ Lệch hoàn toàn => game over
                gameOver = true
                missedBlock = FallingBlock(fallingRect, targetY)
            }

            dropping = false
            currentBlockY = 0f
        }
    }

    // Giao diện tổng
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000080))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (!dropping && !gameOver) {
                        dropping = true
                    }
                })
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = size.height
            val canvasWidth = size.width

            if (swingCenter == 0f) swingCenter = canvasWidth / 2f

            val blockFallingY = if (dropping)
                currentBlockY
            else
                canvasHeight - groundHeight - blocks.size * blockHeight - blockHeight

            // 🔩 Vẽ thềm bê tông
            drawRect(
                color = Color(0xFF707070),
                topLeft = Offset(0f, canvasHeight - groundHeight),
                size = Size(canvasWidth, groundHeight)
            )

            // Vẽ dây treo
            if (!gameOver && !dropping) {
                drawLine(
                    color = Color.White,
                    start = Offset(swingCenter, 0f),
                    end = Offset(currentBlockX + blockWidth / 2f, blockFallingY),
                    strokeWidth = 3f
                )
            }

            // 🧱 Vẽ các block đã rơi
            blocks.forEachIndexed { index, block ->
                val topY = canvasHeight - groundHeight - (blocks.size - index) * blockHeight
                drawRect(
                    color = Color(0xFFCD5C5C),
                    topLeft = Offset(block.left, topY),
                    size = Size(block.width, block.height)
                )
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(block.left + block.width / 4, topY + block.height / 4),
                    size = Size(block.width / 2, block.height / 2)
                )
            }

            // 🔸 Khối đang rơi
            if (!gameOver) {
                drawRect(
                    color = Color(0xFFDAA520),
                    topLeft = Offset(currentBlockX, blockFallingY),
                    size = Size(blockWidth, blockHeight)
                )
            }

            // ⚙️ Khối trượt (nếu có)
            missedBlock?.let { falling ->
                if (falling.currentY == 0f) {
                    coroutineScope.launch {
                        startBlockDrop(falling, falling.rect.top, canvasHeight)
                    }
                }
                drawRect(
                    color = Color(0xFF696969),
                    topLeft = Offset(falling.rect.left, falling.currentY),
                    size = Size(falling.rect.width, falling.rect.height)
                )
            }
        }

        // 🧮 Điểm
        Text(
            text = "Điểm: $currentScore",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        )

        // 🚫 Game Over
        if (gameOver) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("GAME OVER!", color = Color.Red, fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Điểm cuối: $currentScore", color = Color.White, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = restartGame) {
                    Text("CHƠI LẠI", fontSize = 24.sp)
                }
            }
        }
    }
}
