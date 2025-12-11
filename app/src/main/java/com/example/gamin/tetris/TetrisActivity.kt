package com.example.gamin.tetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gamin.MonsterBattler.ui.LeaderboardScreen
import com.example.gamin.MonsterBattler.ui.SaveScoreDialog
import com.example.gamin.ui.theme.GaminTheme

const val GAME_ID_TETRIS = "tetris"

class TetrisActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val difficulty = intent.getStringExtra("difficulty") ?: "Trung bình"

        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // State quản lý Dialog và Leaderboard
                    var showSaveDialog by remember { mutableStateOf(false) }
                    var showLeaderboard by remember { mutableStateOf(false) }
                    var currentScore by remember { mutableIntStateOf(0) }

                    // Dùng AndroidView để nhúng TetrisView (SurfaceView) vào Compose
                    AndroidView(
                        factory = { context ->
                            TetrisView(context, difficulty, onGameOver = { score ->
                                currentScore = score
                                showSaveDialog = true
                            })
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Hiển thị Dialog lưu điểm đè lên game
                    if (showSaveDialog) {
                        SaveScoreDialog(
                            score = currentScore,
                            gameId = GAME_ID_TETRIS,
                            onDismiss = {
                                showSaveDialog = false
                                // Hủy lưu thì dialog tắt, lộ ra nút "Chơi lại" bên dưới
                            },
                            onSaved = {
                                showSaveDialog = false
                                showLeaderboard = true // Lưu xong thì xem bảng xếp hạng
                            }
                        )
                    }

                    // Hiển thị Bảng xếp hạng
                    if (showLeaderboard) {
                        LeaderboardScreen(
                            gameId = GAME_ID_TETRIS,
                            onBack = {
                                showLeaderboard = false
                                // Quay lại thì vẫn nhìn thấy màn hình Game Over để bấm Chơi lại
                            }
                        )
                    }
                }
            }
        }
    }
}