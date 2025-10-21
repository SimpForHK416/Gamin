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
import com.example.gamin.FlappyBird.FlappyBirdActivity
import com.example.gamin.NoughtsAndCrosses.NoughtsAndCrossesActivity
import com.example.gamin.snake.SnakeActivity
import com.example.gamin.MineSweeper.MinesweeperActivity
import com.example.gamin.ui.theme.GaminTheme
import com.example.gamin.game2408.Game2408Activity // QUAN TRỌNG
import com.example.gamin.MemoryCard.MemoryCardActivity
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
            NoughtsAndCrossesActivity::class.java
        ),

        GameItem(
            "Snake",
            "Luật chơi: Điều khiển rắn ăn mồi để dài ra. Tránh đâm vào tường hoặc chính mình!",
            R.drawable.ic_snake,
            SnakeActivity::class.java
        ),

        GameItem(
            "Minesweeper",
            "",
            R.drawable.ic_minesweeper,
            MinesweeperActivity::class.java
        ),

        // BỔ SUNG: Game 2048
        GameItem(
            "2048",
            "Luật chơi: Trượt các ô số. Hai ô cùng số chạm nhau sẽ hợp nhất. Đạt ô 2048 để thắng!",
            R.drawable.ic_2048,
            Game2408Activity::class.java
        ),
        //flappybird

        GameItem(
            "Flappy Bird",
            "Nhấn để bay, tránh các ống. Càng xa càng tốt!",
            R.drawable.ic_flappy_bird_placeholder,
            FlappyBirdActivity::class.java
        ),

        GameItem(
            "Memory Card",
            "Lật 2 thẻ bài giống nhau để tạo cặp. Lật hết các cặp để thắng!",
            R.drawable.ic_memory_card_placeholder, // CẦN TẠO TÀI NGUYÊN NÀY
            MemoryCardActivity::class.java // BỔ SUNG: Class Memory Card
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