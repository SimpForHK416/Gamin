// Vị trí: com/example/gamin/MonsterBattler/ui/PickingScreen.kt
package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Màn hình chính để chọn lựa, bắt đầu bằng việc chọn monster
 */
@Composable
fun PickingScreen(
    onMonsterSelected: (String) -> Unit
) {
    // Danh sách 3 quái vật khởi đầu
    val monsters = listOf("CRISHY", "RHINPLINK", "DOREWEE")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Chọn quái vật khởi đầu",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Hiển thị 3 lựa chọn
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            monsters.forEach { monsterName ->
                MonsterChoiceCard(
                    name = monsterName,
                    onClick = {
                        onMonsterSelected(monsterName)
                    }
                )
            }
        }
    }
}

/**
 * Một card riêng để hiển thị lựa chọn quái vật
 */
@Composable
private fun RowScope.MonsterChoiceCard(
    name: String,
    onClick: () -> Unit
) {
    // Sử dụng helper 'painterFor' để tải ảnh
    // Bạn chỉ cần đặt 'crishy.png', 'rhinplink.png', 'dorewee.png'
    // vào thư mục res/drawable
    val painter = painterFor(name = name)

    Card(
        modifier = Modifier
            .weight(1f) // 3 card chiếm không gian bằng nhau
            .padding(6.dp)
            .aspectRatio(0.8f) // Đảm bảo card có chiều dọc
            .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Image(
                painter = painter,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f), // 70% không gian cho ảnh
                contentScale = ContentScale.Fit
            )
            Text(
                text = name.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.3f) // 30% không gian cho tên
            )
        }
    }
}