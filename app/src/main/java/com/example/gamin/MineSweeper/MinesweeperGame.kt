package com.example.gamin.MineSweeper

import kotlin.random.Random

data class MinesweeperCellState(
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val minesAround: Int = 0
)

data class MinesweeperGame(
    val rows: Int,
    val cols: Int,
    val totalMines: Int,
    val board: List<List<MinesweeperCellState>> = initializeBoard(rows, cols, totalMines),
    val status: String = "Playing"
) {
    val minesLeft: Int
        get() = totalMines - board.flatten().count { it.isFlagged }

    private val revealedCount: Int
        get() = board.flatten().count { it.isRevealed }

    private val allNonMinesRevealed: Boolean
        get() = revealedCount == (rows * cols) - totalMines

    companion object {
        fun initializeBoard(rows: Int, cols: Int, totalMines: Int): List<List<MinesweeperCellState>> {
            var tempBoard = List(rows) { List(cols) { MinesweeperCellState() } }
            val minePositions = mutableSetOf<Pair<Int, Int>>()
            while (minePositions.size < totalMines) {
                minePositions.add(Pair(Random.nextInt(rows), Random.nextInt(cols)))
            }

            tempBoard = tempBoard.mapIndexed { r, row ->
                row.mapIndexed { c, cell ->
                    if (Pair(r, c) in minePositions) cell.copy(isMine = true) else cell
                }
            }

            tempBoard = tempBoard.mapIndexed { r, row ->
                row.mapIndexed { c, cell ->
                    if (!cell.isMine) {
                        val mines = countMinesAround(tempBoard, r, c, rows, cols)
                        cell.copy(minesAround = mines)
                    } else cell
                }
            }
            return tempBoard
        }

        private fun countMinesAround(board: List<List<MinesweeperCellState>>, r: Int, c: Int, rows: Int, cols: Int): Int {
            var count = 0
            for (i in -1..1) {
                for (j in -1..1) {
                    if (i == 0 && j == 0) continue
                    val nr = r + i
                    val nc = c + j
                    if (nr in 0 until rows && nc in 0 until cols && board[nr][nc].isMine) {
                        count++
                    }
                }
            }
            return count
        }
    }

    fun toggleFlag(r: Int, c: Int): MinesweeperGame {
        if (status != "Playing" || board[r][c].isRevealed) return this
        val newBoard = board.toMutableList().map { it.toMutableList() }
        val cell = newBoard[r][c]
        newBoard[r][c] = cell.copy(isFlagged = !cell.isFlagged)
        return copy(board = newBoard)
    }

    fun revealCell(r: Int, c: Int): MinesweeperGame {
        if (status != "Playing" || board[r][c].isRevealed || board[r][c].isFlagged) return this
        val cell = board[r][c]
        if (cell.isMine) {
            val revealedBoard = board.toMutableList().map { row ->
                row.map { c ->
                    if (c.isMine) c.copy(isRevealed = true) else c
                }
            }
            return copy(board = revealedBoard, status = "Game Over 💥")
        }
        val newBoard = revealEmptyCells(r, c, board.map { it.toMutableList() }.toMutableList())
        val newRevealedCount = newBoard.flatten().count { it.isRevealed }
        if (newRevealedCount == (rows * cols) - totalMines) {
            return copy(board = newBoard, status = "You Win! 🎉")
        }
        return copy(board = newBoard)
    }

    private fun revealEmptyCells(r: Int, c: Int, currentBoard: MutableList<MutableList<MinesweeperCellState>>): List<List<MinesweeperCellState>> {
        if (r !in 0 until rows || c !in 0 until cols) return currentBoard
        val cell = currentBoard[r][c]
        if (cell.isRevealed || cell.isMine || cell.isFlagged) return currentBoard
        currentBoard[r][c] = cell.copy(isRevealed = true)
        if (cell.minesAround > 0) return currentBoard
        for (i in -1..1) {
            for (j in -1..1) {
                if (i == 0 && j == 0) continue
                revealEmptyCells(r + i, c + j, currentBoard)
            }
        }
        return currentBoard
    }
}
