package com.example.gamin.MonsterBattler.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.Reward
import kotlinx.coroutines.delay

// =====================================================================
// PHẦN 1: MÀN HÌNH CHỌN STARTER (LÚC ĐẦU GAME)
// =====================================================================

@Composable
fun PickingScreen(
    monsters: List<Monster>,
    onMonsterSelected: (String) -> Unit
) {
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
// PHẦN 2: MÀN HÌNH CHỌN BUFF (ĐÃ CẬP NHẬT TÊN VÀ THAM SỐ)
// =====================================================================

@Composable
fun BuffSelectionScreenModified(
    option1: Reward.StatUpgrade?, // Nullable để ẩn
    option2: Reward.Heal?,
    option3: Reward.SkillEffect?,
    pickCount: Int,               // Tham số mới: Số lần chọn còn lại
    onRewardSelected: (Reward) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
        Text("Còn lại số lần chọn: $pickCount", fontSize = 18.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // Ô 1: Tăng Chỉ Số (Màu Cam)
        if (option1 != null) {
            RewardCard(
                title = "Tăng Chỉ Số",
                desc = option1.description,
                color = Color(0xFFFFF3E0),
                borderColor = Color(0xFFFF9800),
                onClick = { onRewardSelected(option1) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Ô 2: Hồi Phục (Màu Xanh Lá)
        if (option2 != null) {
            RewardCard(
                title = "Hồi Phục",
                desc = option2.description,
                color = Color(0xFFE8F5E9),
                borderColor = Color(0xFF4CAF50),
                onClick = { onRewardSelected(option2) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Ô 3: Hiệu Ứng Kỹ Năng (Màu Tím)
        if (option3 != null) {
            RewardCard(
                title = "Hiệu Ứng: ${option3.buff.name}",
                desc = option3.buff.description,
                color = Color(0xFFF3E5F5),
                borderColor = Color(0xFF9C27B0),
                onClick = { onRewardSelected(option3) }
            )
        }
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


// =====================================================================
// PHẦN 3: MÀN HÌNH NÂNG CẤP LỚN (MAJOR UPGRADE)
// =====================================================================

// Map định nghĩa sự tiến hóa: Tên Gốc -> Tên Tiến Hóa
val EVOLUTION_MAP = mapOf(
    "CRISHY" to "CONFLEVOUR",
    "RHINPLINK" to "RHITAIN",
    "DOREWEE" to "DOPERAMI"
)

enum class UpgradeState {
    MENU, SELECT_STAT_TARGET, SELECT_EVO_TARGET, SELECT_RECRUIT, EVO_ANIMATION
}

@Composable
fun MajorUpgradeScreen(
    currentTeam: List<Monster>,
    availableStarters: List<Monster>, // Danh sách starter chưa được chọn
    onUpgradeFinished: (List<Monster>) -> Unit // Trả về đội hình mới
) {
    var state by remember { mutableStateOf(UpgradeState.MENU) }

    // Biến tạm để xử lý animation tiến hóa
    var monsterToEvolve by remember { mutableStateOf<Monster?>(null) }
    var evolvedMonsterResult by remember { mutableStateOf<Monster?>(null) }

    // Logic kiểm tra điều kiện
    val canRecruit = availableStarters.isNotEmpty()
    val canEvolve = currentTeam.any { EVOLUTION_MAP.containsKey(it.name) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF263238)) // Nền tối
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            UpgradeState.MENU -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "NÂNG CẤP LỚN",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Bạn đã đánh bại Boss! Hãy chọn phần thưởng đặc biệt:",
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                        textAlign = TextAlign.Center
                    )

                    // LỰA CHỌN 1: TĂNG CHỈ SỐ (Luôn hiện)
                    UpgradeOptionCard(
                        title = "Cường Hóa",
                        desc = "+10 Toàn bộ chỉ số cho 1 thành viên.",
                        color = Color(0xFFE3F2FD),
                        borderColor = Color(0xFF2196F3),
                        onClick = { state = UpgradeState.SELECT_STAT_TARGET }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // LỰA CHỌN 2: TIẾN HÓA
                    if (canEvolve) {
                        UpgradeOptionCard(
                            title = "Tiến Hóa",
                            desc = "Biến hình & +20 Toàn bộ chỉ số.",
                            color = Color(0xFFF3E5F5),
                            borderColor = Color(0xFF9C27B0),
                            onClick = { state = UpgradeState.SELECT_EVO_TARGET }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // LỰA CHỌN 3: CHIÊU MỘ
                    if (canRecruit) {
                        UpgradeOptionCard(
                            title = "Chiêu Mộ",
                            desc = "Thêm 1 Starter mới vào đội hình.",
                            color = Color(0xFFE8F5E9),
                            borderColor = Color(0xFF4CAF50),
                            onClick = { state = UpgradeState.SELECT_RECRUIT }
                        )
                    }
                }
            }

            UpgradeState.SELECT_STAT_TARGET -> {
                MonsterSelector(
                    title = "Chọn Quái Thú Cường Hóa (+10 Stats)",
                    monsters = currentTeam,
                    onBack = { state = UpgradeState.MENU },
                    onSelected = { selected ->
                        // Logic +10 chỉ số
                        val updatedTeam = currentTeam.map {
                            if (it.name == selected.name) {
                                it.copy(
                                    hp = it.hp + 10,
                                    atk = it.atk + 10,
                                    def = it.def + 10,
                                    speed = it.speed + 10
                                )
                            } else it
                        }
                        onUpgradeFinished(updatedTeam)
                    }
                )
            }

            UpgradeState.SELECT_RECRUIT -> {
                MonsterSelector(
                    title = "Chọn Đồng Đội Mới",
                    monsters = availableStarters,
                    onBack = { state = UpgradeState.MENU },
                    onSelected = { selected ->
                        // Logic thêm vào đội
                        val updatedTeam = currentTeam + selected
                        onUpgradeFinished(updatedTeam)
                    }
                )
            }

            UpgradeState.SELECT_EVO_TARGET -> {
                // Chỉ hiện những con có thể tiến hóa
                val evolvableMonsters = currentTeam.filter { EVOLUTION_MAP.containsKey(it.name) }

                MonsterSelector(
                    title = "Chọn Quái Thú Tiến Hóa",
                    monsters = evolvableMonsters,
                    onBack = { state = UpgradeState.MENU },
                    onSelected = { selected ->
                        monsterToEvolve = selected
                        val newName = EVOLUTION_MAP[selected.name]!!

                        // Logic +20 chỉ số dựa trên chỉ số CŨ
                        evolvedMonsterResult = selected.copy(
                            name = newName,
                            hp = selected.hp + 20,
                            atk = selected.atk + 20,
                            def = selected.def + 20,
                            speed = selected.speed + 20,
                            description = "Dạng tiến hóa của ${selected.name}"
                        )
                        state = UpgradeState.EVO_ANIMATION
                    }
                )
            }

            UpgradeState.EVO_ANIMATION -> {
                if (monsterToEvolve != null && evolvedMonsterResult != null) {
                    EvolutionAnimation(
                        oldMonster = monsterToEvolve!!,
                        newMonster = evolvedMonsterResult!!,
                        onAnimationFinished = {
                            // Cập nhật đội hình sau khi animation xong
                            val updatedTeam = currentTeam.map {
                                if (it.name == monsterToEvolve!!.name) evolvedMonsterResult!! else it
                            }
                            onUpgradeFinished(updatedTeam)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UpgradeOptionCard(title: String, desc: String, color: Color, borderColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                Text(desc, fontSize = 14.sp, color = Color.DarkGray)
            }
            Text("➤", fontSize = 24.sp, color = borderColor)
        }
    }
}

@Composable
fun MonsterSelector(
    title: String,
    monsters: List<Monster>,
    onBack: () -> Unit,
    onSelected: (Monster) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            monsters.forEach { m ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { onSelected(m) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Image(painter = painterFor(m.name), contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(m.name, color = Color.White, fontWeight = FontWeight.Bold)
                    // Hiện chỉ số để người chơi tham khảo
                    Text("HP: ${m.hp} | Atk: ${m.atk}", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
            Text("Quay Lại")
        }
    }
}

@Composable
fun EvolutionAnimation(
    oldMonster: Monster,
    newMonster: Monster,
    onAnimationFinished: () -> Unit
) {
    // Animation States
    var isEvolving by remember { mutableStateOf(false) }
    var showNewForm by remember { mutableStateOf(false) }

    // Animation Values
    val flashAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        isEvolving = true
        // 1. Nhấp nháy dạng cũ
        flashAnim.animateTo(1f, animationSpec = infiniteRepeatable(tween(200), RepeatMode.Reverse))
    }

    // Logic chuyển cảnh
    LaunchedEffect(Unit) {
        delay(2000) // Chờ 2s nhấp nháy
        showNewForm = true // Đổi sang ảnh mới
        delay(1000) // Hiện dạng mới 1s
        delay(1000) // Chờ thêm chút
        onAnimationFinished() // Xong
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (!showNewForm) "Cái gì?! ${oldMonster.name} đang tiến hóa!"
            else "Chúc mừng! ${oldMonster.name} đã tiến hóa thành ${newMonster.name}!",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(contentAlignment = Alignment.Center) {
            // Ánh sáng nền
            if (!showNewForm) {
                Box(modifier = Modifier
                    .size(250.dp)
                    .background(Color.White.copy(alpha = flashAnim.value * 0.5f), CircleShape))
            } else {
                Box(modifier = Modifier
                    .size(300.dp)
                    .background(Color(0xFFFFD700).copy(alpha = 0.3f), CircleShape))
            }

            // Quái vật
            val currentImageName = if (showNewForm) newMonster.name else oldMonster.name
            val scale = if (!showNewForm) 1f + (flashAnim.value * 0.1f) else 1.2f

            // Nếu đang nhấp nháy thì tint màu đen/trắng, nếu xong rồi thì hiện màu thật
            val colorFilter = if (!showNewForm) {
                ColorFilter.tint(
                    if (flashAnim.value > 0.5f) Color.White else Color.Black
                )
            } else null

            Image(
                painter = painterFor(currentImageName),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (showNewForm) {
            Text(
                "Tất cả chỉ số +20!",
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}