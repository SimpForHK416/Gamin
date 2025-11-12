package com.example.gamin.WhackAMole

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamin.R
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Hole(
    val isMouseVisible: Boolean = false,
    val mouseAppearTime: Long = 0L
)

enum class Difficulty(
    val displayName: String, 
    val holeCount: Int,
    val mouseAppearInterval: Long,
    val mouseVisibilityDuration: Long,
    val restPeriod: Long,
    val multipleMouseChance: Float,
    val tripleMouseChance: Float
) {
    EASY("Dễ", 9, 2000L, 1000L, 800L, 0.1f, 0.0f),
    MEDIUM("Trung bình", 16, 1000L, 500L, 600L, 0.2f, 0.0f),
    HARD("Khó", 24, 500L, 200L, 400L, 0.2f, 0.15f)
}

@Composable
fun WhackAMoleGame(difficulty: Difficulty) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(60) } // 60 seconds game
    var gameStarted by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var holes by remember { mutableStateOf(List(difficulty.holeCount) { Hole() }) }
    var gameTime by remember { mutableLongStateOf(0L) }
    
    // Game timer
    LaunchedEffect(gameStarted, gameOver) {
        if (gameStarted && !gameOver) {
            while (timeLeft > 0 && !gameOver) {
                delay(1000L)
                timeLeft--
                gameTime += 1000L
            }
            if (timeLeft <= 0) {
                gameOver = true
            }
        }
    }
    
    // Mouse appearance logic
    LaunchedEffect(gameStarted, gameOver, difficulty) {
        if (gameStarted && !gameOver) {
            while (!gameOver && timeLeft > 0) {
                // Hide mice that have been visible too long
                holes = holes.mapIndexed { index, hole ->
                    if (hole.isMouseVisible && System.currentTimeMillis() - hole.mouseAppearTime > difficulty.mouseVisibilityDuration) {
                        hole.copy(isMouseVisible = false)
                    } else {
                        hole
                    }
                }
                
                // Check if all mice are hidden (rest period)
                val anyMouseVisible = holes.any { it.isMouseVisible }
                
                if (!anyMouseVisible) {
                    // Rest period - wait before showing new mice
                    delay(difficulty.restPeriod)
                    
                    // Show new mouse(es) randomly
                    val availableHoles = holes.mapIndexedNotNull { index, hole ->
                        if (!hole.isMouseVisible) index else null
                    }
                    
                    if (availableHoles.isNotEmpty()) {
                        // Determine how many mice to show
                        val randomValue = Random.nextFloat()
                        val mouseCount = when {
                            randomValue < difficulty.tripleMouseChance && availableHoles.size >= 3 -> 3
                            randomValue < difficulty.tripleMouseChance + difficulty.multipleMouseChance && availableHoles.size >= 2 -> 2
                            else -> 1
                        }
                        
                        val selectedHoles = availableHoles.shuffled().take(mouseCount)
                        
                        holes = holes.mapIndexed { index, hole ->
                            if (index in selectedHoles) {
                                hole.copy(isMouseVisible = true, mouseAppearTime = System.currentTimeMillis())
                            } else {
                                hole
                            }
                        }
                    }
                } else {
                    // Regular check interval when mice are visible
                    delay(100L)
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Điểm: $score",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Thời gian: ${timeLeft}s",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = "Độ khó: ${difficulty.displayName}",
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!gameStarted) {
            // Start screen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Whack-a-Mole",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nhấn vào chuột khi chúng xuất hiện!",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { gameStarted = true },
                    modifier = Modifier.size(width = 200.dp, height = 50.dp)
                ) {
                    Text("Bắt đầu chơi", fontSize = 18.sp)
                }
            }
        } else if (gameOver) {
            // Game over screen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (score < 0) "Game Over!" else "Hết giờ!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (score < 0) {
                    Text(
                        text = "Điểm âm! Bạn đã nhấn nhầm quá nhiều!",
                        fontSize = 16.sp,
                        color = Color.Red
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Điểm cuối: $score",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row {
                    Button(
                        onClick = {
                            // Restart game
                            score = 0
                            timeLeft = 60
                            gameStarted = false
                            gameOver = false
                            holes = List(difficulty.holeCount) { Hole() }
                            gameTime = 0L
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Chơi lại")
                    }
                    OutlinedButton(
                        onClick = { activity?.finish() }
                    ) {
                        Text("Thoát")
                    }
                }
            }
        } else {
            // Game screen
            LazyVerticalGrid(
                columns = GridCells.Fixed(
                    when (difficulty) {
                        Difficulty.EASY -> 3
                        Difficulty.MEDIUM -> 4
                        Difficulty.HARD -> 4
                    }
                ),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(holes) { index, hole ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32)) // Dark green hole - contrasts with brown mouse
                            .clickable {
                                if (hole.isMouseVisible) {
                                    score += 10
                                    holes = holes.mapIndexed { i, h ->
                                        if (i == index) h.copy(isMouseVisible = false) else h
                                    }
                                } else {
                                    // Penalty for clicking empty hole
                                    score -= 5
                                    if (score < 0) {
                                        gameOver = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (hole.isMouseVisible) {
                            Image(
                                painter = painterResource(id = R.drawable.mouse_vector),
                                contentDescription = "Mouse",
                                modifier = Modifier.size(
                                    when (difficulty) {
                                        Difficulty.EASY -> 60.dp
                                        Difficulty.MEDIUM -> 50.dp
                                        Difficulty.HARD -> 45.dp
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
