package com.example.gamin.Pong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.gamin.ui.theme.GaminTheme

class PongActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // <-- ĐÃ THÊM: Lấy chế độ chơi từ Intent -->
        val mode = intent.getStringExtra("mode") ?: "PVE"
        val gameMode = if (mode == "PVP") GameMode.PVP else GameMode.PVE

        setContent {
            GaminTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    // <-- ĐÃ THÊM: Truyền gameMode vào PongScreen -->
                    PongScreen(gameMode = gameMode)
                }
            }
        }
    }
}