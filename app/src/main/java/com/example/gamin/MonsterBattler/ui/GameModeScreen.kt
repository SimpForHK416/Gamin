package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameModeScreen(
    onModeSelected: (Boolean) -> Unit // True = Demonstrate, False = Normal
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF263238)) // Nền tối
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "CHỌN CHẾ ĐỘ CHƠI",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Nút Chế độ thường
        ModeCard(
            title = "BÌNH THƯỜNG",
            desc = "Trải nghiệm game chuẩn. Thử thách cân bằng.",
            color = Color(0xFFE0F7FA),
            borderColor = Color(0xFF00BCD4),
            onClick = { onModeSelected(false) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nút Chế độ Demonstrate
        ModeCard(
            title = "DEMONSTRATE (TEST)",
            desc = "Sức mạnh áp đảo! Starter được X2 toàn bộ chỉ số.",
            color = Color(0xFFFFEBEE),
            borderColor = Color(0xFFF44336),
            onClick = { onModeSelected(true) }
        )
    }
}

@Composable
fun ModeCard(title: String, desc: String, color: Color, borderColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(3.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.DarkGray)
        }
    }
}