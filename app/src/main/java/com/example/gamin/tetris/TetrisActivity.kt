package com.example.gamin.tetris

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TetrisActivity : AppCompatActivity() {
    private lateinit var tetrisView: TetrisView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // <-- LẤY ĐỘ KHÓ TỪ INTENT -->
        // Đặt "Trung bình" làm mặc định nếu không có gì được truyền
        val difficulty = intent.getStringExtra("difficulty") ?: "Trung bình"

        // <-- TRUYỀN ĐỘ KHÓ VÀO VIEW -->
        tetrisView = TetrisView(this, difficulty)
        setContentView(tetrisView)
    }

    override fun onPause() {
        super.onPause()
        tetrisView.pause()
    }

    override fun onResume() {
        super.onResume()
        tetrisView.resume()
    }
}