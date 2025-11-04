package com.example.gamin.MineSweeper

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinesweeperScreen() {
    var game by remember { mutableStateOf(MinesweeperGame(rows = 8, cols = 8, totalMines = 10)) }
    val activity = (LocalContext.current as? Activity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { activity?.finish() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Quay lại")
            }

            Text("Mines: ${game.minesLeft}", style = MaterialTheme.typography.titleMedium)

            Button(onClick = { game = MinesweeperGame(8, 8, 10) }) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(game.cols),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Gray),
            contentPadding = PaddingValues(1.dp)
        ) {
            itemsIndexed(game.board.flatten()) { index, cell ->
                MinesweeperCell(
                    cell = cell,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .combinedClickable(
                            enabled = game.status == "Playing",
                            onClick = {
                                game = game.revealCell(index / game.cols, index % game.cols)
                            },
                            onLongClick = {
                                game = game.toggleFlag(index / game.cols, index % game.cols)
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(game.status, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun MinesweeperCell(cell: MinesweeperCellState, modifier: Modifier) {
    val backgroundColor = when {
        cell.isRevealed -> Color(0xFFC0C0C0)
        cell.isFlagged -> Color(0xFFFFCC80)
        else -> Color(0xFFE0E0E0)
    }

    val textColor = when (cell.minesAround) {
        1 -> Color.Blue
        2 -> Color.Green.copy(red = 0.5f)
        3 -> Color.Red
        4 -> Color.Blue.copy(green = 0.5f)
        else -> Color.Black
    }

    Box(
        modifier = modifier.background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (cell.isRevealed) {
            when {
                cell.isMine -> Text("💣", fontSize = 18.sp)
                cell.minesAround > 0 -> Text(
                    text = cell.minesAround.toString(),
                    color = textColor,
                    fontSize = 16.sp
                )
            }
        } else if (cell.isFlagged) {
            Text("🚩", fontSize = 18.sp)
        }
    }
}
