// Vị trí: com/example/gamin/MonsterBattler/ui/DialogueBox.kt
package com.example.gamin.MonsterBattler.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialogueBox(
    text: String, // Chỉ nhận text để hiển thị
    isTypingFinished: Boolean, // Thêm biến bool để biết khi nào gõ xong
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp) // Chiều cao cố định
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(4.dp, Color.Black, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Hiển thị text được truyền vào
        Text(
            text = text,
            color = Color.Black,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            maxLines = 2
        )

        // =============================================
        // LƯU Ý MỚI: Indicator "Tiếp tục"
        // =============================================
        if (isTypingFinished) {
            // Hiệu ứng nhấp nháy cho indicator
            val infiniteTransition = rememberInfiniteTransition(label = "indicator-pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "indicator-alpha"
            )

            Text(
                text = "... ▶", // Dùng ký tự '▶' (filled arrowhead)
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Đặt ở góc dưới bên phải
                    .alpha(alpha) // Áp dụng hiệu ứng nhấp nháy
            )
        }
    }
}