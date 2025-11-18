// Vị trí: com/example/gamin/MonsterBattler/ui/PickingScreen.kt
package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// =============================================
// THÊM CÁC IMPORT CẦN THIẾT
// =============================================
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.* // Thêm import này
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamin.MonsterBattler.data.Monster // <-- Import data class

/**
 * Màn hình chọn lựa, đọc data từ database
 */
@Composable
fun PickingScreen(
    monsters: List<Monster>, // <-- THAY ĐỔI: Nhận List<Monster>
    onMonsterSelected: (String) -> Unit
) {
    // State để theo dõi quái vật đang được "focus" (giống hover)
    var focusedMonster by remember { mutableStateOf<Monster?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
        // Bỏ VerticalArrangement.Center để thêm Nút Xác Nhận ở dưới
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
            monsters.forEach { monster -> // <-- Dùng danh sách monster
                // Kiểm tra xem quái vật này có đang được focus không
                val isFocused = monster.name == focusedMonster?.name

                MonsterChoiceCard(
                    name = monster.name, // <-- Chỉ truyền tên
                    isFocused = isFocused, // <-- Truyền trạng thái focus
                    onClick = {
                        focusedMonster = monster // <-- Cập nhật monster đang được focus
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // =============================================
        // KHU VỰC HIỂN THỊ MÔ TẢ (TÍNH NĂNG MỚI)
        // =============================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp) // Cố định chiều cao cho info box
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
                // Hiển thị thông tin của quái vật đang được focus
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

        Spacer(modifier = Modifier.weight(1f)) // Đẩy nút xuống dưới cùng

        // =============================================
        // NÚT XÁC NHẬN (TÍNH NĂNG MỚI)
        // =============================================
        Button(
            onClick = {
                // Chỉ chạy khi focusedMonster không null
                focusedMonster?.let { onMonsterSelected(it.name) }
            },
            enabled = focusedMonster != null, // Chỉ bật khi đã chọn 1 con
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Xác nhận", fontSize = 16.sp)
        }
    }
}

/**
 * Một card riêng để hiển thị lựa chọn quái vật
 */
@Composable
private fun RowScope.MonsterChoiceCard(
    name: String,
    isFocused: Boolean, // <-- Thêm biến
    onClick: () -> Unit
) {
    val painter = painterFor(name = name)

    // Nếu đang được focus, viền sẽ đổi màu
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Gray

    Card(
        modifier = Modifier
            .weight(1f)
            .padding(6.dp)
            .aspectRatio(0.8f)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)) // <-- Dùng viền
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Image(
                painter = painter,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
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