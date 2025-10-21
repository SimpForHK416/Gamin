// Đặt trong thư mục: com.example.gamin/MemoryCard/MemoryCardActivity.kt

package com.example.gamin.MemoryCard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gamin.ui.theme.GaminTheme

class MemoryCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MemoryCardScreen()
                }
            }
        }
    }
}