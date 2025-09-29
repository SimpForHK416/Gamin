package com.example.gamin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gamin.ui.theme.GaminTheme

data class GameItem(
    val title: String,
    val rules: String,
    val imageRes: Int,
    val targetActivity: Class<*>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameMenu { game ->
                        val intent = Intent(this, GameIntroActivity::class.java).apply {
                            putExtra("title", game.title)
                            putExtra("rules", game.rules)
                            putExtra("image", game.imageRes)

                            // SỬA: Ép kiểu targetActivity sang Serializable để GameIntroActivity nhận được an toàn
                            putExtra("targetClass", game.targetActivity as java.io.Serializable)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun GameMenu(onGameClick: (GameItem) -> Unit) {
    val games = listOf(
        GameItem(
            "Noughts And Crosses",
            "Luật chơi: Người chơi lần lượt đánh X và O. Ai tạo được 3 ô liên tiếp sẽ thắng.",
            R.drawable.ic_tictactoe,
            com.example.gamin.NoughtsAndCrosses.NoughtsAndCrossesActivity::class.java
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(games) { game ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onGameClick(game) },
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = game.imageRes),
                        contentDescription = game.title,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(game.title, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}