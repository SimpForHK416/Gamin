package com.example.gamin.BubbleShooter

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("ContextCastToActivity")
@Composable
fun BubbleShooterScreen() {
    val context = LocalContext.current
    // Lấy Activity hiện tại để có thể gọi .finish()
    val activity = (LocalContext.current as? Activity)

    // Nhớ gameView để nó không bị tạo lại
    val gameView = remember {
        BubbleShooterView(context)
    }

    // Sử dụng Box để xếp chồng các Composable lên nhau
    Box(modifier = Modifier.fillMaxSize()) {

        // Game View (chạy ở lớp dưới cùng)
        AndroidView(
            factory = { gameView },
            modifier = Modifier.fillMaxSize()
        )

        // Nút "Back" (chạy ở lớp trên cùng, góc trên bên trái)
        Button(
            onClick = {
                // Tạm dừng game thread trước khi thoát
                gameView.pause()
                activity?.finish() // Đóng Activity này
            },
            modifier = Modifier
                .align(Alignment.TopStart) // Ghim vào góc trên bên trái
                .padding(16.dp),
            // Thêm màu nền bán trong suốt để giống ảnh
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            )
        ) {
            Text("Quay lại")
        }
    }
}