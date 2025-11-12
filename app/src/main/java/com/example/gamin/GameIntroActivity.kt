package com.example.gamin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
// =============================================
// THÊM IMPORT CHO ARKANOID
import com.example.gamin.Arkanoid.ArkanoidActivity
// =============================================
import com.example.gamin.BubbleShooter.BubbleShooterActivity
import com.example.gamin.FlappyBird.FlappyBirdActivity
import com.example.gamin.MemoryCard.MemoryCardActivity
import com.example.gamin.MineSweeper.MinesweeperActivity
import com.example.gamin.Pong.PongActivity
import com.example.gamin.WhackAMole.WhackAMoleActivity
import com.example.gamin.game2408.Game2408Activity
import com.example.gamin.snake.SnakeActivity
import com.example.gamin.tetris.TetrisActivity
import com.example.gamin.ui.theme.GaminTheme

class GameIntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title") ?: ""
        val rules = intent.getStringExtra("rules") ?: ""
        val image = intent.getIntExtra("image", 0)
        val targetClass = intent.getSerializableExtra("targetClass") as? Class<*>

        Log.d("DEBUG", "Target class = $targetClass")
        Log.d("DEBUG", "Title = $title")

        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameIntroScreen(
                        title = title,
                        rules = rules,
                        imageRes = image,
                        targetClass = targetClass
                    )
                }
            }
        }
    }
}

@Composable
fun GameIntroScreen(
    title: String,
    rules: String,
    imageRes: Int,
    targetClass: Class<*>?
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // TẠO BIẾN KIỂM TRA LOẠI GAME
    val isSnakeGame = targetClass == SnakeActivity::class.java
    val isMinesweeperGame = targetClass == MinesweeperActivity::class.java
    val isFlappyBirdGame = targetClass == FlappyBirdActivity::class.java
    val isMemoryCardGame = targetClass == MemoryCardActivity::class.java
    val is2048Game = targetClass == Game2408Activity::class.java
    val isPongGame = targetClass == PongActivity::class.java
    val isTetrisGame = targetClass == TetrisActivity::class.java
    val isBubbleShooterGame = targetClass == BubbleShooterActivity::class.java
    val isWhackAMoleGame = targetClass == WhackAMoleActivity::class.java
    // =============================================
    // THÊM BIẾN KIỂM TRA CHO ARKANOID
    val isArkanoidGame = targetClass == ArkanoidActivity::class.java
    // =============================================

    // Danh sách game chơi đơn
    val isSinglePlayerGame = isSnakeGame || isMinesweeperGame || is2048Game ||
            isFlappyBirdGame || isMemoryCardGame || isBubbleShooterGame ||
            isWhackAMoleGame || isArkanoidGame // <-- THÊM ARKANOID VÀO ĐÂY

    // Game nhiều người "cũ" (X/O)
    val isMultiplayerGame = !isSinglePlayerGame && !isTetrisGame && !isPongGame


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageRes != 0) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = rules, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // --- Logic Hiển thị Nút Chơi Game ---

        // Logic cho Whack-a-Mole
        if (isWhackAMoleGame) {
            Text("Chọn độ khó:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            val difficulties = listOf("Dễ", "Trung bình", "Khó")
            difficulties.forEach { difficulty ->
                Button(
                    onClick = {
                        if (targetClass != null) {
                            val intent = Intent(context, targetClass)
                            intent.putExtra("difficulty", difficulty)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text(difficulty) }
            }
        }

        // Logic Tetris
        else if (isTetrisGame) {
            Text("Chọn độ khó:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            val difficulties = listOf("Dễ", "Trung bình", "Khó")
            difficulties.forEach { difficulty ->
                Button(
                    onClick = {
                        if (targetClass != null) {
                            val intent = Intent(context, targetClass)
                            intent.putExtra("difficulty", difficulty)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text(difficulty) }
            }
        }

        // Logic game 1 người chơi khác (BAO GỒM CẢ ARKANOID)
        else if (isSinglePlayerGame) {
            Button(
                onClick = {
                    if (targetClass != null) {
                        val intent = Intent(context, targetClass)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                // =============================================
                // THÊM TEXT CHO NÚT ARKANOID
                // =============================================
                val buttonText = when {
                    isMemoryCardGame -> "Chơi ngay 🃏"
                    isFlappyBirdGame -> "Chơi ngay 🐦"
                    is2048Game -> "Chơi ngay 🔢"
                    isMinesweeperGame -> "Chơi ngay 💣"
                    isBubbleShooterGame -> "Chơi ngay 🎯"
                    isWhackAMoleGame -> "Chơi ngay 🐭"
                    isArkanoidGame -> "Chơi ngay 🧱" // <-- THÊM TEXT MỚI
                    else -> "Chơi ngay 🐍"
                }
                Text(text = buttonText)
            }
        }

        // Logic game nhiều người (Pong, NoughtsAndCrosses)
        else if (isMultiplayerGame || isPongGame) {
            Button(
                onClick = {
                    if (targetClass != null) {
                        val intent = Intent(context, targetClass)
                        intent.putExtra("mode", "PVE")
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Chơi với máy (PVE)")
            }

            Button(
                onClick = {
                    if (targetClass != null) {
                        val intent = Intent(context, targetClass)
                        intent.putExtra("mode", "PVP")
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chơi với người (PVP)")
            }
        }
        // --- Kết thúc Logic Nút Chơi Game ---

        if (targetClass == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Lỗi: Không thể khởi động game",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Nút Quay lại Menu Chính
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = {
                activity?.finish()
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Quay lại Menu")
        }
    }
}