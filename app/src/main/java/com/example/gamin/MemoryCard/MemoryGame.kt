// Đặt trong thư mục: com.example.gamin/MemoryCard/MemoryGame.kt

package com.example.gamin.MemoryCard

import kotlin.random.Random

// 1. Trạng thái của một thẻ bài
data class CardState(
    val id: Int, // ID duy nhất cho mỗi vị trí
    val contentId: Int, // ID của nội dung (giá trị thẻ, ví dụ: 1, 2, 3...)
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

// 2. Trạng thái Game
data class MemoryGame(
    val rows: Int = 4,
    val cols: Int = 4,
    val cards: List<CardState> = initializeCards(rows, cols),
    val currentlyFlippedIndices: List<Int> = emptyList(), // Danh sách 0, 1 hoặc 2 thẻ đang lật
    val status: String = "Playing",
    val moves: Int = 0
) {
    companion object {
        private fun initializeCards(rows: Int, cols: Int): List<CardState> {
            val totalPairs = (rows * cols) / 2
            val totalCards = rows * cols

            // Tạo các cặp nội dung (ví dụ: 1, 1, 2, 2, 3, 3,...)
            val contentList = (1..totalPairs).flatMap { listOf(it, it) }.shuffled(Random)

            // Tạo danh sách CardState với ID và Content ID
            return List(totalCards) { index ->
                CardState(
                    id = index,
                    contentId = contentList[index]
                )
            }
        }
    }

    // Kiểm tra đã thắng chưa
    private val allMatched: Boolean
        get() = cards.all { it.isMatched }

    // 3. Logic lật thẻ
    fun flipCard(index: Int): MemoryGame {
        // Chỉ cho phép lật khi đang "Playing"
        if (status != "Playing" || index < 0 || index >= cards.size) return this

        val cardToFlip = cards[index]

        // Không lật thẻ đã lật hoặc đã khớp
        if (cardToFlip.isFaceUp || cardToFlip.isMatched) return this

        // --- Bắt đầu quá trình lật ---

        // Tạo trạng thái mới cho thẻ lật
        val newCards = cards.toMutableList()
        newCards[index] = cardToFlip.copy(isFaceUp = true)

        val newFlipped = currentlyFlippedIndices + index

        // Trường hợp 1: Thẻ thứ nhất được lật
        if (newFlipped.size == 1) {
            return copy(cards = newCards, currentlyFlippedIndices = newFlipped)
        }

        // Trường hợp 2: Thẻ thứ hai được lật
        if (newFlipped.size == 2) {
            val firstCard = newCards[newFlipped[0]]
            val secondCard = newCards[newFlipped[1]]

            // Kiểm tra khớp
            val isMatch = firstCard.contentId == secondCard.contentId

            val nextMoves = moves + 1

            if (isMatch) {
                // Đánh dấu khớp và reset danh sách đang lật
                newCards[newFlipped[0]] = firstCard.copy(isMatched = true)
                newCards[newFlipped[1]] = secondCard.copy(isMatched = true)

                val game = copy(
                    cards = newCards,
                    currentlyFlippedIndices = emptyList(),
                    moves = nextMoves
                )

                // Kiểm tra thắng
                return if (game.allMatched) game.copy(status = "You Win! 🎉") else game
            } else {
                // Không khớp: Để thẻ lật trong danh sách currentlyFlippedIndices
                // Logic này cho phép người chơi nhìn thấy 2 thẻ lật trong thời gian ngắn
                // trước khi tự động lật úp lại (sẽ xử lý trong Composable)
                return copy(
                    cards = newCards,
                    currentlyFlippedIndices = newFlipped,
                    moves = nextMoves
                )
            }
        }

        return this // Không nên xảy ra
    }

    // Logic lật úp lại 2 thẻ KHÔNG khớp (được gọi sau một độ trễ)
    fun hideMismatchedCards(): MemoryGame {
        if (currentlyFlippedIndices.size < 2) return this

        val firstCard = cards[currentlyFlippedIndices[0]]
        val secondCard = cards[currentlyFlippedIndices[1]]

        // Lật úp lại nếu chúng chưa khớp (chưa có isMatched = true)
        if (!firstCard.isMatched && !secondCard.isMatched) {
            val newCards = cards.toMutableList()
            newCards[currentlyFlippedIndices[0]] = firstCard.copy(isFaceUp = false)
            newCards[currentlyFlippedIndices[1]] = secondCard.copy(isFaceUp = false)

            return copy(
                cards = newCards,
                currentlyFlippedIndices = emptyList() // Reset danh sách
            )
        }
        return this // Nếu đã khớp thì không làm gì
    }
}