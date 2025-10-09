package com.example.gamin.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Point(val x: Int, val y: Int)

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

@Composable
fun SnakeGameCanvas(
    isPlaying: Boolean,
    onScoreChanged: (Int) -> Unit,
    onGameOver: (Int) -> Unit
) {
    val gridSize = 20

    var snake by remember(isPlaying) {
        mutableStateOf(listOf(Point(5, 5), Point(4, 5), Point(3, 5)))
    }
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var food by remember { mutableStateOf(randomFood(gridSize, snake)) }
    var score by remember { mutableStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 🟩 Canvas game
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFEEEEEE))
        ) {
            val cellPx = size.width / gridSize

            // 🍎 Vẽ mồi
            drawRect(
                color = Color.Red,
                topLeft = Offset(food.x * cellPx, food.y * cellPx),
                size = androidx.compose.ui.geometry.Size(cellPx, cellPx)
            )

            // 🐍 Vẽ rắn
            snake.forEachIndexed { index, p ->
                drawRect(
                    color = if (index == 0) Color.Green else Color(0xFF4CAF50),
                    topLeft = Offset(p.x * cellPx, p.y * cellPx),
                    size = androidx.compose.ui.geometry.Size(cellPx, cellPx)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🕹️ Bảng điều khiển hướng
        DirectionButtons(
            onUp = {
                if (direction != Direction.DOWN) direction = Direction.UP
            },
            onDown = {
                if (direction != Direction.UP) direction = Direction.DOWN
            },
            onLeft = {
                if (direction != Direction.RIGHT) direction = Direction.LEFT
            },
            onRight = {
                if (direction != Direction.LEFT) direction = Direction.RIGHT
            }
        )
    }

    // ⚡ Vòng lặp game
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(150)
            val head = snake.first()
            val newHead = when (direction) {
                Direction.UP -> Point(head.x, head.y - 1)
                Direction.DOWN -> Point(head.x, head.y + 1)
                Direction.LEFT -> Point(head.x - 1, head.y)
                Direction.RIGHT -> Point(head.x + 1, head.y)
            }

            // Va chạm
            if (newHead.x !in 0 until gridSize ||
                newHead.y !in 0 until gridSize ||
                newHead in snake
            ) {
                onGameOver(score)
                break
            }

            val newSnake = mutableListOf(newHead)
            newSnake.addAll(snake)

            if (newHead == food) {
                score += 10
                onScoreChanged(score)
                food = randomFood(gridSize, newSnake)
            } else {
                newSnake.removeLast()
            }

            snake = newSnake
        }
    }
}

@Composable
fun DirectionButtons(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onUp, modifier = Modifier.size(60.dp)) {
            Text("⬆")
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onLeft, modifier = Modifier.size(60.dp)) {
                Text("⬅")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = onRight, modifier = Modifier.size(60.dp)) {
                Text("➡")
            }
        }

        Button(onClick = onDown, modifier = Modifier.size(60.dp)) {
            Text("⬇")
        }
    }
}

fun randomFood(gridSize: Int, snake: List<Point>): Point {
    var p: Point
    do {
        p = Point(Random.nextInt(gridSize), Random.nextInt(gridSize))
    } while (p in snake)
    return p
}
