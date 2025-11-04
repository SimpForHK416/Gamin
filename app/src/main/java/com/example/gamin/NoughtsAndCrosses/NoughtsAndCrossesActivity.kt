package com.example.gamin.NoughtsAndCrosses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamin.R
import com.example.gamin.ui.theme.GaminTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoughtsAndCrossesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra("mode") ?: "PVP"

        setContent {
            GaminTheme {
                NoughtsAndCrossesGame(mode = mode, onBack = { finish() })
            }
        }
    }
}

@Composable
fun NoughtsAndCrossesGame(mode: String, onBack: () -> Unit) {
    var board by remember { mutableStateOf(List(25) { "" }) }
    var currentPlayer by remember { mutableStateOf("X") }
    var gameOver by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isPvE = mode == "PVE"

    fun resetGame() {
        board = List(25) { "" }
        currentPlayer = "X"
        gameOver = false
        winner = null
    }

    fun updateBoardAfterAIMove(moveIndex: Int) {
        val aiBoard = board.toMutableList()
        aiBoard[moveIndex] = "O"
        board = aiBoard
        val aiWinner = MinimaxAI.checkWinner(board)
        if (aiWinner != null) {
            gameOver = true
            winner = aiWinner
        } else if (board.none { it.isEmpty() }) {
            gameOver = true
            winner = "Draw"
        } else {
            currentPlayer = "X"
        }
    }

    LaunchedEffect(currentPlayer, gameOver) {
        if (isPvE && currentPlayer == "O" && !gameOver) {
            val aiMove = withContext(Dispatchers.Default) {
                MinimaxAI.findBestMove(board)
            }
            aiMove?.let { moveIndex ->
                updateBoardAfterAIMove(moveIndex)
            }
        }
    }

    fun makeMove(index: Int) {
        if (board[index].isEmpty() && !gameOver &&
            (currentPlayer == "X" || !isPvE)
        ) {
            val newBoard = board.toMutableList()
            newBoard[index] = currentPlayer
            board = newBoard
            val gameWinner = MinimaxAI.checkWinner(board)
            if (gameWinner != null) {
                gameOver = true
                winner = gameWinner
            } else if (board.none { it.isEmpty() }) {
                gameOver = true
                winner = "Draw"
            } else {
                currentPlayer = if (currentPlayer == "X") "O" else "X"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Noughts and Crosses - 5x5",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mode: $mode | Win: 3 in a row",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (gameOver) {
            Text(
                text = when (winner) {
                    "X" -> "Player X Wins! 🎉"
                    "O" -> if (isPvE) "AI Wins! 🤖" else "Player O Wins! 🎉"
                    "Draw" -> "It's a Draw! 🤝"
                    else -> ""
                },
                style = MaterialTheme.typography.headlineSmall,
                color = when (winner) {
                    "X" -> Color.Green
                    "O" -> Color.Red
                    "Draw" -> Color.Gray
                    else -> Color.Black
                },
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = if (isPvE && currentPlayer == "O") "AI Thinking..."
                else "Current Player: $currentPlayer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .size(350.dp)
                .border(2.dp, Color.Black),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            itemsIndexed(board) { index, cell ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .background(Color.White)
                        .clickable(
                            enabled = !gameOver &&
                                    (currentPlayer == "X" || !isPvE) &&
                                    cell.isEmpty()
                        ) {
                            makeMove(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (cell) {
                        "X" -> Image(
                            painter = painterResource(id = R.drawable.ic_x),
                            contentDescription = "X",
                            modifier = Modifier.size(40.dp)
                        )
                        "O" -> Image(
                            painter = painterResource(id = R.drawable.ic_o),
                            contentDescription = "O",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { resetGame() },
            modifier = Modifier.width(200.dp)
        ) {
            Text("New Game", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            modifier = Modifier.width(200.dp)
        ) {
            Text("Quay lại", fontSize = 16.sp)
        }
    }
}
