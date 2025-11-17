// Vị trí: com/example/gamin/MonsterBattler/MonsterBattlerActivity.kt
package com.example.gamin.MonsterBattler

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gamin.MonsterBattler.ui.BlockadeScreen
import com.example.gamin.MonsterBattler.ui.IntroScreen
import com.example.gamin.MonsterBattler.ui.PickingScreen
import com.example.gamin.ui.theme.GaminTheme

class MonsterBattlerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    // Quản lý màn hình
                    var currentScreen by remember { mutableStateOf("INTRO") }

                    when (currentScreen) {
                        "INTRO" -> {
                            IntroScreen(
                                onIntroFinished = {
                                    // 1. Sau khi intro xong, chuyển sang PICKING
                                    currentScreen = "PICKING"
                                }
                            )
                        }
                        "PICKING" -> {
                            PickingScreen(
                                onMonsterSelected = { monsterName ->
                                    // 2. Sau khi chọn monster, chuyển sang BLOCKADE
                                    Log.d("MonsterBattler", "Player selected: $monsterName")
                                    currentScreen = "BLOCKADE"
                                }
                            )
                        }
                        "BLOCKADE" -> {
                            // 3. Màn hình "Coming Soon"
                            BlockadeScreen()
                        }
                    }
                }
            }
        }
    }
}