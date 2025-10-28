package com.example.gamin.Pong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.gamin.ui.theme.GaminTheme

class PongActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaminTheme {
                // Pong thường có nền đen
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black // Nền đen cho game Pong
                ) {
                    PongScreen()
                }
            }
        }
    }
}
