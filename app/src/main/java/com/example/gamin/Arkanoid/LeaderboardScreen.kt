package com.example.gamin.Arkanoid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gamin.Arkanoid.database.ScoreDao
import com.example.gamin.Arkanoid.database.ScoreRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LeaderboardScreen(
    wave: Int,
    dbDao: ScoreDao,
    onDismiss: () -> Unit
) {
    var highScores by remember { mutableStateOf<List<ScoreRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(wave) {
        isLoading = true
        val scores = withContext(Dispatchers.IO) {
            dbDao.getTopScoresForWave(wave)
        }
        highScores = scores
        isLoading = false
    }

    // Lớp phủ toàn màn hình
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🏆 TOP SCORE - MÀN $wave 🏆",
                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.Yellow),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else if (highScores.isEmpty()) {
                    Text("Chưa có điểm nào được ghi nhận cho màn này.", color = Color.White)
                } else {
                    // Cột tiêu đề (Bỏ cột Thời gian)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color(0xFF333333)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(20.dp)) // Cho cột STT
                        Text("Điểm", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.White)
                        // BỎ Text("Thời gian", ...)
                        Text("Ngày", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        itemsIndexed(highScores) { index, record ->
                            ScoreRow(index = index + 1, record = record, wave = wave)
                            Divider(color = Color(0xFF444444))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("ĐÓNG")
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(index: Int, record: ScoreRecord, wave: Int) {
    val score = getScoreForWave(record, wave)
    // BỎ val time = getTimeForWave(record, wave)
    // BỎ val formattedTime = formatTime(time)

    val dateFormatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val formattedDate = remember { dateFormatter.format(Date(record.timestamp)) }

    val rankColor = when(index) {
        1 -> Color.Yellow
        2 -> Color.LightGray
        3 -> Color.Red
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$index.", color = rankColor, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        Text(score.toString(), Modifier.weight(1f), color = rankColor)
        Text(formattedDate, Modifier.weight(1.5f), color = Color.White.copy(alpha = 0.8f))
    }
}

// Hàm tiện ích để lấy điểm của màn chơi cụ thể
private fun getScoreForWave(record: ScoreRecord, wave: Int): Int {
    return when (wave) {
        1 -> record.wave1Score
        2 -> record.wave2Score
        3 -> record.wave3Score
        4 -> record.wave4Score
        5 -> record.wave5Score
        6 -> record.wave6Score
        7 -> record.wave7Score
        8 -> record.wave8Score
        9 -> record.wave9Score
        10 -> record.wave10Score
        else -> 0
    }
}



private fun formatTime(s: Int): String {
    val mm = s / 60
    val ss = s % 60
    return String.format("%02d:%02d", mm, ss)
}