package com.example.gamin.BubbleShooter

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.example.gamin.R
import kotlin.math.*
import kotlin.random.Random

class BubbleShooterActivity : Activity() {
    private lateinit var gameView: BubbleShooterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = BubbleShooterView(this)
        setContentView(gameView)
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
}

class BubbleShooterView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val thread: BubbleShooterThread
    private var screenWidth = 0
    private var screenHeight = 0
    
    init {
        holder.addCallback(this)
        thread = BubbleShooterThread(holder, context)
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

class BubbleShooterThread(private val surfaceHolder: SurfaceHolder, private val context: Context) : Thread() {
    private var running = false
    private var paused = false
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Game constants
    private val BUBBLE_COLORS = arrayOf(
        Color.RED,
        Color.GREEN, 
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN,
        Color.rgb(255, 165, 0) // Orange
    )
    private val LEVEL_COLUMNS = 12
    private val LEVEL_ROWS = 16
    private var TILE_WIDTH = 60f
    private var TILE_HEIGHT = 60f
    private var ROW_HEIGHT = 52f
    private var BUBBLE_RADIUS = 28f
    
    // Game state
    private enum class GameState { READY, SHOOT_BUBBLE, REMOVE_CLUSTER, GAME_OVER }
    private var gameState = GameState.READY
    private var score = 0
    private var turnCounter = 0
    private var rowOffset = 0
    private var level = 1
    
    // Level data
    private var levelX = 0f
    private var levelY = 0f
    private var levelWidth = 0f
    private var levelHeight = 0f
    private val tiles = Array(LEVEL_COLUMNS) { Array(LEVEL_ROWS) { Tile(-1) } }
    
    // Player data
    private var playerX = 0f
    private var playerY = 0f
    private var playerAngle = 90f
    private var currentBubble = Bubble(0f, 0f, 0, false)
    private var nextBubble = Bubble(0f, 0f, 0, true)
    private var shootingBubble = Bubble(0f, 0f, 0, false)
    
    // Animation
    private var animationTime = 0L
    private var cluster = mutableListOf<Tile>()
    private var floatingClusters = mutableListOf<List<Tile>>()
    
    // Graphics
    private val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private var bubbleSprites: Bitmap? = null
    private val neighborOffsets = arrayOf(
        arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, -1)), // Even row
        arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(0, -1), intArrayOf(1, -1))  // Odd row
    )

    data class Tile(var type: Int, var removed: Boolean = false, var shift: Float = 0f, 
                   var velocity: Float = 0f, var alpha: Float = 1f, var processed: Boolean = false,
                   var x: Int = 0, var y: Int = 0)

    data class Bubble(var x: Float, var y: Float, var type: Int, var visible: Boolean,
                     var angle: Float = 0f, var speed: Float = 3000f)

    init {
        loadAssets()
        initGame()
    }

    private fun loadAssets() {
        try {
            bubbleSprites = BitmapFactory.decodeResource(context.resources, R.drawable.bubble_sprites)
        } catch (e: Exception) {
            // Create a simple colored bubble if sprite not found
            bubbleSprites = createBubbleSprite()
        }
    }

    private fun createBubbleSprite(): Bitmap {
        val bitmap = Bitmap.createBitmap(TILE_WIDTH.toInt(), TILE_HEIGHT.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        for (i in 0 until BUBBLE_COLORS.size) {
            draw3DBubble(canvas, i * TILE_WIDTH + TILE_WIDTH / 2, TILE_HEIGHT / 2, BUBBLE_RADIUS, BUBBLE_COLORS[i], paint)
        }
        return bitmap
    }
    
    private fun draw3DBubble(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, color: Int, paint: Paint) {
        // Draw main bubble
        paint.color = color
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Draw highlight for 3D effect
        val highlightRadius = radius * 0.3f
        val highlightX = centerX - radius * 0.3f
        val highlightY = centerY - radius * 0.3f
        
        val gradient = RadialGradient(
            highlightX, highlightY, highlightRadius,
            Color.WHITE, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawCircle(highlightX, highlightY, highlightRadius, paint)
        
        // Draw shadow for depth
        paint.shader = null
        paint.color = Color.argb(50, 0, 0, 0)
        canvas.drawCircle(centerX + radius * 0.1f, centerY + radius * 0.1f, radius * 0.9f, paint)
        
        // Reset paint
        paint.shader = null
    }

    private fun initGame() {
        if (screenWidth == 0 || screenHeight == 0) return
        
        // Make game responsive to screen size - use more screen space
        val availableWidth = screenWidth * 0.98f
        val availableHeight = screenHeight * 0.75f
        
        // Calculate optimal bubble size based on screen constraints
        val maxTileWidthByScreen = availableWidth / LEVEL_COLUMNS
        val maxTileHeightByScreen = availableHeight / LEVEL_ROWS
        
        // Use the smaller constraint to ensure everything fits
        TILE_WIDTH = minOf(maxTileWidthByScreen, maxTileHeightByScreen)
        TILE_HEIGHT = TILE_WIDTH
        ROW_HEIGHT = TILE_HEIGHT * 0.87f
        BUBBLE_RADIUS = TILE_WIDTH * 0.45f
        
        // Calculate level dimensions to center on screen
        levelWidth = LEVEL_COLUMNS * TILE_WIDTH
        levelHeight = (LEVEL_ROWS - 1) * ROW_HEIGHT + TILE_HEIGHT
        levelX = (screenWidth - levelWidth) / 2
        levelY = screenHeight * 0.05f
        
        // Initialize tiles
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) {
                tiles[i][j] = Tile(-1, x = i, y = j)
            }
        }
        
        // Set player position
        playerX = levelX + levelWidth / 2 - TILE_WIDTH / 2
        playerY = levelY + levelHeight + 50f
        
        // Initialize bubbles
        nextBubble.x = playerX - 2 * TILE_WIDTH
        nextBubble.y = playerY
        
        newGame()
    }

    private fun newGame() {
        score = 0
        turnCounter = 0
        rowOffset = 0
        gameState = GameState.READY
        createLevel()
        nextBubble()
        nextBubble()
    }

    private fun createLevel() {
        // Create initial level with random bubbles
        for (j in 0 until LEVEL_ROWS / 2) {
            var randomType = Random.nextInt(BUBBLE_COLORS.size)
            var count = 0
            for (i in 0 until LEVEL_COLUMNS) {
                if (count >= 2) {
                    var newType = Random.nextInt(BUBBLE_COLORS.size)
                    if (newType == randomType) {
                        newType = (newType + 1) % BUBBLE_COLORS.size
                    }
                    randomType = newType
                    count = 0
                }
                count++
                tiles[i][j].type = randomType
            }
        }
        
        // Empty bottom half
        for (j in LEVEL_ROWS / 2 until LEVEL_ROWS) {
            for (i in 0 until LEVEL_COLUMNS) {
                tiles[i][j].type = -1
            }
        }
    }

    private fun nextBubble() {
        currentBubble.type = nextBubble.type
        currentBubble.visible = true
        
        val existingColors = findExistingColors()
        nextBubble.type = if (existingColors.isNotEmpty()) {
            existingColors[Random.nextInt(existingColors.size)]
        } else {
            Random.nextInt(BUBBLE_COLORS.size)
        }
    }

    private fun findExistingColors(): List<Int> {
        val colors = mutableSetOf<Int>()
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) {
                if (tiles[i][j].type >= 0) {
                    colors.add(tiles[i][j].type)
                }
            }
        }
        return colors.toList()
    }

    fun setRunning(running: Boolean) {
        this.running = running
    }

    fun setSurfaceSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        initGame()
    }

    fun pause() {
        paused = true
    }

    fun unpause() {
        paused = false
    }

    fun doTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (gameState == GameState.GAME_OVER) {
                // Restart game
                newGame()
                return true
            } else if (gameState == GameState.READY) {
                val dx = event.x - (playerX + TILE_WIDTH / 2)
                val dy = event.y - (playerY + TILE_HEIGHT / 2)
                playerAngle = Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble())).toFloat()
                
                // Limit angle
                if (playerAngle < 10) playerAngle = 10f
                if (playerAngle > 170) playerAngle = 170f
                
                shootBubble()
            }
        }
        return true
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
        val targetFPS = 60.0
        val targetTime = 1000000000 / targetFPS
        
        while (running) {
            val currentTime = System.nanoTime()
            val deltaTime = currentTime - lastTime
            
            if (!paused && deltaTime >= targetTime) {
                update()
                draw()
                lastTime = currentTime
            } else {
                try {
                    Thread.sleep(1) // Small sleep to prevent busy waiting
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun update() {
        val deltaTime = 0.016f // 60 FPS = ~16ms per frame
        when (gameState) {
            GameState.SHOOT_BUBBLE -> updateShootBubble(deltaTime)
            GameState.REMOVE_CLUSTER -> updateRemoveCluster(deltaTime)
            else -> {}
        }
    }

    private fun updateShootBubble(deltaTime: Float) {
        // Move bubble
        shootingBubble.x += deltaTime * shootingBubble.speed * cos(Math.toRadians(shootingBubble.angle.toDouble())).toFloat()
        shootingBubble.y += deltaTime * shootingBubble.speed * -sin(Math.toRadians(shootingBubble.angle.toDouble())).toFloat()
        
        // Wall collisions - Fix boundary detection with screen bounds
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
        
        // Top collision
        if (shootingBubble.y <= levelY) {
            shootingBubble.y = levelY
            snapBubble()
            return
        }
        
        // Bubble collisions
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) {
                val tile = tiles[i][j]
                if (tile.type < 0) continue
                
                val coord = getTileCoordinate(i, j)
                if (circleIntersection(
                    shootingBubble.x + TILE_WIDTH / 2, shootingBubble.y + TILE_HEIGHT / 2, BUBBLE_RADIUS,
                    coord.first + TILE_WIDTH / 2, coord.second + TILE_HEIGHT / 2, BUBBLE_RADIUS
                )) {
                    snapBubble()
                    return
                }
            }
        }
    }

    private fun updateRemoveCluster(deltaTime: Float) {
        animationTime += (deltaTime * 1000).toLong()
        
        // Remove cluster bubbles with fade effect
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
        
        // If cluster removal is finished, check for new floating clusters
        if (clusterFinished && cluster.isNotEmpty()) {
            cluster.clear()
            val newFloatingClusters = findFloatingClusters().toMutableList()
            
            // Add any new floating clusters to the existing ones
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
                    // Add points and initialize falling animation
                    score += newCluster.size * 100
                    for (tile in newCluster) {
                        tile.shift = 1f
                        tile.velocity = 900f
                    }
                }
            }
        }
        
        // Drop floating clusters
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
            
            // Add new row every 8 shots for balanced difficulty
            if (turnCounter % 8 == 0) {
                addNewRow()
            }
            
            nextBubble()
            if (checkGameOver()) {
                gameState = GameState.GAME_OVER
            } else {
                gameState = GameState.READY
            }
        }
    }

    private fun snapBubble() {
        val centerX = shootingBubble.x + BUBBLE_RADIUS
        val centerY = shootingBubble.y + BUBBLE_RADIUS
        val gridPos = getGridPosition(centerX, centerY)
        
        // Clamp grid position to ensure it's within bounds
        val clampedX = gridPos.first.coerceIn(0, LEVEL_COLUMNS - 1)
        val clampedY = gridPos.second.coerceIn(0, LEVEL_ROWS - 1)
        
        // Find empty spot
        var addTile = false
        var finalY = clampedY
        
        if (tiles[clampedX][clampedY].type != -1) {
            // Find empty spot below
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
            tiles[clampedX][finalY].type = shootingBubble.type
            
            if (checkGameOver()) return
            
            // Find cluster
            cluster = findCluster(clampedX, finalY, true, true, false).toMutableList()
            
            if (cluster.size >= 3) {
                // Remove cluster
                score += cluster.size * 100
                
                // Mark tiles as removed
                for (tile in cluster) {
                    tile.removed = true
                }
                
                gameState = GameState.REMOVE_CLUSTER
                animationTime = 0
                return
            }
            
            // Always check for floating clusters after adding a bubble
            floatingClusters = findFloatingClusters().toMutableList()
            
            if (floatingClusters.isNotEmpty()) {
                // Add points for floating clusters
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
        
        // No cluster found - turnCounter will be handled in updateRemoveCluster
        
        nextBubble()
        gameState = GameState.READY
    }


    private fun addNewRow() {
        // Check if bottom row has bubbles - if so, game over
        for (i in 0 until LEVEL_COLUMNS) {
            if (tiles[i][LEVEL_ROWS - 1].type != -1) {
                gameState = GameState.GAME_OVER
                return
            }
        }
        
        // Shift all rows down, preserving tile properties
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in LEVEL_ROWS - 1 downTo 1) {
                val sourceTile = tiles[i][j - 1]
                val targetTile = tiles[i][j]
                
                // Copy all properties
                targetTile.type = sourceTile.type
                targetTile.removed = sourceTile.removed
                targetTile.shift = sourceTile.shift
                targetTile.velocity = sourceTile.velocity
                targetTile.alpha = sourceTile.alpha
                targetTile.processed = sourceTile.processed
            }
        }
        
        // Add new row at top with random colors from existing bubbles
        val existingColors = findExistingColors()
        for (i in 0 until LEVEL_COLUMNS) {
            val newTile = tiles[i][0]
            newTile.type = if (existingColors.isNotEmpty()) {
                existingColors[Random.nextInt(existingColors.size)]
            } else {
                Random.nextInt(BUBBLE_COLORS.size)
            }
            // Reset tile properties
            newTile.removed = false
            newTile.shift = 0f
            newTile.velocity = 0f
            newTile.alpha = 1f
            newTile.processed = false
        }
        
        // Update row offset for proper hexagonal pattern
        rowOffset = (rowOffset + 1) % 2
    }

    private fun checkGameOver(): Boolean {
        for (i in 0 until LEVEL_COLUMNS) {
            if (tiles[i][LEVEL_ROWS - 1].type != -1) {
                gameState = GameState.GAME_OVER
                return true
            }
        }
        return false
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
        // Mark all bubbles that are connected to the top row
        val connected = Array(LEVEL_COLUMNS) { Array(LEVEL_ROWS) { false } }
        
        // Start flood fill from all bubbles in the top row
        for (i in 0 until LEVEL_COLUMNS) {
            if (tiles[i][0].type >= 0) {
                floodFillConnected(i, 0, connected)
            }
        }
        
        // Find clusters of unconnected bubbles
        resetProcessed()
        val foundClusters = mutableListOf<List<Tile>>()
        
        for (i in 0 until LEVEL_COLUMNS) {
            for (j in 0 until LEVEL_ROWS) {
                val tile = tiles[i][j]
                if (tile.type >= 0 && !connected[i][j] && !tile.processed) {
                    val foundCluster = findCluster(i, j, false, false, true)
                    if (foundCluster.isNotEmpty()) {
                        foundClusters.add(foundCluster)
                    }
                }
            }
        }
        
        return foundClusters
    }
    
    private fun floodFillConnected(x: Int, y: Int, connected: Array<Array<Boolean>>) {
        if (x < 0 || x >= LEVEL_COLUMNS || y < 0 || y >= LEVEL_ROWS) return
        if (tiles[x][y].type < 0 || connected[x][y]) return
        
        connected[x][y] = true
        
        // Get neighbor coordinates using the same logic as getNeighbors
        val tileRow = (y + rowOffset) % 2
        val offsets = neighborOffsets[tileRow]
        
        for (offset in offsets) {
            val nx = x + offset[0]
            val ny = y + offset[1]
            floodFillConnected(nx, ny, connected)
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
            for (j in 0 until LEVEL_ROWS) {
                tiles[i][j].processed = false
            }
        }
    }

    private fun getTileCoordinate(column: Int, row: Int): Pair<Float, Float> {
        var tileX = levelX + column * TILE_WIDTH
        
        if ((row + rowOffset) % 2 == 1) {
            tileX += TILE_WIDTH / 2
        }
        
        val tileY = levelY + row * ROW_HEIGHT
        return Pair(tileX, tileY)
    }

    private fun getGridPosition(x: Float, y: Float): Pair<Int, Int> {
        val gridY = ((y - levelY) / ROW_HEIGHT).toInt().coerceIn(0, LEVEL_ROWS - 1)
        
        var xOffset = 0f
        if ((gridY + rowOffset) % 2 == 1) {
            xOffset = TILE_WIDTH / 2
        }
        
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
            // Clear screen with gradient background
            val gradient = LinearGradient(
                0f, 0f, 0f, screenHeight.toFloat(),
                Color.rgb(20, 30, 60), Color.rgb(10, 15, 30),
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            paint.shader = null
            
            // Draw game area background
            paint.color = Color.argb(100, 255, 255, 255)
            canvas.drawRoundRect(levelX - 10, levelY - 10, levelX + levelWidth + 20, levelY + levelHeight + 10, 20f, 20f, paint)
            
            // Draw tiles
            drawTiles(canvas)
            
            // Draw player
            drawPlayer(canvas)
            
            // Draw shooting bubble
            if (shootingBubble.visible) {
                drawBubble(canvas, shootingBubble.x, shootingBubble.y, shootingBubble.type)
            }
            
            // Draw UI
            drawUI(canvas)
            
            // Draw game over screen
            if (gameState == GameState.GAME_OVER) {
                drawGameOver(canvas)
            }
            
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawTiles(canvas: Canvas) {
        for (j in 0 until LEVEL_ROWS) {
            for (i in 0 until LEVEL_COLUMNS) {
                val tile = tiles[i][j]
                if (tile.type >= 0) {
                    val coord = getTileCoordinate(i, j)
                    
                    // Apply transparency for fading effect
                    val oldAlpha = paint.alpha
                    paint.alpha = (tile.alpha * 255).toInt()
                    
                    drawBubble(canvas, coord.first, coord.second + tile.shift, tile.type)
                    
                    paint.alpha = oldAlpha
                }
            }
        }
    }

    private fun drawBubble(canvas: Canvas, x: Float, y: Float, type: Int) {
        if (type < 0 || type >= BUBBLE_COLORS.size) return
        
        // Draw 3D bubble directly
        val centerX = x + TILE_WIDTH / 2
        val centerY = y + TILE_HEIGHT / 2
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        draw3DBubble(canvas, centerX, centerY, BUBBLE_RADIUS, BUBBLE_COLORS[type], tempPaint)
    }

    private fun drawPlayer(canvas: Canvas) {
        val centerX = playerX + TILE_WIDTH / 2
        val centerY = playerY + TILE_HEIGHT / 2
        
        // Draw player base
        paint.color = Color.DKGRAY
        canvas.drawCircle(centerX, centerY, BUBBLE_RADIUS + 12, paint)
        
        // Draw aim line
        paint.color = Color.BLUE
        paint.strokeWidth = 4f
        val lineLength = 1.5f * TILE_WIDTH
        val endX = centerX + lineLength * cos(Math.toRadians(playerAngle.toDouble())).toFloat()
        val endY = centerY - lineLength * sin(Math.toRadians(playerAngle.toDouble())).toFloat()
        canvas.drawLine(centerX, centerY, endX, endY, paint)
        
        // Draw current bubble
        if (currentBubble.visible) {
            drawBubble(canvas, playerX, playerY, currentBubble.type)
        }
        
        // Draw next bubble
        drawBubble(canvas, nextBubble.x, nextBubble.y, nextBubble.type)
    }

    private fun drawUI(canvas: Canvas) {
        // Draw score
        paint.color = Color.WHITE
        paint.textSize = 48f
        canvas.drawText("Score: $score", screenWidth - 200f, 60f, paint)
        
        // Draw level
        canvas.drawText("Level: $level", 200f, 60f, paint)
    }

    private fun drawGameOver(canvas: Canvas) {
        // Semi-transparent overlay
        paint.color = Color.argb(200, 0, 0, 0)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
        
        // Game over text
        paint.color = Color.WHITE
        paint.textSize = 72f
        canvas.drawText("Game Over!", screenWidth / 2f, screenHeight / 2f - 50, paint)
        
        paint.textSize = 48f
        canvas.drawText("Final Score: $score", screenWidth / 2f, screenHeight / 2f + 20, paint)
        canvas.drawText("Tap to restart", screenWidth / 2f, screenHeight / 2f + 80, paint)
        
        // Reset game on touch
        if (gameState == GameState.GAME_OVER) {
            // This will be handled in touch event
        }
    }
}
