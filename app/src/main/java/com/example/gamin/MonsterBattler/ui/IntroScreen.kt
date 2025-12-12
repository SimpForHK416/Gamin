package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.gamin.MonsterBattler.Narrator

@Composable
fun IntroScreen(
    onIntroFinished: () -> Unit
) {
    var dialogueIndex by remember { mutableStateOf(0) }

    val currentFullText = Narrator.introDialogue[dialogueIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
    ) {
        // 1. Ảnh Giáo sư
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterFor(name = "oak"),
                contentDescription = "Narrator",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            DialogueBox(
                fullText = currentFullText,
                onNext = {
                    // Logic chuyển sang câu thoại tiếp theo
                    if (dialogueIndex < Narrator.introDialogue.lastIndex) {
                        dialogueIndex++
                    } else {
                        onIntroFinished()
                    }
                }
            )
        }
    }
}