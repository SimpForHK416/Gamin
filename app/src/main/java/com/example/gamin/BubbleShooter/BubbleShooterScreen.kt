package com.example.gamin.BubbleShooter

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gamin.MonsterBattler.ui.LeaderboardScreen
import com.example.gamin.MonsterBattler.ui.SaveScoreDialog

@SuppressLint("ContextCastToActivity")
@Composable
fun BubbleShooterScreen() {
    val context = LocalContext.current
    val activity = (LocalContext.current as? Activity)

    // State quản lý Dialog và Leaderboard
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var currentScore by remember { mutableIntStateOf(0) }

    // Nhớ gameView để nó không bị tạo lại
    val gameView = remember {
        BubbleShooterView(context, onGameOver = { score ->
            currentScore = score
            showSaveDialog = true
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Game View (chạy ở lớp dưới cùng)
        AndroidView(
            factory = { gameView },
            modifier = Modifier.fillMaxSize()
        )

        // Nút "Back" (chạy ở lớp trên cùng)
        Button(
            onClick = {
                gameView.pause()
                activity?.finish()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            )
        ) {
            Text("Quay lại")
        }

        // --- DIALOG LƯU ĐIỂM ---
        if (showSaveDialog) {
            SaveScoreDialog(
                score = currentScore,
                gameId = GAME_ID_BUBBLE_SHOOTER,
                onDismiss = {
                    showSaveDialog = false
                    // Khi tắt dialog, người chơi sẽ thấy màn hình Game Over gốc để bấm chơi lại
                },
                onSaved = {
                    showSaveDialog = false
                    showLeaderboard = true // Mở bảng xếp hạng
                }
            )
        }

        // --- BẢNG XẾP HẠNG ---
        if (showLeaderboard) {
            LeaderboardScreen(
                gameId = GAME_ID_BUBBLE_SHOOTER,
                onBack = {
                    showLeaderboard = false
                }
            )
        }
    }
}