package com.example.gamin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gamin.Arkanoid.ArkanoidActivity
import com.example.gamin.BubbleShooter.BubbleShooterActivity
import com.example.gamin.FlappyBird.FlappyBirdActivity
import com.example.gamin.MemoryCard.MemoryCardActivity
import com.example.gamin.MineSweeper.MinesweeperActivity
import com.example.gamin.NoughtsAndCrosses.NoughtsAndCrossesActivity
import com.example.gamin.Pong.PongActivity
import com.example.gamin.TowerBloxx.TowerBloxxActivity
import com.example.gamin.game2408.Game2408Activity
import com.example.gamin.snake.SnakeActivity
import com.example.gamin.tetris.TetrisActivity
import com.example.gamin.ui.theme.GaminTheme
import com.example.gamin.WhackAMole.WhackAMoleActivity

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

        GameItem(
            "2048",
            "Luật chơi: Trượt các ô số. Hai ô cùng số chạm nhau sẽ hợp nhất. Đạt ô 2048 để thắng!",
            R.drawable.ic_2048,
            Game2408Activity::class.java
        ),

        GameItem(
            "Flappy Bird",
            "Nhấn để bay, tránh các ống. Càng xa càng tốt!",
            R.drawable.ic_flappy_bird_placeholder,
            FlappyBirdActivity::class.java
        ),

        GameItem(
            "Memory Card",
            "Lật 2 thẻ bài giống nhau để tạo cặp. Lật hết các cặp để thắng!",
            R.drawable.ic_memory_card_placeholder,
            MemoryCardActivity::class.java
        ),

        // =============================================
        // ĐÃ CẬP NHẬT GAME PONG
        GameItem(
            "Pong",
            "Dùng thanh trượt di chuyển lên xuống để đánh bóng. Đừng để lọt!",
            R.drawable.ic_pong, // <-- ĐÃ THAY ĐỔI ICON THEO YÊU CẦU
            PongActivity::class.java
        ),
        // =============================================
        //Tetris
        GameItem(
            "Tetris",
            "Luật chơi: Xếp các khối gạch rơi để tạo hàng ngang đầy. Hàng đầy sẽ biến mất!",
            R.drawable.ic_tetris_placeholder,
            TetrisActivity::class.java
        ),
        //arkanoid
        GameItem(
            "Arkanoid",
            "Dùng thanh trượt để đỡ bóng, phá hủy toàn bộ gạch.",
            R.drawable.ic_arkanoid,
            ArkanoidActivity::class.java
        ),

        GameItem(
            "Tower Bloxx",
           "Thả khối nhà sao cho chồng khít với tầng trước đó. Lệch quá là thua!",
            R.drawable.ic_tower_bloxx_placeholder,
            TowerBloxxActivity::class.java
        ),

        GameItem(
            "Bubble Shooter",
            "Bắn bong bóng cùng màu để tạo nhóm 3 quả trở lên và loại bỏ chúng. Đừng để bong bóng chạm đáy!",
            R.drawable.ic_bubble_shooter,
            BubbleShooterActivity::class.java
        ),

        GameItem(
            "Whack-a-Mole",
            "Đập chuột khi chúng xuất hiện từ lỗ! Tốc độ tăng dần theo thời gian. 3 độ khó khác nhau!",
            R.drawable.ic_whack_a_mole,
            WhackAMoleActivity::class.java
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