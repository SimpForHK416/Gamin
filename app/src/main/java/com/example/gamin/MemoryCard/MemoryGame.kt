package com.example.gamin.MemoryCard

import kotlin.random.Random

data class CardState(
    val id: Int,
    val contentId: Int,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

data class MemoryGame(
    val rows: Int = 4,
    val cols: Int = 4,
    val cards: List<CardState> = initializeCards(rows, cols),
    val currentlyFlippedIndices: List<Int> = emptyList(),
    val status: String = "Playing",
    val moves: Int = 0
) {
    companion object {
        private fun initializeCards(rows: Int, cols: Int): List<CardState> {
            val totalPairs = (rows * cols) / 2
            val totalCards = rows * cols
            val contentList = (1..totalPairs).flatMap { listOf(it, it) }.shuffled(Random)
            return List(totalCards) { index ->
                CardState(
                    id = index,
                    contentId = contentList[index]
                )
            }
        }
    }

    private val allMatched: Boolean
        get() = cards.all { it.isMatched }

    fun flipCard(index: Int): MemoryGame {
        if (status != "Playing" || index < 0 || index >= cards.size) return this
        val cardToFlip = cards[index]
        if (cardToFlip.isFaceUp || cardToFlip.isMatched) return this

        val newCards = cards.toMutableList()
        newCards[index] = cardToFlip.copy(isFaceUp = true)
        val newFlipped = currentlyFlippedIndices + index

        if (newFlipped.size == 1) {
            return copy(cards = newCards, currentlyFlippedIndices = newFlipped)
        }

        if (newFlipped.size == 2) {
            val firstCard = newCards[newFlipped[0]]
            val secondCard = newCards[newFlipped[1]]
            val isMatch = firstCard.contentId == secondCard.contentId
            val nextMoves = moves + 1

            if (isMatch) {
                newCards[newFlipped[0]] = firstCard.copy(isMatched = true)
                newCards[newFlipped[1]] = secondCard.copy(isMatched = true)
                val game = copy(
                    cards = newCards,
                    currentlyFlippedIndices = emptyList(),
                    moves = nextMoves
                )
                return if (game.allMatched) game.copy(status = "You Win! 🎉") else game
            } else {
                return copy(
                    cards = newCards,
                    currentlyFlippedIndices = newFlipped,
                    moves = nextMoves
                )
            }
        }

        return this
    }

    fun hideMismatchedCards(): MemoryGame {
        if (currentlyFlippedIndices.size < 2) return this
        val firstCard = cards[currentlyFlippedIndices[0]]
        val secondCard = cards[currentlyFlippedIndices[1]]
        if (!firstCard.isMatched && !secondCard.isMatched) {
            val newCards = cards.toMutableList()
            newCards[currentlyFlippedIndices[0]] = firstCard.copy(isFaceUp = false)
            newCards[currentlyFlippedIndices[1]] = secondCard.copy(isFaceUp = false)
            return copy(cards = newCards, currentlyFlippedIndices = emptyList())
        }
        return this
    }
}
