package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamin.MonsterBattler.data.Monster

@Composable
fun TeamManagementScreen(
    team: List<Monster>,
    onSwap: (Int, Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF263238))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "QUẢN LÝ ĐỘI HÌNH",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            "Chọn 2 quái thú để đổi vị trí.\nQuái thú ở vị trí đầu tiên (#1) sẽ ra trận.",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(team.size) { index ->
                val monster = team[index]
                val isSelected = selectedIndex == index
                val isLeader = index == 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            if (isSelected) Color(0xFFFFD54F) else Color.White,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            if (isSelected) 4.dp else 0.dp,
                            if (isSelected) Color(0xFFFF6F00) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (selectedIndex == null) {
                                selectedIndex = index
                            } else {
                                if (selectedIndex != index) {
                                    onSwap(selectedIndex!!, index)
                                }
                                selectedIndex = null
                            }
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${index + 1}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLeader) Color.Red else Color.Gray,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    Image(
                        painter = painterFor(monster.name),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )

                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(monster.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("HP: ${monster.hp} | Atk: ${monster.atk}", fontSize = 14.sp)
                        Text("Def: ${monster.def} | Spd: ${monster.speed}", fontSize = 14.sp)
                        if (isLeader) {
                            Text("ĐANG CHỌN", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Quay Lại")
        }
    }
}