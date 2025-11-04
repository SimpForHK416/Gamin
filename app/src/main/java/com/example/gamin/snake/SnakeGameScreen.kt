package com.example.gamin.snake

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@SuppressLint("ContextCastToActivity")
@Composable
fun SnakeGameScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)

    var score by remember { mutableStateOf(0) }
    var best by remember { mutableStateOf(prefs.getInt("best_score", 0)) }
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    val activity = (LocalContext.current as? Activity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { activity?.finish() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f)
            ) {
                Text("Quay lại")
            }

            Box(
                modifier = Modifier.weight(2f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Score: $score")
                    Text("Best: $best")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

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
