package com.example.gamin.tetris

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.concurrent.thread
import kotlin.math.abs

class TetrisView(context: Context, private val difficulty: String) : SurfaceView(context), SurfaceHolder.Callback {

    private var threadRunning = false
    private var gridWidth = 10
    private var gridHeight = 20
    private var grid = Array(gridHeight) { IntArray(gridWidth) }
    private var currentBlock = TetrisBlock.randomBlock()
    private var score = 0

    private var isGameOver = false
    private var initialFallDelay = 500L
    private var maxFallDelay = 100L
    private var fallDelay = 500L
    private var lastFallTime = System.currentTimeMillis()

    private val NEXT_BLOCK_COUNT = 3
    private var nextBlocks = mutableListOf<TetrisBlock>()

    private var cellSize = 0
    private var offsetX = 0
    private var offsetY = 0
    private var gameAreaWidth = 0
    private var panelAreaWidth = 0
    private val PANEL_RATIO = 0.3f
    private val PADDING = 20
    private var topAreaHeight = 0
    private var bottomAreaHeight = 0

    private var leftButtonRect = RectF()
    private var rightButtonRect = RectF()
    private var playAgainButtonRect = RectF()
    private var backButtonRect = RectF()

    private var startX = 0f
    private var startY = 0f
    private val SWIPE_HORIZONTAL_THRESHOLD = 150f
    private val SWIPE_VERTICAL_THRESHOLD = 300f
    private val TAP_THRESHOLD = 50f
    private var buttonPressed = false

    private val gameStateLock = Any()

    private val paintPanel = Paint().apply {
        color = Color.BLACK
        alpha = 30
        style = Paint.Style.FILL
    }
    private val paintGrid = Paint().apply {
        color = Color.parseColor("#444455")
        style = Paint.Style.STROKE
        alpha = 60
        strokeWidth = 1.5f
    }
    private val paintGhost = Paint().apply {
        style = Paint.Style.FILL
        alpha = 15
        isAntiAlias = true
    }
    private val paintText = Paint().apply {
        color = Color.LTGRAY
        textSize = 48f
        isFakeBoldText = true
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }
    private val paintScore = Paint(paintText).apply {
        color = Color.YELLOW
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }
    private val paintButtonBg = Paint().apply {
        color = Color.parseColor("#4A4A6A")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintButtonText = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val blockPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val blockLightBorderPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val blockDarkBorderPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        holder.addCallback(this)
    }

    private fun lightenColor(color: Int, factor: Float = 0.3f): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.rgb(
            (r + (255 - r) * factor).toInt().coerceAtMost(255),
            (g + (255 - g) * factor).toInt().coerceAtMost(255),
            (b + (255 - b) * factor).toInt().coerceAtMost(255)
        )
    }

    private fun darkenColor(color: Int, factor: Float = 0.4f): Int {
        return Color.rgb(
            (Color.red(color) * (1 - factor)).toInt(),
            (Color.green(color) * (1 - factor)).toInt(),
            (Color.blue(color) * (1 - factor)).toInt()
        )
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        threadRunning = true
        synchronized(gameStateLock) {
            startGame()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        threadRunning = false
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    private fun startGame() {
        isGameOver = false
        grid = Array(gridHeight) { IntArray(gridWidth) }
        score = 0

        when (difficulty) {
            "Dễ" -> {
                initialFallDelay = 800L
                maxFallDelay = 200L
            }
            "Trung bình" -> {
                initialFallDelay = 500L
                maxFallDelay = 150L
            }
            "Khó" -> {
                initialFallDelay = 300L
                maxFallDelay = 100L
            }
            else -> {
                initialFallDelay = 500L
                maxFallDelay = 150L
            }
        }
        fallDelay = initialFallDelay

        nextBlocks.clear()
        repeat(NEXT_BLOCK_COUNT) {
            nextBlocks.add(TetrisBlock.randomBlock())
        }
        currentBlock = nextBlocks.removeAt(0)

        lastFallTime = System.currentTimeMillis()

        if (threadRunning && !isGameOver) {
            gameLoop()
        }
    }

    private fun gameLoop() {
        thread {
            while (threadRunning) {
                synchronized(gameStateLock) {
                    if (!isGameOver) {
                        val now = System.currentTimeMillis()
                        if (now - lastFallTime > fallDelay) {
                            moveDown()
                            lastFallTime = now
                        }
                    }
                    drawGame()
                }
                Thread.sleep(30)
            }
        }
    }

    private fun getNewBlock() {
        currentBlock = nextBlocks.removeAt(0)
        nextBlocks.add(TetrisBlock.randomBlock())
        if (collides(currentBlock, 0, 0)) {
            gameOver()
        }
    }

    private fun drawCell(canvas: Canvas, px: Float, py: Float, size: Float, color: Int) {
        val border = size * 0.15f
        blockPaint.color = color
        blockLightBorderPaint.color = lightenColor(color)
        blockDarkBorderPaint.color = darkenColor(color)
        canvas.drawRect(px, py, px + size, py + border, blockLightBorderPaint)
        canvas.drawRect(px, py + border, px + border, py + size, blockLightBorderPaint)
        canvas.drawRect(px, py + size - border, px + size, py + size, blockDarkBorderPaint)
        canvas.drawRect(px + size - border, py, px + size, py + size - border, blockDarkBorderPaint)
        canvas.drawRect(px + border, py + border, px + size - border, py + size - border, blockPaint)
    }

    private fun drawGame() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.parseColor("#222233"))

            if (cellSize == 0) {
                panelAreaWidth = (width * PANEL_RATIO).toInt()
                gameAreaWidth = width - panelAreaWidth - PADDING * 2
                topAreaHeight = (height * 0.1f).toInt()
                bottomAreaHeight = (height * 0.15f).toInt()
                val availableHeight = height - topAreaHeight - bottomAreaHeight - PADDING * 2
                val cellW = gameAreaWidth / gridWidth
                val cellH = availableHeight / gridHeight
                cellSize = Math.min(cellW, cellH)
                gameAreaWidth = cellSize * gridWidth
                offsetX = PADDING
                offsetY = topAreaHeight + PADDING
            }

            for (y in grid.indices) {
                for (x in grid[0].indices) {
                    if (grid[y][x] != 0) {
                        drawCell(canvas, (offsetX + x * cellSize).toFloat(), (offsetY + y * cellSize).toFloat(), cellSize.toFloat(), grid[y][x])
                    }
                }
            }

            if (!isGameOver) {
                val ghostBlock = currentBlock.copy()
                while (!collides(ghostBlock, 0, 1)) { ghostBlock.y++ }
                paintGhost.color = currentBlock.color
                for (i in ghostBlock.shape.indices) {
                    for (j in ghostBlock.shape[0].indices) {
                        if (ghostBlock.shape[i][j] == 1) {
                            val px = ghostBlock.x + j
                            val py = ghostBlock.y + i
                            if (py >= 0) {
                                canvas.drawRect(
                                    (offsetX + px * cellSize).toFloat(),
                                    (offsetY + py * cellSize).toFloat(),
                                    (offsetX + (px + 1) * cellSize).toFloat(),
                                    (offsetY + (py + 1) * cellSize).toFloat(),
                                    paintGhost
                                )
                            }
                        }
                    }
                }

                for (i in currentBlock.shape.indices) {
                    for (j in currentBlock.shape[0].indices) {
                        if (currentBlock.shape[i][j] == 1) {
                            val px = currentBlock.x + j
                            val py = currentBlock.y + i
                            if (py >= 0) {
                                drawCell(canvas, (offsetX + px * cellSize).toFloat(), (offsetY + py * cellSize).toFloat(), cellSize.toFloat(), currentBlock.color)
                            }
                        }
                    }
                }
            }

            for (i in 0..gridHeight) {
                canvas.drawLine(offsetX.toFloat(), (offsetY + i * cellSize).toFloat(), (offsetX + gridWidth * cellSize).toFloat(), (offsetY + i * cellSize).toFloat(), paintGrid)
            }
            for (j in 0..gridWidth) {
                canvas.drawLine((offsetX + j * cellSize).toFloat(), offsetY.toFloat(), (offsetX + j * cellSize).toFloat(), (offsetY + gridHeight * cellSize).toFloat(), paintGrid)
            }

            val panelX = (offsetX + gameAreaWidth + PADDING).toFloat()
            val panelRight = width - PADDING.toFloat()
            val gridBottom = (offsetY + gridHeight * cellSize).toFloat()
            val scoreRect = RectF(panelX, offsetY.toFloat(), panelRight, offsetY + 180f)
            val nextRect = RectF(panelX, offsetY + 200f, panelRight, gridBottom)

            canvas.drawRoundRect(scoreRect, 20f, 20f, paintPanel)
            canvas.drawRoundRect(nextRect, 20f, 20f, paintPanel)

            val panelCenterX = panelX + (panelRight - panelX) / 2
            canvas.drawText("Score:", panelX + PADDING, offsetY + 60f, paintText)
            canvas.drawText("$score", panelCenterX, offsetY + 130f, paintScore)
            canvas.drawText("Next:", panelX + PADDING, offsetY + 260f, paintText)

            val nextCellSize = (panelAreaWidth * 0.9f) / 4
            var currentY = offsetY + 300f
            nextBlocks.forEach { block ->
                val blockHeight = block.shape.size * nextCellSize
                val blockWidth = block.shape[0].size * nextCellSize
                val blockStartX = panelCenterX - (blockWidth / 2f)
                for (i in block.shape.indices) {
                    for (j in block.shape[0].indices) {
                        if (block.shape[i][j] == 1) {
                            drawCell(canvas, blockStartX + (j * nextCellSize), currentY + (i * nextCellSize), nextCellSize, block.color)
                        }
                    }
                }
                currentY += blockHeight + 30f
            }

            val backButtonSize = topAreaHeight * 0.8f
            backButtonRect = RectF(offsetX.toFloat(), PADDING.toFloat(), offsetX.toFloat() + backButtonSize + (PADDING * 2), PADDING.toFloat() + backButtonSize)
            val backButtonPaint = Paint(paintButtonBg).apply { color = Color.parseColor("#808080") }
            val backButtonTextPaint = Paint(paintButtonText).apply { textSize = 48f }
            canvas.drawRoundRect(backButtonRect, 15f, 15f, backButtonPaint)
            canvas.drawText("Menu", backButtonRect.centerX(), backButtonRect.centerY() + backButtonTextPaint.textSize / 3, backButtonTextPaint)

            val bottomAreaTop = gridBottom + PADDING
            val buttonWidth = (gameAreaWidth * 0.48f)
            val buttonGap = (gameAreaWidth * 0.04f)
            val buttonHeight = bottomAreaHeight - PADDING * 1.5f
            val totalButtonWidth = (buttonWidth * 2) + buttonGap
            val startXButtons = offsetX.toFloat() + (gameAreaWidth - totalButtonWidth) / 2f

            leftButtonRect = RectF(startXButtons, bottomAreaTop, startXButtons + buttonWidth, bottomAreaTop + buttonHeight)
            rightButtonRect = RectF(startXButtons + buttonWidth + buttonGap, bottomAreaTop, startXButtons + buttonWidth * 2 + buttonGap, bottomAreaTop + buttonHeight)
            canvas.drawRoundRect(leftButtonRect, 25f, 25f, paintButtonBg)
            canvas.drawRoundRect(rightButtonRect, 25f, 25f, paintButtonBg)
            canvas.drawText("<", leftButtonRect.centerX(), leftButtonRect.centerY() + paintButtonText.textSize / 3, paintButtonText)
            canvas.drawText(">", rightButtonRect.centerX(), rightButtonRect.centerY() + paintButtonText.textSize / 3, paintButtonText)

            if (isGameOver) {
                playAgainButtonRect = RectF(panelX, PADDING.toFloat(), panelRight, PADDING + topAreaHeight * 0.8f)
                val playAgainPaint = Paint(paintButtonBg).apply { color = Color.parseColor("#FFC107") }
                val playAgainTextPaint = Paint(paintButtonText).apply { color = Color.BLACK; textSize = 48f }
                canvas.drawRoundRect(playAgainButtonRect, 20f, 20f, playAgainPaint)
                canvas.drawText("Chơi Lại", playAgainButtonRect.centerX(), playAgainButtonRect.centerY() + playAgainTextPaint.textSize / 3, playAgainTextPaint)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun moveDown() {
        if (!collides(currentBlock, 0, 1)) {
            currentBlock.y++
        } else {
            mergeBlock()
            clearLines()
            getNewBlock()
        }
    }

    private fun moveLeft() {
        if (isGameOver) return
        if (!collides(currentBlock, -1, 0)) currentBlock.x--
    }

    private fun moveRight() {
        if (isGameOver) return
        if (!collides(currentBlock, 1, 0)) currentBlock.x++
    }

    private fun rotate() {
        if (isGameOver) return
        val temp = currentBlock.copy(shape = currentBlock.shape.map { it.clone() }.toTypedArray())
        temp.rotate()
        if (!collides(temp, 0, 0)) currentBlock.rotate()
    }

    private fun hardDrop() {
        if (isGameOver) return
        while (!collides(currentBlock, 0, 1)) {
            currentBlock.y++
        }
        mergeBlock()
        clearLines()
        getNewBlock()
        lastFallTime = System.currentTimeMillis()
    }

    private fun collides(block: TetrisBlock, dx: Int, dy: Int): Boolean {
        for (i in block.shape.indices) {
            for (j in block.shape[0].indices) {
                if (block.shape[i][j] == 1) {
                    val x = block.x + j + dx
                    val correctedY = block.y + i + dy
                    if (x < 0 || x >= gridWidth || correctedY >= gridHeight) return true
                    if (correctedY >= 0 && grid[correctedY][x] != 0) return true
                }
            }
        }
        return false
    }

    private fun mergeBlock() {
        for (i in currentBlock.shape.indices) {
            for (j in currentBlock.shape[0].indices) {
                if (currentBlock.shape[i][j] == 1) {
                    val x = currentBlock.x + j
                    val y = currentBlock.y + i
                    if (y >= 0) grid[y][x] = currentBlock.color
                }
            }
        }
    }

    private fun clearLines() {
        val newGrid = grid.filter { row -> row.any { it == 0 } }.toMutableList()
        val linesCleared = gridHeight - newGrid.size
        if (linesCleared > 0) {
            repeat(linesCleared) { newGrid.add(0, IntArray(gridWidth)) }
            grid = newGrid.toTypedArray()
            score += linesCleared * 100 * linesCleared
            val level = score / 1000
            fallDelay = (initialFallDelay - (level * 50L)).coerceAtLeast(maxFallDelay)
        }
    }

    private fun gameOver() {
        isGameOver = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                buttonPressed = false
                if (backButtonRect.contains(event.x, event.y)) {
                    threadRunning = false
                    post {
                        (context as? Activity)?.runOnUiThread {
                            (context as? Activity)?.finish()
                        }
                    }
                    buttonPressed = true
                    return true
                }
                synchronized(gameStateLock) {
                    if (isGameOver) {
                        if (playAgainButtonRect.contains(event.x, event.y)) {
                            startGame()
                            buttonPressed = true
                            return true
                        }
                    } else {
                        if (leftButtonRect.contains(event.x, event.y)) {
                            moveLeft()
                            buttonPressed = true
                            return true
                        }
                        if (rightButtonRect.contains(event.x, event.y)) {
                            moveRight()
                            buttonPressed = true
                            return true
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isGameOver) return true
                if (buttonPressed) {
                    buttonPressed = false
                    return true
                }
                val deltaX = event.x - startX
                val deltaY = event.y - startY
                if (startX > offsetX && startX < (offsetX + gameAreaWidth)) {
                    synchronized(gameStateLock) {
                        if (deltaY > SWIPE_VERTICAL_THRESHOLD && abs(deltaX) < SWIPE_HORIZONTAL_THRESHOLD) {
                            hardDrop()
                        } else if (abs(deltaX) < TAP_THRESHOLD && abs(deltaY) < TAP_THRESHOLD) {
                            rotate()
                        }
                    }
                }
            }
        }
        return true
    }

    fun pause() {
        threadRunning = false
    }

    fun resume() {
        if (!threadRunning) {
            threadRunning = true
            gameLoop()
        }
    }
}
