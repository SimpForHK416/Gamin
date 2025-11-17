// Vị trí: com/example/gamin/MonsterBattler/ui/IntroScreen.kt
package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.gamin.MonsterBattler.Narrator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IntroScreen(
    onIntroFinished: () -> Unit
) {
    var dialogueIndex by remember { mutableStateOf(0) }

    val currentFullText = Narrator.introDialogue[dialogueIndex]
    var displayedText by remember { mutableStateOf("") }
    var typingFinished by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var typingJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(key1 = currentFullText) {
        typingJob?.cancel()
        typingJob = coroutineScope.launch {
            typingFinished = false
            displayedText = ""
            currentFullText.forEachIndexed { index, char ->
                displayedText = currentFullText.substring(0, index + 1)
                delay(50L)
            }
            typingFinished = true
        }
    }

    // =============================================
    // LƯU Ý MỚI: Dùng helper 'painterFor'
    // Bạn chỉ cần đặt file 'oak.png' vào res/drawable là code này sẽ chạy
    // =============================================
    val oakPainter = painterFor(name = "oak")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (typingFinished) {
                    typingJob?.cancel()
                    if (dialogueIndex < Narrator.introDialogue.lastIndex) {
                        dialogueIndex++
                    } else {
                        onIntroFinished()
                    }
                } else {
                    typingJob?.cancel()
                    displayedText = currentFullText
                    typingFinished = true
                }
            }
    ) {
        // 1. Giáo sư
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = oakPainter, // <-- ĐÃ THAY ĐỔI
                contentDescription = "Narrator",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
        }

        // 2. Hộp thoại
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            DialogueBox(
                text = displayedText,
                isTypingFinished = typingFinished
            )
        }
    }
}