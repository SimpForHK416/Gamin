package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MysteryScreen(
    currentHp: Int,
    maxHp: Int,
    onFinished: (Int) -> Unit
) {
    var screenState by remember { mutableStateOf("MENU") } // MENU, GAMBLE_GAME, RESULT
    var message by remember { mutableStateOf("Bạn tìm thấy một suối nước thiêng.\nBạn muốn làm gì?") }
    var tempHp by remember { mutableIntStateOf(currentHp) }

    // SỬA: Khởi tạo bằng 0 để biết là chưa tung
    var playerDice by remember { mutableIntStateOf(0) }
    var enemyDice by remember { mutableIntStateOf(0) }
    var isRolling by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "KHU VỰC BÍ ẨN",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0277BD)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text(
                "HP Hiện Tại: $tempHp / $maxHp",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if(tempHp < maxHp * 0.3) Color.Red else Color.Green
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp) // Tăng chiều cao lên chút cho thoáng
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF0277BD), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (screenState == "GAMBLE_GAME") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Hiển thị 2 xúc xắc
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiceBox("BẠN", playerDice, Color.Blue)

                        Text("VS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Gray)

                        DiceBox("ĐỊCH", enemyDice, Color.Red)
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    if (isRolling) {
                        Text("Đang tung...", fontSize = 24.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    isRolling = true
                                    // Hiệu ứng tung xúc xắc
                                    repeat(15) {
                                        playerDice = (1..6).random()
                                        enemyDice = (1..6).random()
                                        delay(80)
                                    }
                                    isRolling = false

                                    // Logic kết quả
                                    if (playerDice > enemyDice) {
                                        val heal = (maxHp * 0.5).toInt()
                                        tempHp = (tempHp + heal).coerceAtMost(maxHp)
                                        message = "THẮNG LỚN!\nBạn ($playerDice) > Địch ($enemyDice)\nHồi 50% HP (+$heal)"
                                        screenState = "RESULT"
                                    } else if (playerDice < enemyDice) {
                                        val damage = (maxHp * 0.1).toInt()
                                        tempHp = tempHp - damage
                                        if (tempHp <= 0) tempHp = 1
                                        message = "THUA CUỘC...\nBạn ($playerDice) < Địch ($enemyDice)\nMất 10% HP (-$damage)"
                                        screenState = "RESULT"
                                    } else {
                                        // Hòa -> Giữ nguyên màn hình để tung lại
                                        // Không làm gì cả, chỉ cập nhật UI
                                    }
                                }
                            },
                            modifier = Modifier.height(50.dp)
                        ) {
                            // SỬA: Logic hiển thị chữ trên nút
                            val buttonText = when {
                                playerDice == 0 -> "Tung Xúc Xắc" // Chưa tung lần nào
                                playerDice == enemyDice -> "Hòa! Tung lại!" // Đã tung và hòa
                                else -> "Tung Xúc Xắc" // Trường hợp khác
                            }
                            Text(buttonText, fontSize = 18.sp)
                        }
                    }
                }
            } else {
                // Hiển thị thông báo (Menu hoặc Kết quả)
                Text(
                    text = message,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 30.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (screenState == "MENU") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        val heal = (maxHp * 0.1).toInt()
                        tempHp = (tempHp + heal).coerceAtMost(maxHp)
                        message = "Bạn chọn an toàn.\nĐã nghỉ ngơi và hồi $heal HP."
                        screenState = "RESULT"
                    },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NGHỈ NGƠI")
                        Text("+10% HP", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        message = "Luật chơi:\nCao hơn -> Thắng (+50% HP)\nThấp hơn -> Thua (-10% HP)\nBằng nhau -> Tung lại"
                        screenState = "GAMBLE_GAME"
                    },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CÁ CƯỢC")
                        Text("Thắng +50% / Thua -10%", fontSize = 12.sp)
                    }
                }
            }
        } else if (screenState == "RESULT") {
            Button(
                onClick = { onFinished(tempHp) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Tiếp tục hành trình", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun DiceBox(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(80.dp) // Tăng kích thước ô xúc xắc
                .border(4.dp, color, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // SỬA: Nếu giá trị là 0 thì hiện dấu ?, ngược lại hiện số
            val textToShow = if (value == 0) "?" else "$value"

            Text(
                text = textToShow,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if(value == 0) Color.Gray else color
            )
        }
    }
}