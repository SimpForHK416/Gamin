package com.example.gamin.BubbleShooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gamin.ui.theme.GaminTheme

// ID định danh cho Bubble Shooter trên Firebase
const val GAME_ID_BUBBLE_SHOOTER = "bubble_shooter"

class BubbleShooterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GaminTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BubbleShooterScreen()
                }
            }
        }
    }
}