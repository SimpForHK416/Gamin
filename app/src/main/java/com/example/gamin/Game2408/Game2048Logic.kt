package com.example.gamin.game2408

import kotlin.random.Random

// --- Cấu trúc trạng thái ô và game mới ---

data class Tile(
    val id: Long,
    val value: Int,
    val row: Int,
    val col: Int,
    val mergedFrom: Pair<Tile, Tile>? = null, // Giờ đây lưu trữ toàn bộ đối tượng Tile
    val isNew: Boolean = false
)

data class Game2048State(
    val tiles: List<Tile>,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val hasWon: Boolean = false,
    val size: Int = 4,
    private val nextTileId: Long = 1L
) {
    // --- (SỬA LỖI) Tạo ID mới một cách an toàn ---
    fun getNextId(): Pair<Long, Game2048State> {
        return Pair(nextTileId, this.copy(nextTileId = this.nextTileId + 1))
    }

    val board: List<List<Int>>
        get() {
            val grid = MutableList(size) { MutableList(size) { 0 } }
            tiles.forEach { tile ->
                grid[tile.row][tile.col] = tile.value
            }
            return grid
        }

    fun checkGameOver(): Game2048State {
        if (tiles.size < size * size) return this

        for (r in 0 until size) {
            for (c in 0 until size) {
                val current = board[r][c]
                if (c < size - 1 && current == board[r][c + 1]) return this
                if (r < size - 1 && current == board[r + 1][c]) return this
            }
        }
        return copy(isGameOver = true)
    }

    fun checkWin(): Game2048State {
        if (!hasWon && tiles.any { it.value >= 2048 }) {
            return copy(hasWon = true)
        }
        return this
    }
}

// --- Logic Trượt và Hợp nhất ---
// Tuple để lưu trữ kết quả của việc xử lý một dòng
private data class LineProcessResult(
    val tiles: List<Tile>,
    val score: Int
)

private fun slideAndMerge(line: List<Tile>, state: Game2048State): Pair<LineProcessResult, Game2048State> {
    if (line.isEmpty()) return Pair(LineProcessResult(emptyList(), 0), state)

    var currentIdState = state
    var nextId: Long

    val result = mutableListOf<Tile>()
    var score = 0
    var i = 0
    while (i < line.size) {
        val current = line[i]
        if (i + 1 < line.size && current.value == line[i + 1].value) {
            val next = line[i + 1]
            val mergedValue = current.value * 2
            score += mergedValue

            // Lấy ID mới cho ô được hợp nhất
            val (id, newState) = currentIdState.getNextId()
            nextId = id
            currentIdState = newState

            result.add(
                Tile(
                    id = nextId,
                    value = mergedValue,
                    row = current.row, // Vị trí sẽ được cập nhật sau
                    col = current.col,
                    mergedFrom = Pair(current, next) // Lưu lại 2 ô gốc
                )
            )
            i += 2 // Bỏ qua ô tiếp theo vì nó đã được hợp nhất
        } else {
            result.add(current)
            i++
        }
    }
    return Pair(LineProcessResult(result, score), currentIdState)
}


// --- Hàm chính ---

fun initialize2048Game(size: Int = 4): Game2048State {
    var state = Game2048State(tiles = emptyList(), size = size)
    // Thêm 2 ô ban đầu
    val (id1, s1) = state.getNextId()
    val (id2, s2) = s1.getNextId()
    state = addNewTile(s2, 2, false, id1) // Thêm ô đầu tiên
    state = addNewTile(state, 2, false, id2) // Thêm ô thứ hai
    return state.checkGameOver().checkWin()
}

// --- (SỬA LỖI) addNewTile cần nhận ID từ bên ngoài ---
fun addNewTile(state: Game2048State, value: Int = if (Random.nextFloat() < 0.9) 2 else 4, isNew: Boolean = true, tileId: Long? = null): Game2048State {
    val board = state.board
    val size = state.size
    val emptyCells = mutableListOf<Pair<Int, Int>>()

    for (r in 0 until size) {
        for (c in 0 until size) {
            if (board[r][c] == 0) {
                emptyCells.add(Pair(r, c))
            }
        }
    }

    if (emptyCells.isEmpty()) return state

    val (r, c) = emptyCells.random()

    var tempState = state
    val id: Long
    if (tileId == null) {
        val (newId, newState) = state.getNextId()
        id = newId
        tempState = newState
    } else {
        id = tileId
    }

    val newTile = Tile(id = id, value = value, row = r, col = c, isNew = isNew)

    // Đặt lại cờ isNew cho các ô cũ
    val updatedOldTiles = tempState.tiles.map { it.copy(isNew = false) }

    return tempState.copy(
        tiles = updatedOldTiles + newTile
    )
}

// --- (SỬA LỖI HOÀN TOÀN) Hàm move đã được viết lại để hỗ trợ animation ---
fun move(state: Game2048State, direction: Direction): Game2048State {
    if (state.isGameOver) return state

    var tempState = state
    var totalScoreAdded = 0
    val newTiles = mutableListOf<Tile>()
    val tilesToRemove = mutableSetOf<Long>()
    var boardChanged = false

    // Xác định các trục để lặp
    val traversals = when (direction) {
        Direction.UP -> (0 until state.size).map { c -> (0 until state.size).map { r -> r to c } }
        Direction.DOWN -> (0 until state.size).map { c -> (0 until state.size).reversed().map { r -> r to c } }
        Direction.LEFT -> (0 until state.size).map { r -> (0 until state.size).map { c -> r to c } }
        Direction.RIGHT -> (0 until state.size).map { r -> (0 until state.size).reversed().map { c -> r to c } }
    }

    // Lặp qua từng hàng/cột
    traversals.forEach { lineCoords ->
        val lineTiles = lineCoords
            .mapNotNull { (r, c) -> tempState.tiles.find { it.row == r && it.col == c } }
            .sortedBy { if (direction == Direction.UP || direction == Direction.LEFT) it.row + it.col else -(it.row + it.col) }

        if (lineTiles.isEmpty()) return@forEach // Bỏ qua nếu dòng trống

        // Hợp nhất các ô trong dòng
        val (lineResult, newState) = slideAndMerge(lineTiles, tempState)
        tempState = newState
        totalScoreAdded += lineResult.score

        // Cập nhật vị trí mới cho các ô trong dòng đã xử lý
        lineResult.tiles.forEachIndexed { index, newTile ->
            val (newRow, newCol) = when (direction) {
                Direction.UP -> index to lineCoords[0].second
                Direction.DOWN -> (state.size - 1 - index) to lineCoords[0].second
                Direction.LEFT -> lineCoords[0].first to index
                Direction.RIGHT -> lineCoords[0].first to (state.size - 1 - index)
            }

            // Nếu ô này được hợp nhất từ 2 ô khác
            if (newTile.mergedFrom != null) {
                // Thêm ID của các ô gốc vào danh sách cần xóa
                tilesToRemove.add(newTile.mergedFrom.first.id)
                tilesToRemove.add(newTile.mergedFrom.second.id)
                // Thêm ô mới được hợp nhất (với vị trí đã cập nhật)
                newTiles.add(newTile.copy(row = newRow, col = newCol))
                boardChanged = true
            } else {
                // Nếu ô chỉ di chuyển, tìm nó trong state cũ và cập nhật vị trí
                val originalTile = tempState.tiles.find { it.id == newTile.id }
                if (originalTile != null && (originalTile.row != newRow || originalTile.col != newCol)) {
                    boardChanged = true
                }
                newTiles.add(newTile.copy(row = newRow, col = newCol))
            }
        }
    }

    if (!boardChanged) return state // Nếu không có gì thay đổi, trả về state cũ

    // Tạo danh sách ô cuối cùng:
    // 1. Lọc ra các ô không nằm trong danh sách bị xóa (chưa di chuyển hoặc hợp nhất)
    // 2. Thêm các ô đã được cập nhật vị trí hoặc đã hợp nhất
    val finalTiles = tempState.tiles.filter { it.id !in tilesToRemove }
        .map { oldTile -> newTiles.find { it.id == oldTile.id } ?: oldTile }
        .plus(newTiles.filter { newTile -> tempState.tiles.none { it.id == newTile.id } })
        .map { it.copy(isNew = false) } // Đảm bảo không có ô nào được đánh dấu là mới

    return tempState.copy(
        tiles = finalTiles,
        score = state.score + totalScoreAdded
    ).checkWin().checkGameOver()
}

enum class Direction {
    LEFT, RIGHT, UP, DOWN
}
