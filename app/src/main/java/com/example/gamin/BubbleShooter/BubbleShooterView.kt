package com.example.gamin.BubbleShooter

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.ArrayDeque
import kotlin.math.*
import kotlin.random.Random

class BubbleShooterView(
    context: Context,
    private val onGameOver: (Int) -> Unit
) : SurfaceView(context), SurfaceHolder.Callback {

    private val thread: BubbleShooterThread
    private var screenWidth = 0
    private var screenHeight = 0

    init {
        holder.addCallback(this)
        thread = BubbleShooterThread(holder, context, onGameOver)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread.setRunning(true)
        thread.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        while (retry) {
            try {
                thread.setRunning(false)
                thread.join()
                retry = false
            } catch (e: InterruptedException) {
                // Will try again
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        thread.setSurfaceSize(width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return thread.doTouchEvent(event)
    }

    fun resume() {
        thread.unpause()
    }

    fun pause() {
        thread.pause()
    }
}

class BubbleShooterThread(
    private val surfaceHolder: SurfaceHolder,
    private val context: Context,
    private val onGameOver: (Int) -> Unit
) : Thread() {

    private var running = false
    private var paused = false
    private var screenWidth = 0
    private var screenHeight = 0

    private val BUBBLE_COLORS = arrayOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.rgb(255, 165, 0)
    )
    private val BOMB_BUBBLE_TYPE = -2
    private val RAINBOW_BUBBLE_TYPE = -3

    private val LEVEL_COLUMNS = 12
    private val LEVEL_ROWS = 16
    private var TILE_WIDTH = 60f
    private var TILE_HEIGHT = 60f
    private var ROW_HEIGHT = 52f
    private var BUBBLE_RADIUS = 28f

    private enum class GameState { READY, SHOOT_BUBBLE, REMOVE_CLUSTER, GAME_OVER }
    private var gameState = GameState.READY
    private var score = 0
    private var turnCounter = 0
    private var rowOffset = 0

    private var hasTriggeredGameOver = false

    private var bombCount = 3
    private var rainbowCount = 3

    private var levelX = 0f
    private var levelY = 0f
    private var levelWidth = 0f
    private var levelHeight = 0f
    private val tiles = Array(LEVEL_COLUMNS) { Array(LEVEL_ROWS) { Tile(-1) } }

    private var playerX = 0f
    private var playerY = 0f
    private var playerAngle = 90f
    private var currentBubble = Bubble(0f, 0f, 0, false)
    private var nextBubble = Bubble(0f, 0f, 0, true)
    private var shootingBubble = Bubble(0f, 0f, 0, false)

    private var bombButtonCenterX = 0f
    private var bombButtonCenterY = 0f
    private var rainbowButtonCenterX = 0f
    private var rainbowButtonCenterY = 0f
    private var powerupButtonRadius = 0f

    private var animationTime = 0L
    private var cluster = mutableListOf<Tile>()
    private var floatingClusters = mutableListOf<List<Tile>>()

    private val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val neighborOffsets = arrayOf(
        arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, -1)),
        arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(0, -1), intArrayOf(1, -1))
    )

    data class Tile(var type: Int, var removed: Boolean = false, var shift: Float = 0f,
                    var velocity: Float = 0f, var alpha: Float = 1f, var processed: Boolean = false,
                    var x: Int = 0, var y: Int = 0)

    data class Bubble(var x: Float, var y: Float, var type: Int, var visible: Boolean,
                      var angle: Float = 0f, var speed: Float = 1800f)

    init {
        initGame()
    }

    private fun draw3DBubble(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, color: Int, paint: Paint) {
        paint.color = color
        canvas.drawCircle(centerX, centerY, radius, paint)
        val highlightRadius = radius * 0.3f
        val highlightX = centerX - radius * 0.3f
        val highlightY = centerY - radius * 0.3f
        val gradient = RadialGradient(highlightX, highlightY, highlightRadius, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.shader = gradient
        canvas.drawCircle(highlightX, highlightY, highlightRadius, paint)
        paint.shader = null
        paint.color = Color.argb(50, 0, 0, 0)
        canvas.drawCircle(centerX + radius * 0.1f, centerY + radius * 0.1f, radius * 0.9f, paint)
        paint.shader = null
    }

    private fun initGame() {
        if (screenWidth == 0 || screenHeight == 0) return
        val availableWidth = screenWidth * 0.98f
        val availableHeight = screenHeight * 0.75f
        val maxTileWidthByScreen = availableWidth / LEVEL_COLUMNS
        val maxTileHeightByScreen = availableHeight / LEVEL_ROWS
        TILE_WIDTH = minOf(maxTileWidthByScreen, maxTileHeightByScreen)
        TILE_HEIGHT = TILE_WIDTH
        ROW_HEIGHT = TILE_HEIGHT * 0.87f
        BUBBLE_RADIUS = TILE_WIDTH * 0.45f
        levelWidth = LEVEL_COLUMNS * TILE_WIDTH
        levelHeight = (LEVEL_ROWS - 1) * ROW_HEIGHT + TILE_HEIGHT
        levelX = (screenWidth - levelWidth) / 2
        levelY = 150f
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) {
                tiles[i][j] = Tile(-1, x = i, y = j)
            }
        }
        playerX = levelX + levelWidth / 2 - TILE_WIDTH / 2
        playerY = levelY + levelHeight + 50f
        nextBubble.x = playerX - 2 * TILE_WIDTH
        nextBubble.y = playerY
        powerupButtonRadius = BUBBLE_RADIUS * 1.5f
        val buttonSpacing = screenWidth / 4f
        bombButtonCenterX = buttonSpacing
        bombButtonCenterY = screenHeight - powerupButtonRadius - (TILE_HEIGHT / 2)
        rainbowButtonCenterX = screenWidth - buttonSpacing
        rainbowButtonCenterY = screenHeight - powerupButtonRadius - (TILE_HEIGHT / 2)
        newGame()
    }

    private fun newGame() {
        score = 0
        turnCounter = 0
        rowOffset = 0
        gameState = GameState.READY
        hasTriggeredGameOver = false
        bombCount = 3
        rainbowCount = 3
        createLevel()
        nextBubble()
        nextBubble()
    }

    private fun createLevel() {
        for (j in 0 until LEVEL_ROWS / 2) {
            var randomType = Random.nextInt(BUBBLE_COLORS.size)
            var count = 0
            for (i in 0 until LEVEL_COLUMNS) {
                if (count >= 2) {
                    var newType = Random.nextInt(BUBBLE_COLORS.size)
                    if (newType == randomType) newType = (newType + 1) % BUBBLE_COLORS.size
                    randomType = newType
                    count = 0
                }
                count++
                tiles[i][j].type = randomType
            }
        }
        for (j in LEVEL_ROWS / 2 until LEVEL_ROWS) {
            for (i in 0 until LEVEL_COLUMNS) tiles[i][j].type = -1
        }
    }

    private fun nextBubble() {
        currentBubble.type = nextBubble.type
        currentBubble.visible = true
        val existingColors = findExistingColors()
        nextBubble.type = if (existingColors.isNotEmpty()) existingColors[Random.nextInt(existingColors.size)] else Random.nextInt(BUBBLE_COLORS.size)
    }

    private fun findExistingColors(): List<Int> {
        val colors = mutableSetOf<Int>()
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) {
                if (tiles[i][j].type >= 0) colors.add(tiles[i][j].type)
            }
        }
        return colors.toList()
    }

    fun setRunning(running: Boolean) { this.running = running }
    fun setSurfaceSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        initGame()
    }
    fun pause() { paused = true }
    fun unpause() { paused = false }

    fun doTouchEvent(event: MotionEvent): Boolean {
        if (gameState == GameState.GAME_OVER) {
            if (event.action == MotionEvent.ACTION_DOWN) newGame()
            return true
        }
        val touchX = event.x
        val touchY = event.y
        if (gameState == GameState.READY && event.action == MotionEvent.ACTION_DOWN) {
            val dxBomb = touchX - bombButtonCenterX
            val dyBomb = touchY - bombButtonCenterY
            if (dxBomb * dxBomb + dyBomb * dyBomb < powerupButtonRadius * powerupButtonRadius) {
                if (bombCount > 0) {
                    currentBubble.type = BOMB_BUBBLE_TYPE
                    bombCount--
                }
                return true
            }
            val dxRainbow = touchX - rainbowButtonCenterX
            val dyRainbow = touchY - rainbowButtonCenterY
            if (dxRainbow * dxRainbow + dyRainbow * dyRainbow < powerupButtonRadius * powerupButtonRadius) {
                if (rainbowCount > 0) {
                    currentBubble.type = RAINBOW_BUBBLE_TYPE
                    rainbowCount--
                }
                return true
            }
        }
        if (gameState != GameState.READY) return false
        if (touchY < playerY) {
            val dx = touchX - (playerX + TILE_WIDTH / 2)
            val dy = touchY - (playerY + TILE_HEIGHT / 2)
            var angle = Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble())).toFloat()
            if (angle < 10) angle = 10f
            if (angle > 170) angle = 170f
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> playerAngle = angle
                MotionEvent.ACTION_UP -> {
                    playerAngle = angle
                    shootBubble()
                }
            }
            return true
        }
        return false
    }

    private fun shootBubble() {
        shootingBubble.x = playerX
        shootingBubble.y = playerY
        shootingBubble.angle = playerAngle
        shootingBubble.type = currentBubble.type
        shootingBubble.visible = true
        currentBubble.visible = false
        gameState = GameState.SHOOT_BUBBLE
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (running) {
            val currentTime = System.nanoTime()
            val deltaTimeNanos = currentTime - lastTime
            lastTime = currentTime
            if (!paused) {
                val deltaTimeSeconds = deltaTimeNanos / 1000000000.0f
                val clampedDeltaTime = minOf(deltaTimeSeconds, 0.1f)
                if (clampedDeltaTime > 0) {
                    update(clampedDeltaTime)
                    draw()
                }
            }
            try { Thread.sleep(1) } catch (e: InterruptedException) { break }
        }
    }

    private fun update(deltaTime: Float) {
        when (gameState) {
            GameState.SHOOT_BUBBLE -> updateShootBubble(deltaTime)
            GameState.REMOVE_CLUSTER -> updateRemoveCluster(deltaTime)
            else -> {}
        }
    }

    private fun updateShootBubble(deltaTime: Float) {
        shootingBubble.x += deltaTime * shootingBubble.speed * cos(Math.toRadians(shootingBubble.angle.toDouble())).toFloat()
        shootingBubble.y += deltaTime * shootingBubble.speed * -sin(Math.toRadians(shootingBubble.angle.toDouble())).toFloat()
        val bubbleRadius = BUBBLE_RADIUS
        val leftBound = maxOf(levelX, bubbleRadius)
        val rightBound = minOf(levelX + levelWidth, screenWidth.toFloat() - bubbleRadius)
        if (shootingBubble.x <= leftBound) {
            shootingBubble.angle = 180 - shootingBubble.angle
            shootingBubble.x = leftBound
        } else if (shootingBubble.x >= rightBound) {
            shootingBubble.angle = 180 - shootingBubble.angle
            shootingBubble.x = rightBound
        }
        if (shootingBubble.y <= levelY) {
            shootingBubble.y = levelY
            snapBubble()
            return
        }
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) {
                val tile = tiles[i][j]
                if (tile.type < 0) continue
                val coord = getTileCoordinate(i, j)
                if (circleIntersection(shootingBubble.x + TILE_WIDTH / 2, shootingBubble.y + TILE_HEIGHT / 2, BUBBLE_RADIUS, coord.first + TILE_WIDTH / 2, coord.second + TILE_HEIGHT / 2, BUBBLE_RADIUS)) {
                    snapBubble()
                    return
                }
            }
        }
    }

    private fun updateRemoveCluster(deltaTime: Float) {
        animationTime += (deltaTime * 1000).toLong()
        var tilesLeft = false
        var clusterFinished = true
        for (tile in cluster) {
            if (tile.type >= 0) {
                tilesLeft = true
                clusterFinished = false
                tile.alpha -= deltaTime * 15
                if (tile.alpha <= 0) {
                    tile.alpha = 0f
                    tile.type = -1
                    tile.alpha = 1f
                }
            }
        }
        if (clusterFinished && cluster.isNotEmpty()) {
            cluster.clear()
            val newFloatingClusters = findFloatingClusters().toMutableList()
            for (newCluster in newFloatingClusters) {
                var alreadyExists = false
                for (existingCluster in floatingClusters) {
                    if (existingCluster.any { existing -> newCluster.any { new -> existing.x == new.x && existing.y == new.y } }) {
                        alreadyExists = true
                        break
                    }
                }
                if (!alreadyExists) {
                    floatingClusters.add(newCluster)
                    score += newCluster.size * 100
                    for (tile in newCluster) {
                        tile.shift = 1f
                        tile.velocity = 900f
                    }
                }
            }
        }
        for (floatingCluster in floatingClusters) {
            for (tile in floatingCluster) {
                if (tile.type >= 0) {
                    tilesLeft = true
                    tile.velocity += deltaTime * 700
                    tile.shift += deltaTime * tile.velocity
                    tile.alpha -= deltaTime * 8
                    if (tile.alpha <= 0 || tile.y * ROW_HEIGHT + tile.shift > (LEVEL_ROWS - 1) * ROW_HEIGHT + TILE_HEIGHT) {
                        tile.type = -1
                        tile.shift = 0f
                        tile.alpha = 1f
                    }
                }
            }
        }
        if (!tilesLeft) {
            turnCounter++
            if (turnCounter % 3 == 0) addNewRow()
            nextBubble()
            checkGameOver()
            if (gameState != GameState.GAME_OVER) {
                gameState = GameState.READY
            }
        }
    }

    private fun snapBubble() {
        val centerX = shootingBubble.x + BUBBLE_RADIUS
        val centerY = shootingBubble.y + BUBBLE_RADIUS
        val gridPos = getGridPosition(centerX, centerY)
        val clampedX = gridPos.first.coerceIn(0, LEVEL_COLUMNS - 1)
        val clampedY = gridPos.second.coerceIn(0, LEVEL_ROWS - 1)
        var addTile = false
        var finalY = clampedY
        if (tiles[clampedX][clampedY].type != -1) {
            for (newRow in clampedY + 1 until LEVEL_ROWS) {
                if (tiles[clampedX][newRow].type == -1) {
                    finalY = newRow
                    addTile = true
                    break
                }
            }
        } else {
            addTile = true
        }
        if (addTile) {
            shootingBubble.visible = false
            val bubbleType = shootingBubble.type
            tiles[clampedX][finalY].type = bubbleType

            if (checkGameOver()) return

            if (bubbleType == BOMB_BUBBLE_TYPE) {
                handleBomb(clampedX, finalY)
                tiles[clampedX][finalY].type = -1
                score += cluster.size * 100
                gameState = GameState.REMOVE_CLUSTER
                animationTime = 0
                return
            }
            if (bubbleType == RAINBOW_BUBBLE_TYPE) {
                val newType = handleRainbow(clampedX, finalY)
                tiles[clampedX][finalY].type = newType
            }
            cluster = findCluster(clampedX, finalY, true, true, false).toMutableList()
            if (cluster.size >= 3) {
                score += cluster.size * 100
                for (tile in cluster) tile.removed = true
                gameState = GameState.REMOVE_CLUSTER
                animationTime = 0
                return
            }
            floatingClusters = findFloatingClusters().toMutableList()
            if (floatingClusters.isNotEmpty()) {
                for (floatingCluster in floatingClusters) {
                    score += floatingCluster.size * 100
                    for (tile in floatingCluster) {
                        tile.shift = 1f
                        tile.velocity = 900f
                    }
                }
                gameState = GameState.REMOVE_CLUSTER
                animationTime = 0
                return
            }
        }
        turnCounter++
        if (turnCounter % 3 == 0) addNewRow()
        nextBubble()

        if (!checkGameOver()) {
            gameState = GameState.READY
        }
    }

    private fun handleBomb(x: Int, y: Int) {
        val neighbors = getNeighbors(tiles[x][y])
        for (neighbor in neighbors) {
            if (neighbor.type != -1 && !neighbor.removed) {
                neighbor.removed = true
                cluster.add(neighbor)
            }
        }
    }

    private fun handleRainbow(x: Int, y: Int): Int {
        val neighbors = getNeighbors(tiles[x][y])
        val neighborColors = neighbors.map { it.type }.filter { it >= 0 }
        if (neighborColors.isEmpty()) return Random.nextInt(BUBBLE_COLORS.size)
        val colorCounts = neighborColors.groupBy { it }.mapValues { it.value.size }
        return colorCounts.maxByOrNull { it.value }?.key ?: Random.nextInt(BUBBLE_COLORS.size)
    }

    private fun addNewRow() {
        for (i in 0 until LEVEL_COLUMNS) {
            if (tiles[i][LEVEL_ROWS - 1].type != -1) {
                setGameOver()
                return
            }
        }
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in LEVEL_ROWS - 1 downTo 1) {
                val sourceTile = tiles[i][j - 1]
                val targetTile = tiles[i][j]
                targetTile.type = sourceTile.type
                targetTile.removed = sourceTile.removed
                targetTile.shift = sourceTile.shift
                targetTile.velocity = sourceTile.velocity
                targetTile.alpha = sourceTile.alpha
                targetTile.processed = sourceTile.processed
            }
        }
        val existingColors = findExistingColors()
        for (i in 0 until LEVEL_COLUMNS) {
            val newTile = tiles[i][0]
            newTile.type = if (existingColors.isNotEmpty()) existingColors[Random.nextInt(existingColors.size)] else Random.nextInt(BUBBLE_COLORS.size)
            newTile.removed = false
            newTile.shift = 0f
            newTile.velocity = 0f
            newTile.alpha = 1f
            newTile.processed = false
        }
        rowOffset = (rowOffset + 1) % 2
    }

    private fun checkGameOver(): Boolean {
        for (i in 0 until LEVEL_COLUMNS) {
            if (tiles[i][LEVEL_ROWS - 1].type != -1) {
                setGameOver()
                return true
            }
        }
        return false
    }

    private fun setGameOver() {
        gameState = GameState.GAME_OVER
        if (!hasTriggeredGameOver) {
            hasTriggeredGameOver = true
            mainHandler.post {
                onGameOver(score)
            }
        }
    }

    private fun findCluster(startX: Int, startY: Int, matchType: Boolean, reset: Boolean, skipRemoved: Boolean): List<Tile> {
        if (reset) resetProcessed()
        val targetTile = tiles[startX][startY]
        val toProcess = mutableListOf(targetTile)
        val foundCluster = mutableListOf<Tile>()
        targetTile.processed = true
        while (toProcess.isNotEmpty()) {
            val currentTile = toProcess.removeAt(toProcess.size - 1)
            if (currentTile.type == -1) continue
            if (skipRemoved && currentTile.removed) continue
            if (!matchType || currentTile.type == targetTile.type) {
                foundCluster.add(currentTile)
                val neighbors = getNeighbors(currentTile)
                for (neighbor in neighbors) {
                    if (!neighbor.processed) {
                        toProcess.add(neighbor)
                        neighbor.processed = true
                    }
                }
            }
        }
        return foundCluster
    }

    private fun findFloatingClusters(): List<List<Tile>> {
        val connected = Array(LEVEL_COLUMNS) { Array(LEVEL_ROWS) { false } }
        for (i in 0 until LEVEL_COLUMNS) {
            if (tiles[i][0].type >= 0) floodFillConnected(i, 0, connected)
        }
        resetProcessed()
        val foundClusters = mutableListOf<List<Tile>>()
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) {
                val tile = tiles[i][j]
                if (tile.type >= 0 && !connected[i][j] && !tile.processed) {
                    val foundCluster = findCluster(i, j, false, false, true)
                    if (foundCluster.isNotEmpty()) foundClusters.add(foundCluster)
                }
            }
        }
        return foundClusters
    }

    private fun floodFillConnected(startX: Int, startY: Int, connected: Array<Array<Boolean>>) {
        if (connected[startX][startY] || tiles[startX][startY].type < 0) return
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addLast(Pair(startX, startY))
        connected[startX][startY] = true
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            val tileRow = (y + rowOffset) % 2
            val offsets = neighborOffsets[tileRow]
            for (offset in offsets) {
                val nx = x + offset[0]
                val ny = y + offset[1]
                if (nx < 0 || nx >= LEVEL_COLUMNS || ny < 0 || ny >= LEVEL_ROWS) continue
                if (!connected[nx][ny] && tiles[nx][ny].type >= 0) {
                    connected[nx][ny] = true
                    queue.addLast(Pair(nx, ny))
                }
            }
        }
    }

    private fun getNeighbors(tile: Tile): List<Tile> {
        val tileRow = (tile.y + rowOffset) % 2
        val neighbors = mutableListOf<Tile>()
        val offsets = neighborOffsets[tileRow]
        for (offset in offsets) {
            val nx = tile.x + offset[0]
            val ny = tile.y + offset[1]
            if (nx >= 0 && nx < LEVEL_COLUMNS && ny >= 0 && ny < LEVEL_ROWS) {
                neighbors.add(tiles[nx][ny])
            }
        }
        return neighbors
    }

    private fun resetProcessed() {
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) tiles[i][j].processed = false
        }
    }

    private fun getTileCoordinate(column: Int, row: Int): Pair<Float, Float> {
        var tileX = levelX + column * TILE_WIDTH
        if ((row + rowOffset) % 2 == 1) tileX += TILE_WIDTH / 2
        val tileY = levelY + row * ROW_HEIGHT
        return Pair(tileX, tileY)
    }

    private fun getGridPosition(x: Float, y: Float): Pair<Int, Int> {
        val gridY = ((y - levelY) / ROW_HEIGHT).toInt().coerceIn(0, LEVEL_ROWS - 1)
        var xOffset = 0f
        if ((gridY + rowOffset) % 2 == 1) xOffset = TILE_WIDTH / 2
        val gridX = (((x - xOffset) - levelX) / TILE_WIDTH).toInt().coerceIn(0, LEVEL_COLUMNS - 1)
        return Pair(gridX, gridY)
    }

    private fun circleIntersection(x1: Float, y1: Float, r1: Float, x2: Float, y2: Float, r2: Float): Boolean {
        val dx = x1 - x2
        val dy = y1 - y2
        val distance = sqrt(dx * dx + dy * dy)
        return distance < (r1 + r2)
    }

    private fun draw() {
        val canvas = surfaceHolder.lockCanvas() ?: return
        try {
            val gradient = LinearGradient(0f, 0f, 0f, screenHeight.toFloat(), Color.rgb(20, 30, 60), Color.rgb(10, 15, 30), Shader.TileMode.CLAMP)
            paint.shader = gradient
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            paint.shader = null
            paint.color = Color.argb(100, 255, 255, 255)
            canvas.drawRoundRect(levelX - 10, levelY - 10, levelX + levelWidth + 20, levelY + levelHeight + 10, 20f, 20f, paint)
            drawTiles(canvas)
            drawPlayer(canvas)
            if (shootingBubble.visible) drawBubble(canvas, shootingBubble.x + TILE_WIDTH / 2, shootingBubble.y + TILE_HEIGHT / 2, BUBBLE_RADIUS, shootingBubble.type)
            drawUI(canvas)
            if (gameState == GameState.GAME_OVER) drawGameOver(canvas)
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawTiles(canvas: Canvas) {
        for (j in 0 until LEVEL_ROWS) {
            for (i in 0 until LEVEL_COLUMNS) {
                val tile = tiles[i][j]
                if (tile.type != -1) {
                    val coord = getTileCoordinate(i, j)
                    val oldAlpha = paint.alpha
                    paint.alpha = (tile.alpha * 255).toInt()
                    drawBubble(canvas, coord.first + TILE_WIDTH / 2, coord.second + TILE_HEIGHT / 2 + tile.shift, BUBBLE_RADIUS, tile.type)
                    paint.alpha = oldAlpha
                }
            }
        }
    }

    private fun drawBubble(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, type: Int) {
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val originalAlpha = paint.alpha
        tempPaint.alpha = originalAlpha
        when (type) {
            BOMB_BUBBLE_TYPE -> {
                tempPaint.color = Color.BLACK
                canvas.drawCircle(centerX, centerY, radius, tempPaint)
                val highlightRadius = radius * 0.3f
                val highlightX = centerX - radius * 0.3f
                val highlightY = centerY - radius * 0.3f
                val gradient = RadialGradient(highlightX, highlightY, highlightRadius, Color.rgb(150, 150, 150), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                tempPaint.shader = gradient
                canvas.drawCircle(highlightX, highlightY, highlightRadius, tempPaint)
                tempPaint.shader = null
                tempPaint.alpha = originalAlpha
                tempPaint.color = Color.rgb(150, 75, 0)
                tempPaint.strokeWidth = radius * 0.2f
                val fuseStartX = centerX + radius * 0.7f
                val fuseStartY = centerY - radius * 0.7f
                val fuseEndX = centerX + radius * 0.9f
                val fuseEndY = centerY - radius * 0.9f
                canvas.drawLine(fuseStartX, fuseStartY, fuseEndX, fuseEndY, tempPaint)
                tempPaint.color = Color.YELLOW
                canvas.drawCircle(fuseEndX, fuseEndY, radius * 0.15f, tempPaint)
            }
            RAINBOW_BUBBLE_TYPE -> {
                tempPaint.color = Color.WHITE
                canvas.drawCircle(centerX, centerY, radius, tempPaint)
                tempPaint.alpha = originalAlpha
                tempPaint.style = Paint.Style.STROKE
                tempPaint.strokeWidth = radius * 0.25f
                val rect = RectF(centerX - radius * 0.7f, centerY - radius * 0.7f, centerX + radius * 0.7f, centerY + radius * 0.7f)
                tempPaint.color = Color.RED
                canvas.drawArc(rect, 180f, 180f, false, tempPaint)
                tempPaint.color = Color.GREEN
                rect.inset(radius * 0.25f, radius * 0.25f)
                canvas.drawArc(rect, 180f, 180f, false, tempPaint)
                tempPaint.color = Color.BLUE
                rect.inset(radius * 0.25f, radius * 0.25f)
                canvas.drawArc(rect, 180f, 180f, false, tempPaint)
            }
            else -> {
                if (type < 0 || type >= BUBBLE_COLORS.size) return
                draw3DBubble(canvas, centerX, centerY, radius, BUBBLE_COLORS[type], tempPaint)
            }
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        val centerX = playerX + TILE_WIDTH / 2
        val centerY = playerY + TILE_HEIGHT / 2
        paint.color = Color.DKGRAY
        canvas.drawCircle(centerX, centerY, BUBBLE_RADIUS + 12, paint)
        paint.color = Color.BLUE
        paint.strokeWidth = 4f
        val lineLength = 1.5f * TILE_WIDTH
        val endX = centerX + lineLength * cos(Math.toRadians(playerAngle.toDouble())).toFloat()
        val endY = centerY - lineLength * sin(Math.toRadians(playerAngle.toDouble())).toFloat()
        canvas.drawLine(centerX, centerY, endX, endY, paint)
        if (currentBubble.visible) drawBubble(canvas, centerX, centerY, BUBBLE_RADIUS, currentBubble.type)
        drawBubble(canvas, nextBubble.x + TILE_WIDTH / 2, nextBubble.y + TILE_HEIGHT / 2, BUBBLE_RADIUS, nextBubble.type)
    }

    private fun drawUI(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.textSize = 48f
        paint.style = Paint.Style.FILL
        canvas.drawText("Score: $score", screenWidth / 2f, 120f, paint)
        drawPowerupButton(canvas, bombButtonCenterX, bombButtonCenterY, bombCount, BOMB_BUBBLE_TYPE)
        drawPowerupButton(canvas, rainbowButtonCenterX, rainbowButtonCenterY, rainbowCount, RAINBOW_BUBBLE_TYPE)
    }

    private fun drawPowerupButton(canvas: Canvas, centerX: Float, centerY: Float, count: Int, type: Int) {
        val tempPaint = Paint(paint)
        val alpha = if (count > 0) 255 else 100
        tempPaint.alpha = alpha
        paint.alpha = alpha
        tempPaint.color = Color.WHITE
        tempPaint.style = Paint.Style.STROKE
        tempPaint.strokeWidth = 5f
        canvas.drawCircle(centerX, centerY, powerupButtonRadius, tempPaint)
        tempPaint.style = Paint.Style.FILL
        tempPaint.color = Color.DKGRAY
        canvas.drawCircle(centerX, centerY, powerupButtonRadius - 3f, tempPaint)
        drawBubble(canvas, centerX, centerY, BUBBLE_RADIUS, type)
        tempPaint.style = Paint.Style.FILL
        tempPaint.color = Color.WHITE
        tempPaint.textSize = 40f
        tempPaint.alpha = alpha
        canvas.drawText(count.toString(), centerX + powerupButtonRadius * 0.7f, centerY + powerupButtonRadius * 0.7f, tempPaint)
        paint.alpha = 255
    }

    private fun drawGameOver(canvas: Canvas) {
        paint.color = Color.argb(200, 0, 0, 0)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
        paint.color = Color.WHITE
        paint.textSize = 72f
        canvas.drawText("Game Over!", screenWidth / 2f, screenHeight / 2f - 50, paint)
        paint.textSize = 48f
        canvas.drawText("Final Score: $score", screenWidth / 2f, screenHeight / 2f + 20, paint)
        canvas.drawText("Tap to restart", screenWidth / 2f, screenHeight / 2f + 80, paint)
    }
}