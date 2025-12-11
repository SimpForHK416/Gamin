package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gamin.MonsterBattler.data.LeaderboardManager
import com.example.gamin.MonsterBattler.data.PlayerScore

@Composable
fun SaveScoreDialog(
    score: Int,
    gameId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("KẾT THÚC!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Text("Điểm của bạn: $score", fontSize = 18.sp, modifier = Modifier.padding(8.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Text("Nhập tên (3 ký tự):")
                OutlinedTextField(
                    value = name,
                    onValueChange = { input ->
                        if (input.length <= 3) {
                            name = input.uppercase()
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.width(120.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isSaving) {
                    CircularProgressIndicator()
                } else {
                    Row {
                        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                            Text("Không lưu")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                isSaving = true
                                LeaderboardManager.saveScore(gameId, name, score, {
                                    isSaving = false
                                    onSaved()
                                }, { isSaving = false })
                            },
                            enabled = name.length == 3
                        ) {
                            Text("Lưu điểm")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardScreen(
    gameId: String,
    onBack: () -> Unit
) {
    var scores by remember { mutableStateOf<List<PlayerScore>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        LeaderboardManager.getTopScores(gameId) { list ->
            scores = list
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF212121))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BẢNG XẾP HẠNG", fontSize = 28.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                item {
                    Row(Modifier.padding(8.dp)) {
                        Text("#", Modifier.width(30.dp), fontWeight = FontWeight.Bold)
                        Text("TÊN", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("ĐIỂM", Modifier.width(80.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                    Divider(color = Color.Black, thickness = 2.dp)
                }
                itemsIndexed(scores) { index, player ->
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val color = when(index) { 0 -> Color(0xFFFFD700); 1 -> Color.Gray; 2 -> Color(0xFFCD7F32); else -> Color.Black }
                        Text("${index + 1}", Modifier.width(30.dp), fontWeight = FontWeight.Bold, color = color)
                        Text(player.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("${player.score}", Modifier.width(80.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                    }
                    Divider(color = Color.LightGray)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Về Menu Chính")
        }
    }
}

// Extension function cho Text width
fun androidx.compose.ui.text.TextStyle.copy() = this
@Composable
fun Text(text: String, width: androidx.compose.ui.unit.Dp, fontWeight: FontWeight? = null, textAlign: TextAlign? = null, color: Color = Color.Unspecified) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        fontWeight = fontWeight,
        textAlign = textAlign,
        color = color
    )
}