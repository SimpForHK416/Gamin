// Đặt trong thư mục: com.example.gamin/MemoryCard/MemoryCardScreen.kt

package com.example.gamin.MemoryCard

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val HIDE_DELAY_MS = 1000L // 1 giây

@Composable
fun MemoryCardScreen() {
    // Khởi tạo trạng thái game (4x4 = 16 thẻ, 8 cặp)
    var game by remember {
        mutableStateOf(MemoryGame(rows = 4, cols = 4))
    }

    // --- Logic Tự động lật úp thẻ ---
    LaunchedEffect(game.currentlyFlippedIndices) {
        if (game.currentlyFlippedIndices.size == 2) {
            // Chờ một chút để người chơi nhìn thấy
            delay(HIDE_DELAY_MS)

            // Lật úp các thẻ không khớp
            game = game.hideMismatchedCards()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header và Status ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Moves: ${game.moves}", style = MaterialTheme.typography.titleMedium)
            Text(game.status, style = MaterialTheme.typography.titleLarge)
            Button(onClick = { game = MemoryGame(4, 4) }) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Game Grid ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(game.cols),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(4.dp),
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
    val backgroundColor = when {
        card.isMatched -> Color(0xFFA5D6A7) // Xanh lá nhạt khi khớp
        card.isFaceUp -> Color(0xFFFDD835) // Vàng khi lật
        else -> Color(0xFF1976D2) // Xanh dương khi úp
    }

    // Nếu đã khớp, thẻ sẽ không thể click được nữa
    val clickableModifier = if (card.isMatched) {
        Modifier
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .aspectRatio(1f) // Thẻ vuông
            .then(clickableModifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (card.isFaceUp || card.isMatched) {
                // Sử dụng emoji làm nội dung thẻ
                val emojiContent = getEmojiForContentId(card.contentId)
                Text(
                    text = emojiContent,
                    fontSize = 32.sp
                )
            }
        }
    }
}

// Chuyển ID nội dung thành một Emoji (hoặc Drawable/Image nếu bạn dùng tài nguyên)
private fun getEmojiForContentId(contentId: Int): String {
    return when (contentId % 8) { // Giả sử tối đa 8 cặp
        1 -> "🍎"
        2 -> "🍊"
        3 -> "🍌"
        4 -> "🍓"
        5 -> "🍇"
        6 -> "🍍"
        7 -> "🍉"
        else -> "🥝"
    }
}