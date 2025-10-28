package com.example.gamin.Pong

import androidx.compose.ui.geometry.Offset
import kotlin.random.Random

// --- Thông số cố định ---
const val PADDLE_WIDTH = 30f // Tăng chiều rộng để dễ chạm hơn
const val PADDLE_HEIGHT = 100f
const val BALL_RADIUS = 10f
const val AI_PADDLE_SPEED = 400f // Tốc độ của máy (pixels/s)
const val BALL_START_SPEED = 700f // Tăng tốc độ bóng (từ 500f lên 700f)
const val GOAL_ZONE_WIDTH = 10f // THAY ĐỔI MỚI: Chiều rộng khu vực ghi điểm (Goal)

// --- Trạng thái Game ---
enum class PongGameStatus {
    Ready, Playing
}

data class PongState(
    val ballPosition: Offset,
    val ballVelocity: Offset,
    val playerPaddleY: Float,
    val aiPaddleY: Float,
    val playerScore: Int = 0,
    val aiScore: Int = 0,
    val status: PongGameStatus = PongGameStatus.Ready
)

// --- Hàm Khởi tạo ---
fun initializePongGame(gameWidth: Float, gameHeight: Float): PongState {
    return PongState(
        ballPosition = Offset(gameWidth / 2, gameHeight / 2),
        ballVelocity = Offset(0f, 0f), // Sẽ được gán khi bắt đầu
        playerPaddleY = gameHeight / 2 - PADDLE_HEIGHT / 2,
        aiPaddleY = gameHeight / 2 - PADDLE_HEIGHT / 2,
    )
}

// --- Hàm Cập nhật Vòng lặp Game ---
fun updateGamePhysics(
    state: PongState,
    dt: Float, // Delta time (thời gian giữa các khung hình)
    gameWidth: Float,
    gameHeight: Float
): PongState {
    if (state.status != PongGameStatus.Playing) return state

    var (ballPos, ballVel, playerY, aiY, pScore, aiScore, status) = state

    // 1. Cập nhật AI (Máy)
    val aiTargetY = ballPos.y - PADDLE_HEIGHT / 2
    val diff = aiTargetY - aiY
    val aiMove = (AI_PADDLE_SPEED * dt).coerceAtMost(kotlin.math.abs(diff)) * kotlin.math.sign(diff)
    aiY = (aiY + aiMove).coerceIn(0f, gameHeight - PADDLE_HEIGHT)

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
        GOAL_ZONE_WIDTH, // THAY ĐỔI: Bắt đầu từ sau Goal Zone
        playerY,
        GOAL_ZONE_WIDTH + PADDLE_WIDTH,
        playerY + PADDLE_HEIGHT
    )
    val aiPaddleRect = Rect(
        gameWidth - GOAL_ZONE_WIDTH - PADDLE_WIDTH, // THAY ĐỔI: Cách Goal Zone một khoảng
        aiY,
        gameWidth - GOAL_ZONE_WIDTH, // THAY ĐỔI: Kết thúc trước Goal Zone
        aiY + PADDLE_HEIGHT
    )

    // Va chạm với Player
    if (ballVel.x < 0f && ballPos.x - BALL_RADIUS < playerPaddleRect.right &&
        ballPos.y > playerPaddleRect.top && ballPos.y < playerPaddleRect.bottom
    ) {
        ballPos = ballPos.copy(x = playerPaddleRect.right + BALL_RADIUS)
        ballVel = ballVel.copy(x = -ballVel.x * 1.05f)
    }

    // Va chạm với AI
    if (ballVel.x > 0f && ballPos.x + BALL_RADIUS > aiPaddleRect.left &&
        ballPos.y > aiPaddleRect.top && ballPos.y < aiPaddleRect.bottom
    ) {
        ballPos = ballPos.copy(x = aiPaddleRect.left - BALL_RADIUS)
        ballVel = ballVel.copy(x = -ballVel.x * 1.05f)
    }

    // 5. Kiểm tra Ghi điểm
    // AI ghi điểm (Bóng vượt qua Player và vào Goal Zone)
    if (ballPos.x - BALL_RADIUS < GOAL_ZONE_WIDTH) { // THAY ĐỔI: Va chạm với Goal Zone
        aiScore++
        ballPos = Offset(gameWidth / 2, gameHeight / 2)
        ballVel = randomStartVelocity(goingLeft = true)
        status = PongGameStatus.Ready
    }
    // Player ghi điểm (Bóng vượt qua AI và vào Goal Zone)
    if (ballPos.x + BALL_RADIUS > gameWidth - GOAL_ZONE_WIDTH) { // THAY ĐỔI: Va chạm với Goal Zone
        pScore++
        ballPos = Offset(gameWidth / 2, gameHeight / 2)
        ballVel = randomStartVelocity(goingLeft = false)
        status = PongGameStatus.Ready
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

// Hàm reset bóng về giữa và giao cho 1 bên
fun randomStartVelocity(goingLeft: Boolean): Offset {
    val angle = Random.nextDouble(-Math.PI / 4, Math.PI / 4).toFloat() // Góc 45 độ
    val speedX = BALL_START_SPEED * kotlin.math.cos(angle)
    val speedY = BALL_START_SPEED * kotlin.math.sin(angle)
    return if (goingLeft) Offset(-speedX, speedY) else Offset(speedX, speedY)
}

// Dùng cho Rect (mô phỏng)
data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)
