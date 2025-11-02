package com.example.gamin.tetris

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TetrisActivity : AppCompatActivity() {
    private lateinit var tetrisView: TetrisView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tạo view game trực tiếp, không cần XML
        tetrisView = TetrisView(this)
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
