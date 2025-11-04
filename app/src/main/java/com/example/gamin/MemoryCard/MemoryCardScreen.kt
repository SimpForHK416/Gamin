package com.example.gamin.MemoryCard

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gamin.R
import kotlinx.coroutines.delay

private const val HIDE_DELAY_MS = 1000L

private sealed class GameScreenState {
    object DifficultySelect : GameScreenState()
    data class Playing(val rows: Int, val cols: Int) : GameScreenState()
}

@Composable
fun MemoryCardGameRoot() {
    var screenState by remember {
        mutableStateOf<GameScreenState>(GameScreenState.DifficultySelect)
    }

    when (val state = screenState) {
        is GameScreenState.DifficultySelect -> {
            DifficultySelectionScreen(
                onDifficultySelected = { rows, cols ->
                    screenState = GameScreenState.Playing(rows, cols)
                }
            )
        }
        is GameScreenState.Playing -> {
            GameGridScreen(
                rows = state.rows,
                cols = state.cols,
                onNavigateBackToSelect = {
                    screenState = GameScreenState.DifficultySelect
                }
            )
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun DifficultySelectionScreen(onDifficultySelected: (Int, Int) -> Unit) {
    val activity = (LocalContext.current as? Activity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Chọn độ khó", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onDifficultySelected(2, 3) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dễ (2x3)")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onDifficultySelected(4, 4) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Trung bình (4x4)")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onDifficultySelected(5, 6) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Khó (5x6)")
        }

        Spacer(modifier = Modifier.height(64.dp))
        OutlinedButton(
            onClick = { activity?.finish() },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Quay lại Menu")
        }
    }
}

@Composable
fun GameGridScreen(
    rows: Int,
    cols: Int,
    onNavigateBackToSelect: () -> Unit
) {
    var resetTrigger by remember { mutableIntStateOf(0) }
    var game by remember(rows, cols, resetTrigger) {
        mutableStateOf(MemoryGame(rows = rows, cols = cols))
    }

    LaunchedEffect(game.currentlyFlippedIndices) {
        if (game.currentlyFlippedIndices.size == 2) {
            delay(HIDE_DELAY_MS)
            game = game.hideMismatchedCards()
        }
    }

    if (game.status == "You Win! 🎉") {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Chúc mừng!") },
            text = { Text("Bạn đã thắng với ${game.moves} bước đi!") },
            confirmButton = {
                Button(onClick = { resetTrigger++ }) {
                    Text("Chơi lại")
                }
            },
            dismissButton = {
                Button(onClick = { onNavigateBackToSelect() }) {
                    Text("Chuyển độ khó")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateBackToSelect,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Quay lại")
            }

            Text(
                "Moves: ${game.moves}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(onClick = { resetTrigger++ }) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(game.cols),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(game.cards) { index, card ->
                MemoryCard(
                    card = card,
                    onClick = {
                        game = game.flipCard(index)
                    }
                )
            }
        }
    }
}

@Composable
fun MemoryCard(card: CardState, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "CardFlipAnimation"
    )
    val backgroundColor = if (card.isMatched) Color(0xFFA5D6A7) else MaterialTheme.colorScheme.surface
    val clickableModifier = if (card.isMatched || card.isFaceUp || rotation > 90f) {
        Modifier
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 8 * density
            }
            .then(clickableModifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation < 90f) {
                Image(
                    painter = painterResource(id = R.drawable.ic_card_back),
                    contentDescription = "Mặt sau thẻ",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = getImageResForContentId(card.contentId)),
                    contentDescription = "Nội dung thẻ",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .graphicsLayer { rotationY = 180f }
                )
            }
        }
    }
}

@Composable
private fun getImageResForContentId(contentId: Int): Int {
    return when (contentId % 15) {
        1 -> R.drawable.mem_icon_1
        2 -> R.drawable.mem_icon_2
        3 -> R.drawable.mem_icon_3
        4 -> R.drawable.mem_icon_4
        5 -> R.drawable.mem_icon_5
        6 -> R.drawable.mem_icon_6
        7 -> R.drawable.mem_icon_7
        8 -> R.drawable.mem_icon_8
        9 -> R.drawable.mem_icon_9
        10 -> R.drawable.mem_icon_10
        11 -> R.drawable.mem_icon_11
        12 -> R.drawable.mem_icon_12
        13 -> R.drawable.mem_icon_13
        14 -> R.drawable.mem_icon_14
        0 -> R.drawable.mem_icon_15
        else -> R.drawable.ic_card_back
    }
}
