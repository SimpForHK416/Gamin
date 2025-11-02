package com.example.gamin.Pong

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// --- Thông số cố định (ĐÃ SỬA) ---
const val PADDLE_WIDTH = 40f // <-- Tăng lên 40f
const val PADDLE_HEIGHT = 120f // <-- Tăng lên 120f
const val BALL_RADIUS = 15f // <-- Tăng lên 15f
const val AI_PADDLE_SPEED = 300f // <-- Giảm tốc độ AI để "dốt" hơn
const val BALL_START_SPEED = 700f
const val GOAL_ZONE_WIDTH = 20f // <-- Tăng lên 20f

// --- YÊU CẦU MỚI ---
const val WIN_SCORE = 7
const val SPEED_INCREASE_FACTOR = 1.10f // <-- ĐÃ SỬA: Tăng 10% tốc độ
const val MAX_BOUNCE_ANGLE_RAD = (Math.PI / 4).toFloat()

// --- Trạng thái Game Mới ---
enum class GameMode { PVE, PVP }

enum class PongGameStatus {
    Ready, Playing, PlayerWins, AIWins
}

data class PongState(
    val ballPosition: Offset,
    val ballVelocity: Offset,
    val playerPaddleY: Float,
    val aiPaddleY: Float,
    val playerScore: Int = 0,
    val aiScore: Int = 0,
    val status: PongGameStatus = PongGameStatus.Ready,
    val gameMode: GameMode = GameMode.PVE
)

// --- Hàm Khởi tạo Mới ---
fun initializePongGame(gameWidth: Float, gameHeight: Float, mode: GameMode): PongState {
    return PongState(
        ballPosition = Offset(gameWidth / 2, gameHeight / 2),
        ballVelocity = Offset(0f, 0f),
        playerPaddleY = gameHeight / 2 - PADDLE_HEIGHT / 2,
        aiPaddleY = gameHeight / 2 - PADDLE_HEIGHT / 2,
        gameMode = mode
    )
}

// --- Hàm Cập nhật Vòng lặp Game (ĐÃ CẬP NHẬT TOÀN BỘ) ---
fun updateGamePhysics(
    state: PongState,
    dt: Float,
    gameWidth: Float,
    gameHeight: Float
): PongState {
    if (state.status != PongGameStatus.Playing) return state

    var (ballPos, ballVel, playerY, aiY, pScore, aiScore, status, mode) = state

    // 1. Cập nhật AI (Chỉ khi là PVE và làm "dốt" hơn)
    if (mode == GameMode.PVE) {
        val aiTargetY = ballPos.y - PADDLE_HEIGHT / 2
        val diff = aiTargetY - aiY

        // <-- ĐÃ SỬA: Tăng "dead zone" của AI để "dốt" hơn -->
        val deadZone = PADDLE_HEIGHT * 0.3f // Ví dụ: AI sẽ không di chuyển nếu bóng nằm trong 30% chính giữa vợt
        if (abs(diff) > deadZone) {
            val aiMove = (AI_PADDLE_SPEED * dt).coerceAtMost(abs(diff)) * sign(diff)
            aiY = (aiY + aiMove).coerceIn(0f, gameHeight - PADDLE_HEIGHT)
        }
    }

    // 2. Cập nhật vị trí bóng
    ballPos = ballPos.copy(
        x = ballPos.x + ballVel.x * dt,
        y = ballPos.y + ballVel.y * dt
    )

    // 3. Kiểm tra va chạm Tường (Trên/Dưới)
    if (ballPos.y - BALL_RADIUS < 0f) {
        ballPos = ballPos.copy(y = BALL_RADIUS)
        ballVel = ballVel.copy(y = -ballVel.y)
    }
    if (ballPos.y + BALL_RADIUS > gameHeight) {
        ballPos = ballPos.copy(y = gameHeight - BALL_RADIUS)
        ballVel = ballVel.copy(y = -ballVel.y)
    }

    // 4. Kiểm tra va chạm Thanh trượt (Paddles)
    val playerPaddleRect = Rect(
        GOAL_ZONE_WIDTH,
        playerY,
        GOAL_ZONE_WIDTH + PADDLE_WIDTH,
        playerY + PADDLE_HEIGHT
    )
    val aiPaddleRect = Rect(
        gameWidth - GOAL_ZONE_WIDTH - PADDLE_WIDTH,
        aiY,
        gameWidth - GOAL_ZONE_WIDTH,
        aiY + PADDLE_HEIGHT
    )

    // Va chạm với Player (Bên trái)
    if (ballVel.x < 0f && ballPos.x - BALL_RADIUS < playerPaddleRect.right &&
        ballPos.y > playerPaddleRect.top && ballPos.y < playerPaddleRect.bottom
    ) {
        ballPos = ballPos.copy(x = playerPaddleRect.right + BALL_RADIUS)

        val relativeIntersectY = (playerY + PADDLE_HEIGHT / 2 - ballPos.y) / (PADDLE_HEIGHT / 2)
        val bounceAngle = relativeIntersectY * MAX_BOUNCE_ANGLE_RAD

        val currentSpeed = sqrt(ballVel.x * ballVel.x + ballVel.y * ballVel.y)
        val newSpeed = currentSpeed * SPEED_INCREASE_FACTOR // <-- Tăng 10%

        ballVel = Offset(
            newSpeed * cos(bounceAngle),
            -newSpeed * sin(bounceAngle)
        )
    }

    // Va chạm với AI/Player 2 (Bên phải)
    if (ballVel.x > 0f && ballPos.x + BALL_RADIUS > aiPaddleRect.left &&
        ballPos.y > aiPaddleRect.top && ballPos.y < aiPaddleRect.bottom
    ) {
        ballPos = ballPos.copy(x = aiPaddleRect.left - BALL_RADIUS)

        val relativeIntersectY = (aiY + PADDLE_HEIGHT / 2 - ballPos.y) / (PADDLE_HEIGHT / 2)
        val bounceAngle = relativeIntersectY * MAX_BOUNCE_ANGLE_RAD

        val currentSpeed = sqrt(ballVel.x * ballVel.x + ballVel.y * ballVel.y)
        val newSpeed = currentSpeed * SPEED_INCREASE_FACTOR // <-- Tăng 10%

        ballVel = Offset(
            -newSpeed * cos(bounceAngle),
            -newSpeed * sin(bounceAngle)
        )
    }

    // 5. Kiểm tra Ghi điểm

    // AI/Player 2 ghi điểm
    if (ballPos.x - BALL_RADIUS < GOAL_ZONE_WIDTH) {
        aiScore++
        if (aiScore == WIN_SCORE) {
            status = PongGameStatus.AIWins
            ballVel = Offset(0f, 0f)
        } else {
            ballPos = Offset(gameWidth / 2, gameHeight / 2)
            ballVel = randomStartVelocity(goingLeft = true)
            status = PongGameStatus.Ready
        }
    }

    // Player 1 ghi điểm
    if (ballPos.x + BALL_RADIUS > gameWidth - GOAL_ZONE_WIDTH) {
        pScore++
        if (pScore == WIN_SCORE) {
            status = PongGameStatus.PlayerWins
            ballVel = Offset(0f, 0f)
        } else {
            ballPos = Offset(gameWidth / 2, gameHeight / 2)
            ballVel = randomStartVelocity(goingLeft = false)
            status = PongGameStatus.Ready
        }
    }

    return state.copy(
        ballPosition = ballPos,
        ballVelocity = ballVel,
        playerPaddleY = playerY,
        aiPaddleY = aiY,
        playerScore = pScore,
        aiScore = aiScore,
        status = status
    )
}

// Hàm reset bóng (không đổi)
fun randomStartVelocity(goingLeft: Boolean): Offset {
    val angle = Random.nextDouble(-Math.PI / 4, Math.PI / 4).toFloat()
    val speedX = BALL_START_SPEED * cos(angle)
    val speedY = BALL_START_SPEED * sin(angle)
    return if (goingLeft) Offset(-speedX, speedY) else Offset(speedX, speedY)
}

// Data class Rect (không đổi)
data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)