package com.example.gamin.Arkanoid

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============ CONSTANTS ============
const val INITIAL_PADDLE_HEIGHT = 25f
const val INITIAL_PADDLE_WIDTH = 200f
const val PADDLE_Y_OFFSET = 60f

const val BALL_SIZE = 40f
const val INITIAL_BALL_SPEED = 700f
const val INITIAL_BALL_ANGLE = 60.0

const val BRICK_ROWS = 6
const val BRICK_COLS = 8
const val BRICK_PADDING = 5f
const val BRICK_HEIGHT = 50f
const val BRICK_SCORE = 10

const val SCORE_PANEL_HEIGHT = 90f
const val TOP_PADDING_OFFSET = SCORE_PANEL_HEIGHT + 10f

const val POWER_UP_SIZE = 30f
const val POWER_UP_SPEED = 250f
const val POWER_UP_CHANCE = 0.25
const val EXPLOSIVE_CHANCE = 0.1
const val POWER_UP_DURATION_MS = 5000L // 5 giây

// ============ DATA MODELS ============
sealed class GameState {
    object Ready : GameState()
    object Playing : GameState()
    object WaveClear : GameState()
    object GameOver : GameState()
    object TimeUp : GameState()
}

enum class BrickType {
    NORMAL,
    EXPLOSIVE,
    BOSS
}

// === THAY ĐỔI: Đã xóa BALL_SLOW ===
enum class PowerUpType {
    MULTI_BALL,
    PADDLE_GROW
}

data class PowerUpItem(
    val x: Float,
    val y: Float,
    val type: PowerUpType,
    val id: Long = Random.nextLong()
)

data class PaddleState(val x: Float)

data class BallState(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val id: Long = Random.nextLong()
)

data class BrickState(
    val rect: Rect,
    val color: Color,
    val type: BrickType,
    val powerUp: PowerUpType?,
    val hasStar: Boolean = false,
    val hitPoints: Int = if (type == BrickType.BOSS) 3 else 1,
    val isFlashing: Boolean = false,
    val isDestroyed: Boolean = false
)

// ============ WAVE GENERATOR ============
fun createBrickPattern(gameWidth: Float, wave: Int): List<BrickState> {
    val totalBrickWidth = gameWidth - BRICK_PADDING * (BRICK_COLS + 1)
    val brickWidth = totalBrickWidth / BRICK_COLS
    val colors = listOf(Color.Red, Color.Green, Color.Cyan, Color.Magenta, Color.Blue)

    val bricks = mutableListOf<BrickState>()
    val patternType = wave % 5

    for (row in 0 until BRICK_ROWS) {
        for (col in 0 until BRICK_COLS) {
            val brickX = BRICK_PADDING + col * (brickWidth + BRICK_PADDING)
            val brickY = TOP_PADDING_OFFSET + row * (BRICK_HEIGHT + BRICK_PADDING)
            val rect = Rect(
                left = brickX,
                top = brickY,
                right = brickX + brickWidth,
                bottom = brickY + BRICK_HEIGHT
            )

            val show = when (patternType) {
                0 -> true
                1 -> (row + col) % 2 == 0
                else -> Random.nextBoolean() || row < 2
            }

            if (show) {
                var brickType = BrickType.NORMAL
                var powerUp: PowerUpType? = null
                var color = colors.random()

                if (Random.nextFloat() < EXPLOSIVE_CHANCE) {
                    brickType = BrickType.EXPLOSIVE
                    color = Color.DarkGray
                }
                else if (Random.nextFloat() < POWER_UP_CHANCE) {
                    powerUp = PowerUpType.values().random()
                    color = Color.Magenta
                }

                bricks.add(
                    BrickState(
                        rect = rect,
                        color = color,
                        type = brickType,
                        powerUp = powerUp
                    )
                )
            }
        }
    }


    val bossCount = if (wave == 1) 0 else ((wave - 1) * 2).coerceAtLeast(1)

    val bossHP = 3 + (wave / 4)


    val normalBrickIndices = bricks.indices.filter { bricks[it].type == BrickType.NORMAL }

    val selectedIndices = normalBrickIndices.shuffled().take(bossCount)

    val finalBricks = bricks.toMutableList()
    for (index in selectedIndices) {
        val originalBrick = finalBricks[index]
        finalBricks[index] = originalBrick.copy(
            type = BrickType.BOSS,
            color = Color(0xFFFF4500),
            hitPoints = bossHP,
            powerUp = null,
            isFlashing = false
        )
    }

    return finalBricks
}



fun createInitialBall(gameWidth: Float, gameHeight: Float, speed: Float): BallState {
    val angleRadians = Math.toRadians(INITIAL_BALL_ANGLE)
    val yPos = gameHeight - PADDLE_Y_OFFSET - INITIAL_PADDLE_HEIGHT - BALL_SIZE / 2 - 1f - TOP_PADDING_OFFSET

    return BallState(
        x = gameWidth / 2,
        y = yPos,
        velocityX = speed * cos(angleRadians).toFloat(),
        velocityY = -speed * sin(angleRadians).toFloat()
    )
}