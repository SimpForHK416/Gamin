package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.Reward

// =====================================================================
// PHẦN 1: MÀN HÌNH CHỌN QUÁI VẬT (STARTER)
// =====================================================================

@Composable
fun PickingScreen(
    monsters: List<Monster>,
    onMonsterSelected: (String) -> Unit
) {
    // Biến lưu quái vật đang được chọn (để hiện thông tin)
    var focusedMonster by remember { mutableStateOf<Monster?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Chọn quái vật khởi đầu",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Hàng chứa 3 thẻ bài quái vật
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            monsters.forEach { monster ->
                val isFocused = monster.name == focusedMonster?.name
                MonsterChoiceCard(
                    name = monster.name,
                    isFocused = isFocused,
                    onClick = { focusedMonster = monster }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Khung hiển thị thông tin chi tiết
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (focusedMonster == null) {
                Text(
                    "Hãy nhấn vào một quái vật để xem chi tiết...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                Column {
                    Text(
                        "${focusedMonster!!.name} - (Hệ: ${focusedMonster!!.type})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Nội tại: ${focusedMonster!!.ability}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        focusedMonster!!.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Nút Xác Nhận
        Button(
            onClick = {
                focusedMonster?.let { onMonsterSelected(it.name) }
            },
            enabled = focusedMonster != null,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Xác nhận", fontSize = 16.sp)
        }
    }
}

@Composable
private fun RowScope.MonsterChoiceCard(
    name: String,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val painter = painterFor(name = name)
    // Đổi màu viền nếu được chọn
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Gray

    Card(
        modifier = Modifier
            .weight(1f)
            .padding(6.dp)
            .aspectRatio(0.8f)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
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
                modifier = Modifier.fillMaxWidth().weight(0.7f),
                contentScale = ContentScale.Fit
            )
            Text(
                text = name.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.3f)
            )
        }
    }
}

// =====================================================================
// PHẦN 2: MÀN HÌNH CHỌN BUFF (SAU KHI THẮNG) - 3 Ô LỰA CHỌN
// =====================================================================

@Composable
fun BuffSelectionScreen(
    option1: Reward.StatUpgrade, // Ô 1: Tăng chỉ số
    option2: Reward.Heal,        // Ô 2: Hồi máu
    option3: Reward.SkillEffect, // Ô 3: Buff hiệu ứng
    onRewardSelected: (Reward) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "CHIẾN THẮNG!",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFFD700), // Màu vàng
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Chọn 1 trong 3 phần thưởng:", fontSize = 18.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // Ô 1: Tăng Chỉ Số (Màu Cam)
        RewardCard(
            title = "Tăng Chỉ Số",
            desc = option1.description,
            color = Color(0xFFFFF3E0),
            borderColor = Color(0xFFFF9800),
            onClick = { onRewardSelected(option1) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ô 2: Hồi Phục (Màu Xanh Lá)
        RewardCard(
            title = "Hồi Phục",
            desc = option2.description,
            color = Color(0xFFE8F5E9),
            borderColor = Color(0xFF4CAF50),
            onClick = { onRewardSelected(option2) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ô 3: Hiệu Ứng Kỹ Năng (Màu Tím)
        RewardCard(
            title = "Hiệu Ứng: ${option3.buff.name}",
            desc = option3.buff.description,
            color = Color(0xFFF3E5F5),
            borderColor = Color(0xFF9C27B0),
            onClick = { onRewardSelected(option3) }
        )
    }
}

@Composable
fun RewardCard(title: String, desc: String, color: Color, borderColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}