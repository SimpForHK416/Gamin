package com.example.gamin.snake

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SnakeGameScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)

    var score by remember { mutableStateOf(0) }
    var best by remember { mutableStateOf(prefs.getInt("best_score", 0)) }
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Điểm
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Score: $score")
            Text("Best: $best")
        }

        Spacer(Modifier.height(16.dp))

        // 🟩 Khu vực game
        SnakeGameCanvas(
            isPlaying = isPlaying,
            onScoreChanged = { score = it },
            onGameOver = { finalScore ->
                isPlaying = false
                isGameOver = true
                if (finalScore > best) {
                    best = finalScore
                    prefs.edit().putInt("best_score", finalScore).apply()
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // 🕹 Nút Bắt đầu / Chơi lại
        if (!isPlaying && !isGameOver) {
            Button(onClick = {
                score = 0
                isGameOver = false
                isPlaying = true
            }) {
                Text("Bắt đầu")
            }
        }

        if (isGameOver) {
            Text("Game Over 🐍", modifier = Modifier.padding(8.dp))
            Button(onClick = {
                score = 0
                isGameOver = false
                isPlaying = true
            }) {
                Text("Chơi lại")
            }
        }
    }
}
