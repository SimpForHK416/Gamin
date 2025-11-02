package com.example.gamin.tetris

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.concurrent.thread
import kotlin.math.abs

class TetrisView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var threadRunning = false
    private var gridWidth = 10
    private var gridHeight = 20
    private var grid = Array(gridHeight) { IntArray(gridWidth) }
    private var currentBlock = TetrisBlock.randomBlock()
    private var fallDelay = 500L
    private var lastFallTime = System.currentTimeMillis()
    private var cellSize = 0
    private var offsetX = 0
    private var offsetY = 0
    private var score = 0

    // --- Lưới mờ nhẹ ---
    private val paintGrid = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        alpha = 40     // 👈 Làm mờ lưới
        strokeWidth = 1.5f
    }

    // --- Cảm ứng vuốt ---
    private var startX = 0f
    private var startY = 0f
    private val SWIPE_THRESHOLD = 200f
    private val SWIPE_VERTICAL_LIMIT = 150f

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        threadRunning = true
        startGame()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        threadRunning = false
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    private fun startGame() {
        grid = Array(gridHeight) { IntArray(gridWidth) }
        score = 0
        currentBlock = TetrisBlock.randomBlock()

        thread {
            while (threadRunning) {
                val now = System.currentTimeMillis()
                if (now - lastFallTime > fallDelay) {
                    moveDown()
                    lastFallTime = now
                }
                drawGame()
                Thread.sleep(30)
            }
        }
    }

    private fun drawGame() {
        val canvas = holder.lockCanvas() ?: return

        // --- Nền sáng dịu ---
        canvas.drawColor(Color.parseColor("#F2F2F2"))

        if (cellSize == 0) {
            cellSize = width / gridWidth
            offsetX = (width - cellSize * gridWidth) / 2
            offsetY = (height - cellSize * gridHeight) / 2
        }

        // --- Vẽ khối cố định ---
        for (y in grid.indices) {
            for (x in grid[0].indices) {
                if (grid[y][x] != 0) {
                    val paint = Paint().apply { color = grid[y][x] }
                    canvas.drawRect(
                        (offsetX + x * cellSize).toFloat(),
                        (offsetY + y * cellSize).toFloat(),
                        (offsetX + (x + 1) * cellSize).toFloat(),
                        (offsetY + (y + 1) * cellSize).toFloat(),
                        paint
                    )
                }
            }
        }

        // --- Vẽ khối đang rơi ---
        for (i in currentBlock.shape.indices) {
            for (j in currentBlock.shape[0].indices) {
                if (currentBlock.shape[i][j] == 1) {
                    val px = currentBlock.x + j
                    val py = currentBlock.y + i
                    if (py >= 0) {
                        val paint = currentBlock.getPaint()
                        canvas.drawRect(
                            (offsetX + px * cellSize).toFloat(),
                            (offsetY + py * cellSize).toFloat(),
                            (offsetX + (px + 1) * cellSize).toFloat(),
                            (offsetY + (py + 1) * cellSize).toFloat(),
                            paint
                        )
                    }
                }
            }
        }

        // --- Lưới mờ ---
        for (i in 0..gridHeight) {
            canvas.drawLine(
                offsetX.toFloat(), (offsetY + i * cellSize).toFloat(),
                (offsetX + gridWidth * cellSize).toFloat(), (offsetY + i * cellSize).toFloat(),
                paintGrid
            )
        }
        for (j in 0..gridWidth) {
            canvas.drawLine(
                (offsetX + j * cellSize).toFloat(), offsetY.toFloat(),
                (offsetX + j * cellSize).toFloat(), (offsetY + gridHeight * cellSize).toFloat(),
                paintGrid
            )
        }

        // --- Điểm ---
        val scorePaint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            isFakeBoldText = true
        }
        canvas.drawText("Score: $score", offsetX.toFloat(), 60f, scorePaint)

        holder.unlockCanvasAndPost(canvas)
    }

    private fun moveDown() {
        if (!collides(currentBlock, 0, 1)) {
            currentBlock.y++
        } else {
            mergeBlock()
            clearLines()
            currentBlock = TetrisBlock.randomBlock()
            if (collides(currentBlock, 0, 0)) {
                gameOver()
            }
        }
    }

    private fun moveLeft() {
        if (!collides(currentBlock, -1, 0))
            currentBlock.x--
    }

    private fun moveRight() {
        if (!collides(currentBlock, 1, 0))
            currentBlock.x++
    }

    private fun rotate() {
        val temp = currentBlock.copy(shape = currentBlock.shape.map { it.clone() }.toTypedArray())
        temp.rotate()
        if (!collides(temp, 0, 0))
            currentBlock.rotate()
    }

    private fun collides(block: TetrisBlock, dx: Int, dy: Int): Boolean {
        for (i in block.shape.indices) {
            for (j in block.shape[0].indices) {
                if (block.shape[i][j] == 1) {
                    val x = block.x + j + dx
                    val y = block.y + i + dy
                    if (x < 0 || x >= gridWidth || y >= gridHeight) return true
                    if (y >= 0 && grid[y][x] != 0) return true
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
        repeat(linesCleared) { newGrid.add(0, IntArray(gridWidth)) }
        grid = newGrid.toTypedArray()
        score += linesCleared * 100
    }

    private fun gameOver() {
        threadRunning = false
        post {
            AlertDialog.Builder(context)
                .setTitle("Game Over")
                .setMessage("Bạn đã thua! Quay lại menu chính?")
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    (context as? Activity)?.finish()
                }
                .show()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
            }

            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - startX
                val deltaY = event.y - startY

                // --- Vuốt từ trái sang phải để thoát ---
                if (deltaX > SWIPE_THRESHOLD && abs(deltaY) < SWIPE_VERTICAL_LIMIT) {
                    threadRunning = false
                    post {
                        (context as? Activity)?.runOnUiThread {
                            (context as? Activity)?.finish()
                        }
                    }
                    return true
                }

                // --- Điều khiển khối ---
                val third = width / 3
                when {
                    event.x < third -> moveLeft()
                    event.x > 2 * third -> moveRight()
                    else -> rotate()
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
            startGame()
        }
    }
}
