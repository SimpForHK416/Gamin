package com.example.gamin.game2408

import kotlin.random.Random

data class Tile(
    val id: Long,
    val value: Int,
    val row: Int,
    val col: Int,
    val mergedFrom: Pair<Tile, Tile>? = null,
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
            val (id, newState) = currentIdState.getNextId()
            nextId = id
            currentIdState = newState
            result.add(
                Tile(
                    id = nextId,
                    value = mergedValue,
                    row = current.row,
                    col = current.col,
                    mergedFrom = Pair(current, next)
                )
            )
            i += 2
        } else {
            result.add(current)
            i++
        }
    }
    return Pair(LineProcessResult(result, score), currentIdState)
}

fun initialize2048Game(size: Int = 4): Game2048State {
    var state = Game2048State(tiles = emptyList(), size = size)
    val (id1, s1) = state.getNextId()
    val (id2, s2) = s1.getNextId()
    state = addNewTile(s2, 2, false, id1)
    state = addNewTile(state, 2, false, id2)
    return state.checkGameOver().checkWin()
}

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
    val updatedOldTiles = tempState.tiles.map { it.copy(isNew = false) }
    return tempState.copy(
        tiles = updatedOldTiles + newTile
    )
}

fun move(state: Game2048State, direction: Direction): Game2048State {
    if (state.isGameOver) return state
    var tempState = state
    var totalScoreAdded = 0
    val newTiles = mutableListOf<Tile>()
    val tilesToRemove = mutableSetOf<Long>()
    var boardChanged = false
    val traversals = when (direction) {
        Direction.UP -> (0 until state.size).map { c -> (0 until state.size).map { r -> r to c } }
        Direction.DOWN -> (0 until state.size).map { c -> (0 until state.size).reversed().map { r -> r to c } }
        Direction.LEFT -> (0 until state.size).map { r -> (0 until state.size).map { c -> r to c } }
        Direction.RIGHT -> (0 until state.size).map { r -> (0 until state.size).reversed().map { c -> r to c } }
    }
    traversals.forEach { lineCoords ->
        val lineTiles = lineCoords
            .mapNotNull { (r, c) -> tempState.tiles.find { it.row == r && it.col == c } }
            .sortedBy { if (direction == Direction.UP || direction == Direction.LEFT) it.row + it.col else -(it.row + it.col) }
        if (lineTiles.isEmpty()) return@forEach
        val (lineResult, newState) = slideAndMerge(lineTiles, tempState)
        tempState = newState
        totalScoreAdded += lineResult.score
        lineResult.tiles.forEachIndexed { index, newTile ->
            val (newRow, newCol) = when (direction) {
                Direction.UP -> index to lineCoords[0].second
                Direction.DOWN -> (state.size - 1 - index) to lineCoords[0].second
                Direction.LEFT -> lineCoords[0].first to index
                Direction.RIGHT -> lineCoords[0].first to (state.size - 1 - index)
            }
            if (newTile.mergedFrom != null) {
                tilesToRemove.add(newTile.mergedFrom.first.id)
                tilesToRemove.add(newTile.mergedFrom.second.id)
                newTiles.add(newTile.copy(row = newRow, col = newCol))
                boardChanged = true
            } else {
                val originalTile = tempState.tiles.find { it.id == newTile.id }
                if (originalTile != null && (originalTile.row != newRow || originalTile.col != newCol)) {
                    boardChanged = true
                }
                newTiles.add(newTile.copy(row = newRow, col = newCol))
            }
        }
    }
    if (!boardChanged) return state
    val finalTiles = tempState.tiles.filter { it.id !in tilesToRemove }
        .map { oldTile -> newTiles.find { it.id == oldTile.id } ?: oldTile }
        .plus(newTiles.filter { newTile -> tempState.tiles.none { it.id == newTile.id } })
        .map { it.copy(isNew = false) }
    return tempState.copy(
        tiles = finalTiles,
        score = state.score + totalScoreAdded
    ).checkWin().checkGameOver()
}

enum class Direction {
    LEFT, RIGHT, UP, DOWN
}
