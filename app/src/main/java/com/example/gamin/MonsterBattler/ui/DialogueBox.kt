package com.example.gamin.MonsterBattler.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DialogueBox(
    fullText: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. State lưu văn bản đang hiển thị
    var displayedText by remember { mutableStateOf("") }

    // 2. Quản lý Coroutine để chạy chữ
    val scope = rememberCoroutineScope()
    var typingJob by remember { mutableStateOf<Job?>(null) }

    // 3. Kiểm tra xem đã hiện hết chữ chưa (để hiện mũi tên)
    val isTypingFinished = displayedText == fullText

    // Mỗi khi 'fullText' thay đổi (câu thoại mới), bắt đầu chạy chữ
    LaunchedEffect(fullText) {
        typingJob?.cancel() // Hủy job cũ nếu có
        typingJob = scope.launch {
            displayedText = ""
            fullText.forEach { char ->
                delay(30L) // Tốc độ gõ chữ
                displayedText += char
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(4.dp, Color.Black, RoundedCornerShape(12.dp))
            .padding(16.dp)
            // 4. XỬ LÝ CLICK (Logic Skip/Next ở đây)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isTypingFinished) {
                    // TRƯỜNG HỢP 2: Chữ đã hiện hết -> Chuyển câu tiếp theo
                    onNext()
                } else {
                    // TRƯỜNG HỢP 1: Chữ đang chạy -> Dừng chạy và hiện hết ngay lập tức
                    typingJob?.cancel()
                    displayedText = fullText
                }
            }
    ) {
        // Hiển thị text
        Text(
            text = displayedText,
            color = Color.Black,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            lineHeight = 24.sp
        )

        // 5. Chỉ hiện mũi tên nhấp nháy khi đã hiện hết chữ
        if (isTypingFinished) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "alpha"
            )

            Text(
                text = "▼",
                color = Color.Red,
                fontSize = 24.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .alpha(alpha)
            )
        }
    }
}