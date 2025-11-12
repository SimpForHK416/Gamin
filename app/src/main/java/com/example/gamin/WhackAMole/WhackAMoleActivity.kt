package com.example.gamin.WhackAMole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gamin.ui.theme.GaminTheme

class WhackAMoleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val difficulty = when(intent.getStringExtra("difficulty")) {
            "Dễ" -> Difficulty.EASY
            "Trung bình" -> Difficulty.MEDIUM
            "Khó" -> Difficulty.HARD
            else -> Difficulty.EASY
        }
        
        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WhackAMoleGame(difficulty = difficulty)
                }
            }
        }
    }
}
