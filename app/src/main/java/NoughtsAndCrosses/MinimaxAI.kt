package com.example.gamin.NoughtsAndCrosses

object MinimaxAI {
    private const val AI_PLAYER = "O"
    private const val HUMAN_PLAYER = "X"
    private const val BOARD_SIZE = 5
    private const val WIN_CONDITION = 3

    // CÓ THỂ ĐIỀU CHỈNH: Độ sâu tối đa để tìm kiếm.
    // Thử với 4. Nếu vẫn chậm, giảm xuống 3.
    // Nếu quá "ngu", tăng lên 5.
    private const val MAX_DEPTH = 4

    fun findBestMove(board: List<String>): Int? {
        var bestScore = Int.MIN_VALUE
        var move: Int? = null

        // Cắt tỉa Alpha-Beta chỉ được dùng trong Minimax đệ quy,
        // không dùng ở cấp độ này.
        for (i in board.indices) {
            if (board[i].isEmpty()) {
                val newBoard = board.toMutableList()
                newBoard[i] = AI_PLAYER

                // Thay đổi: Truyền alpha (MIN_VALUE) và beta (MAX_VALUE)
                val score = minimax(newBoard, MAX_DEPTH - 1, false, Int.MIN_VALUE, Int.MAX_VALUE)

                if (score > bestScore) {
                    bestScore = score
                    move = i
                }
            }
        }
        return move
    }

    // THAY ĐỔI: Thêm alpha và beta vào hàm Minimax
    private fun minimax(board: List<String>, depth: Int, isMaximizing: Boolean, alpha: Int, beta: Int): Int {
        var currentAlpha = alpha
        var currentBeta = beta

        val winner = checkWinner(board)
        if (winner != null || depth == 0) {
            return when (winner) {
                AI_PLAYER -> 1000000 + depth
                HUMAN_PLAYER -> -1000000 - depth
                "Draw" -> 0
                else -> if (depth == 0) evaluate(board) else 0 // Trạng thái hòa hoặc đạt MAX_DEPTH
            }
        }

        if (isMaximizing) {
            var bestScore = Int.MIN_VALUE
            for (i in board.indices) {
                if (board[i].isEmpty()) {
                    val newBoard = board.toMutableList()
                    newBoard[i] = AI_PLAYER

                    // Gọi đệ quy với alpha và beta
                    val score = minimax(newBoard, depth - 1, false, currentAlpha, currentBeta)
                    bestScore = maxOf(score, bestScore)
                    currentAlpha = maxOf(currentAlpha, bestScore) // Cập nhật Alpha

                    if (currentBeta <= currentAlpha) break // CẮT TỈA BETA
                }
            }
            return bestScore
        } else {
            var bestScore = Int.MAX_VALUE
            for (i in board.indices) {
                if (board[i].isEmpty()) {
                    val newBoard = board.toMutableList()
                    newBoard[i] = HUMAN_PLAYER

                    // Gọi đệ quy với alpha và beta
                    val score = minimax(newBoard, depth - 1, true, currentAlpha, currentBeta)
                    bestScore = minOf(score, bestScore)
                    currentBeta = minOf(currentBeta, bestScore) // Cập nhật Beta

                    if (currentBeta <= currentAlpha) break // CẮT TỈA ALPHA
                }
            }
            return bestScore
        }
    }

    // --- Hàm evaluate và checkWinner giữ nguyên như phiên bản trước ---

    private fun evaluate(board: List<String>): Int {
        var score = 0
        val centerIndex = 12 // (5*5)/2 = 12
        if (board[centerIndex] == AI_PLAYER) score += 10
        if (board[centerIndex] == HUMAN_PLAYER) score -= 10

        fun calculateLineScore(slice: List<String>): Int {
            val aiCount = slice.count { it == AI_PLAYER }
            val humanCount = slice.count { it == HUMAN_PLAYER }

            if (aiCount == WIN_CONDITION) return 1000000
            if (humanCount == WIN_CONDITION) return -1000000

            if (aiCount == WIN_CONDITION - 1 && humanCount == 0) return 500
            if (humanCount == WIN_CONDITION - 1 && aiCount == 0) return -500

            if (aiCount == WIN_CONDITION - 2 && humanCount == 0) return 100
            if (humanCount == WIN_CONDITION - 2 && aiCount == 0) return -100

            return 0
        }

        // Hàng
        for (r in 0 until BOARD_SIZE) {
            for (c in 0..BOARD_SIZE - WIN_CONDITION) {
                val slice = board.slice(r * BOARD_SIZE + c until r * BOARD_SIZE + c + WIN_CONDITION)
                score += calculateLineScore(slice)
            }
        }
        // Cột
        for (c in 0 until BOARD_SIZE) {
            for (r in 0..BOARD_SIZE - WIN_CONDITION) {
                val slice = (0 until WIN_CONDITION).map { board[(r + it) * BOARD_SIZE + c] }
                score += calculateLineScore(slice)
            }
        }
        // Chéo chính
        for (r in 0..BOARD_SIZE - WIN_CONDITION) {
            for (c in 0..BOARD_SIZE - WIN_CONDITION) {
                val slice = (0 until WIN_CONDITION).map { board[(r + it) * BOARD_SIZE + (c + it)] }
                score += calculateLineScore(slice)
            }
        }
        // Chéo phụ
        for (r in 0..BOARD_SIZE - WIN_CONDITION) {
            for (c in WIN_CONDITION - 1 until BOARD_SIZE) {
                val slice = (0 until WIN_CONDITION).map { board[(r + it) * BOARD_SIZE + (c - it)] }
                score += calculateLineScore(slice)
            }
        }

        return score
    }

    fun checkWinner(board: List<String>): String? {
        // kiểm tra hàng
        for (r in 0 until BOARD_SIZE) {
            for (c in 0..BOARD_SIZE - WIN_CONDITION) {
                val slice = board.slice(r * BOARD_SIZE + c until r * BOARD_SIZE + c + WIN_CONDITION)
                if (slice.all { it == HUMAN_PLAYER }) return HUMAN_PLAYER
                if (slice.all { it == AI_PLAYER }) return AI_PLAYER
            }
        }
        // kiểm tra cột
        for (c in 0 until BOARD_SIZE) {
            for (r in 0..BOARD_SIZE - WIN_CONDITION) {
                val slice = (0 until WIN_CONDITION).map { board[(r + it) * BOARD_SIZE + c] }
                if (slice.all { it == HUMAN_PLAYER }) return HUMAN_PLAYER
                if (slice.all { it == AI_PLAYER }) return AI_PLAYER
            }
        }
        // kiểm tra chéo chính
        for (r in 0..BOARD_SIZE - WIN_CONDITION) {
            for (c in 0..BOARD_SIZE - WIN_CONDITION) {
                val slice = (0 until WIN_CONDITION).map { board[(r + it) * BOARD_SIZE + (c + it)] }
                if (slice.all { it == HUMAN_PLAYER }) return HUMAN_PLAYER
                if (slice.all { it == AI_PLAYER }) return AI_PLAYER
            }
        }
        // kiểm tra chéo phụ
        for (r in 0..BOARD_SIZE - WIN_CONDITION) {
            for (c in WIN_CONDITION - 1 until BOARD_SIZE) {
                val slice = (0 until WIN_CONDITION).map { board[(r + it) * BOARD_SIZE + (c - it)] }
                if (slice.all { it == HUMAN_PLAYER }) return HUMAN_PLAYER
                if (slice.all { it == AI_PLAYER }) return AI_PLAYER
            }
        }
        // hòa
        if (board.none { it.isEmpty() }) return "Draw"
        return null
    }
}