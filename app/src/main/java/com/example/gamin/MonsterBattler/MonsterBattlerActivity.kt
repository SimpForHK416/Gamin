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
// =============================================
// THÊM IMPORT SQLITE HELPER VÀ DATA
// =============================================
import com.example.gamin.MonsterBattler.data.MonsterDbHelper
import com.example.gamin.MonsterBattler.data.Monster // <-- Cần import file data mới
import com.example.gamin.MonsterBattler.ui.BlockadeScreen
import com.example.gamin.MonsterBattler.ui.IntroScreen
import com.example.gamin.MonsterBattler.ui.PickingScreen
import com.example.gamin.ui.theme.GaminTheme

class MonsterBattlerActivity : ComponentActivity() {

    // Khởi tạo SQLite Helper (chỉ 1 lần)
    private val dbHelper by lazy { MonsterDbHelper(this.applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    // Quản lý màn hình
                    var currentScreen by remember { mutableStateOf("INTRO") }

                    // =============================================
                    // LẤY DỮ LIỆU TỪ SQLITE KHI KHỞI TẠO
                    // =============================================
                    // 'remember' để nó chỉ chạy 1 lần
                    val monsters = remember {
                        dbHelper.getStartingMonsters()
                    }

                    when (currentScreen) {
                        "INTRO" -> {
                            IntroScreen(
                                onIntroFinished = {
                                    currentScreen = "PICKING"
                                }
                            )
                        }
                        "PICKING" -> {
                            // =============================================
                            // TRUYỀN DỮ LIỆU VÀO PICKING SCREEN
                            // =============================================
                            PickingScreen(
                                monsters = monsters, // <-- Truyền danh sách quái vật
                                onMonsterSelected = { monsterName ->
                                    Log.d("MonsterBattler", "Player selected: $monsterName")
                                    currentScreen = "BLOCKADE"
                                }
                            )
                        }
                        "BLOCKADE" -> {
                            BlockadeScreen()
                        }
                    }
                }
            }
        }
    }
}