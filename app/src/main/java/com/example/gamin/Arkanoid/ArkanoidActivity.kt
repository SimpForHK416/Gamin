package com.example.gamin.Arkanoid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.gamin.MonsterBattler.ui.LeaderboardScreen
import com.example.gamin.MonsterBattler.ui.SaveScoreDialog
import com.example.gamin.ui.theme.GaminTheme

// ID định danh cho Arkanoid trên Firebase
const val GAME_ID_ARKANOID = "arkanoid"

class ArkanoidActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // State quản lý việc hiện Dialog và Leaderboard
                    var showSaveDialog by remember { mutableStateOf(false) }
                    var showLeaderboard by remember { mutableStateOf(false) }
                    var currentScore by remember { mutableIntStateOf(0) }

                    if (showSaveDialog) {
                        SaveScoreDialog(
                            score = currentScore,
                            gameId = GAME_ID_ARKANOID,
                            onDismiss = { showSaveDialog = false },
                            onSaved = {
                                showSaveDialog = false
                                showLeaderboard = true // Lưu xong thì mở bảng xếp hạng
                            }
                        )
                    } else if (showLeaderboard) {
                        LeaderboardScreen(
                            gameId = GAME_ID_ARKANOID,
                            onBack = { showLeaderboard = false }
                        )
                    } else {
                        // Truyền callback để Screen gọi khi Game Over
                        ArkanoidScreen(
                            onGameOver = { finalScore ->
                                currentScore = finalScore
                                showSaveDialog = true
                            },
                            onShowLeaderboard = {
                                showLeaderboard = true
                            }
                        )
                    }
                }
            }
        }
    }
}