package com.example.gamin.MonsterBattler.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gamin.MonsterBattler.StatusEffect
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.Skill
import kotlinx.coroutines.launch

// --- LƯU Ý: ĐÃ XÓA ENUM BATTLESTATE Ở ĐÂY ĐỂ TRÁNH TRÙNG LẶP ---
// Nó sẽ tự động dùng Enum BattleState nằm trong file BattleViewModel.kt

@Composable
fun BattleScreen(
    viewModel: BattleViewModel,
    team: List<Monster>,
    onSwitchMonster: (Monster) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showSwapDialog by remember { mutableStateOf(false) }

    val playerOffsetX = remember { Animatable(-1000f) }
    val enemyOffsetX = remember { Animatable(1000f) }

    LaunchedEffect(state.battleState) {
        when(state.battleState) {
            BattleState.ENTERING -> {
                launch { playerOffsetX.animateTo(0f, tween(1000)) }
                launch { enemyOffsetX.animateTo(0f, tween(1000)) }
            }
            BattleState.PLAYER_ATTACKING -> {
                playerOffsetX.animateTo(150f, tween(200))
                playerOffsetX.animateTo(0f, tween(200))
            }
            BattleState.ENEMY_ATTACKING -> {
                enemyOffsetX.animateTo(-150f, tween(200))
                enemyOffsetX.animateTo(0f, tween(200))
            }
            else -> {}
        }
    }

    if (state.playerMonster == null || state.enemyMonster == null) return

    // DIALOG CHỌN QUÁI
    if (showSwapDialog) {
        Dialog(onDismissRequest = { showSwapDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Đổi quái (Mất 1 lượt)", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

                    val availableMonsters = team.filter { it.name != state.playerMonster!!.name }

                    if (availableMonsters.isEmpty()) {
                        Text("Không còn quái thú nào khác!", color = Color.Gray)
                    } else {
                        availableMonsters.forEach { monster ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSwitchMonster(monster)
                                        showSwapDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(painter = painterFor(monster.name), contentDescription = null, modifier = Modifier.size(50.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(monster.name, fontWeight = FontWeight.Bold)
                                    Text("HP: ${monster.hp}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Divider(color = Color.LightGray)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showSwapDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F8F8))) {

        Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
            if (state.battleState != BattleState.INTRO_DIALOGUE) {
                Column(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top=40.dp, end=20.dp)
                        .graphicsLayer { translationX = enemyOffsetX.value },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InfoBox(state.enemyMonster!!.name, state.enemyHp, state.enemyMonster!!.hp)
                    if (state.enemyBuffTurns > 0) Text("▲ BUFF (${state.enemyBuffTurns})", color = Color.Red, fontWeight = FontWeight.Bold)
                    if (state.enemyStatus != StatusEffect.NONE) Text(state.enemyStatus.name, color = Color.Magenta, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    val enemySize = if (state.isBoss) 240.dp else 150.dp
                    Image(painter = painterFor(state.enemyMonster!!.name), contentDescription = null, modifier = Modifier.size(enemySize), contentScale = ContentScale.Fit)
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(bottom=20.dp, start=20.dp)
                        .graphicsLayer { translationX = playerOffsetX.value },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painter = painterFor("${state.playerMonster!!.name}_back"), contentDescription = null, modifier = Modifier.size(180.dp), contentScale = ContentScale.Fit)
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoBox(state.playerMonster!!.name, state.playerHp, state.playerMonster!!.hp)
                    if (state.playerBuffTurns > 0) Text("▲ BUFF (${state.playerBuffTurns})", color = Color.Blue, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(modifier = Modifier.weight(0.4f).fillMaxWidth().background(Color(0xFF2D3436)).padding(16.dp)) {

            if (state.battleState == BattleState.PLAYER_ATTACKING ||
                state.battleState == BattleState.ENEMY_ATTACKING ||
                state.battleState == BattleState.BATTLE_END ||
                state.battleState == BattleState.PLAYER_FAINTED) { // Thêm hiển thị log khi chết

                Box(modifier = Modifier.fillMaxSize().background(Color.White, RoundedCornerShape(8.dp)).padding(16.dp)) {
                    Text(state.logMessage, fontSize = 18.sp, color = state.logColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                    if (state.battleState == BattleState.BATTLE_END) {
                        Button(onClick = { viewModel.onEndBattleConfirmed() }, modifier = Modifier.align(Alignment.BottomEnd)) {
                            Text(if(state.enemyHp <= 0) "Tiếp tục" else "Thoát")
                        }
                    }
                }
                return@Column
            }

            when (state.battleState) {
                BattleState.INTRO_DIALOGUE -> DialogueBox(fullText = state.dialogueText, onNext = { viewModel.onDialogueNext() })
                BattleState.ENTERING -> {}
                BattleState.MAIN_MENU -> {
                    Text(state.logMessage.ifEmpty { "Lựa chọn hành động:" }, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                    Row(modifier = Modifier.fillMaxSize()) {
                        ActionButton("ĐÁNH ⚔️", Color(0xFFE53935), { viewModel.toSkillSelect() }, Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        ActionButton("QUÁI THÚ 🐉", Color(0xFF43A047), { showSwapDialog = true }, Modifier.weight(1f))
                    }
                }
                BattleState.SKILL_SELECT -> {
                    Column {
                        Row(modifier = Modifier.weight(1f)) {
                            SkillButton(state.playerSkills.getOrNull(0), { viewModel.onPlayerSkillSelected(0) }, Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            SkillButton(state.playerSkills.getOrNull(1), { viewModel.onPlayerSkillSelected(1) }, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.weight(1f)) {
                            SkillButton(state.playerSkills.getOrNull(2), { viewModel.onPlayerSkillSelected(2) }, Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            SkillButton(state.playerSkills.getOrNull(3), { viewModel.onPlayerSkillSelected(3) }, Modifier.weight(1f))
                        }
                        Button(onClick = { viewModel.toMainMenu() }, modifier = Modifier.align(Alignment.End).padding(top = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Back") }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = color), elevation = ButtonDefaults.buttonElevation(8.dp)) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun InfoBox(name: String, hp: Int, maxHp: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = name.uppercase(), fontWeight = FontWeight.Bold);
            LinearProgressIndicator(progress = { if(maxHp>0) hp.toFloat()/maxHp.toFloat() else 0f }, modifier = Modifier.width(100.dp).height(6.dp), color = Color.Green, trackColor = Color.LightGray);
            Text(text = "$hp / $maxHp", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SkillButton(skill: Skill?, onClick: (Skill) -> Unit, modifier: Modifier = Modifier) {
    val isEnabled = skill != null && skill.currentPp > 0
    Button(onClick = { if (isEnabled && skill != null) onClick(skill) }, enabled = isEnabled, modifier = modifier.fillMaxHeight(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDFE6E9)), shape = RoundedCornerShape(8.dp)) {
        if (skill != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(skill.name, color = Color.Black, fontWeight = FontWeight.Bold)
                Text("${skill.currentPp} PP", color = if(skill.currentPp==0) Color.Red else Color.DarkGray, fontSize = 10.sp)
                Text(skill.type, color = getTypeColor(skill.type), fontSize = 10.sp)
            }
        } else { Text("-", color = Color.Gray) }
    }
}

fun getTypeColor(type: String): Color {
    return when(type) { "Fire" -> Color(0xFFD32F2F); "Water" -> Color(0xFF1976D2); "Leaf" -> Color(0xFF388E3C); else -> Color.DarkGray }
}