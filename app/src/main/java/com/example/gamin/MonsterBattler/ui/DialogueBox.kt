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
        var displayedText by remember { mutableStateOf("") }

        val scope = rememberCoroutineScope()
        var typingJob by remember { mutableStateOf<Job?>(null) }

        val isTypingFinished = displayedText == fullText

        LaunchedEffect(fullText) {
            typingJob?.cancel()
            typingJob = scope.launch {
                displayedText = ""
                fullText.forEach { char ->
                    delay(30L)
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isTypingFinished) {
                        onNext()
                    } else {
                        typingJob?.cancel()
                        displayedText = fullText
                    }
                }
        ) {
            Text(
                text = displayedText,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                lineHeight = 24.sp
            )

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