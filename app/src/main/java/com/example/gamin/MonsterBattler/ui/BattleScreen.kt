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
import com.example.gamin.MonsterBattler.BattleMechanics
import com.example.gamin.MonsterBattler.StatusEffect
import com.example.gamin.MonsterBattler.data.Buff
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.Skill
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BattleState {
    INTRO_DIALOGUE, ENTERING, MAIN_MENU, SKILL_SELECT, MONSTER_SELECT,
    PLAYER_ATTACKING, ENEMY_ATTACKING, BATTLE_END, WARNING_DIALOGUE
}

@Composable
fun BattleScreen(
    playerMonster: Monster,
    enemyMonster: Monster,
    playerSkills: List<Skill>,
    enemySkills: List<Skill>,
    activeBuffs: List<Buff>,
    opponentDialogue: String,
    initialPlayerHp: Int,
    onBattleEnd: (Boolean, Int) -> Unit
) {
    var currentState by remember { mutableStateOf(BattleState.INTRO_DIALOGUE) }

    var currentPlayerHp by remember { mutableIntStateOf(initialPlayerHp) }
    var currentEnemyHp by remember { mutableIntStateOf(enemyMonster.hp) }
    var battleLog by remember { mutableStateOf("") }
    var logColor by remember { mutableStateOf(Color.Black) }
    var warningMessage by remember { mutableStateOf("") }

    // Buff Turns Management
    var playerBuffTurns by remember { mutableIntStateOf(0) }
    var enemyBuffTurns by remember { mutableIntStateOf(0) }

    // PP Management (Create mutable copies)
    val currentPlayerSkills = remember { playerSkills.map { it.copy() }.toMutableStateList() }
    val currentEnemySkills = remember { enemySkills.map { it.copy() }.toMutableList() }

    // Status Effects
    var enemyStatus by remember { mutableStateOf(StatusEffect.NONE) }
    var enemyStatusTurns by remember { mutableIntStateOf(0) }

    // Animations
    val playerOffsetX = remember { Animatable(-1000f) }
    val enemyOffsetX = remember { Animatable(1000f) }
    val scope = rememberCoroutineScope()

    fun getSkillEffectDescription(skillName: String): String {
        return when(skillName) {
            "Nóng Giận", "Phấn Hoa", "Thiền Định", "Gầm Gừ" -> "Đã TĂNG Tấn Công!"
            "Vỏ Cứng", "Thu Mình", "Giáp Gai", "Tích Tụ" -> "Đã TĂNG Phòng Thủ!"
            "Vũ Điệu", "Vũ Điệu Mưa" -> "Đã TĂNG Tốc Độ!"
            "Quang Hợp", "Mưa Rào", "Hồi Máu" -> "Đã Hồi Phục Máu!"
            else -> "Chỉ số bản thân đã tăng!"
        }
    }

    fun tryApplyBuffEffect(skill: Skill) {
        val applicableBuff = activeBuffs.find { it.targetType == "Any" || it.targetType == skill.type }
        if (applicableBuff != null) {
            val chance = 0.1
            if (Math.random() < chance) {
                val newEffect = when(applicableBuff.effectType) {
                    "STUN" -> StatusEffect.STUN
                    "BURN" -> StatusEffect.BURN
                    "BREAK_DEF" -> StatusEffect.BREAK_DEF
                    "WEAKEN" -> StatusEffect.WEAKEN
                    else -> StatusEffect.NONE
                }
                if (enemyStatus != StatusEffect.STUN) {
                    enemyStatus = newEffect
                    enemyStatusTurns = if (newEffect == StatusEffect.STUN) 1 else 2
                }
            }
        }
    }

    fun performTurn(skillIndex: Int) {
        val chosenSkill = currentPlayerSkills[skillIndex]

        scope.launch {
            currentState = BattleState.PLAYER_ATTACKING

            // Reduce PP
            chosenSkill.currentPp--
            currentPlayerSkills[skillIndex] = chosenSkill.copy() // Trigger UI update

            if (chosenSkill.power > 0) {
                launch { playerOffsetX.animateTo(150f, tween(200)); playerOffsetX.animateTo(0f, tween(200)) }
                val damage = BattleMechanics.calculateDamage(playerMonster, enemyMonster, chosenSkill, playerBuffTurns > 0, enemyBuffTurns > 0)
                val typeMod = BattleMechanics.getTypeEffectiveness(chosenSkill.type, enemyMonster.type)

                val effText = when {
                    typeMod > 1.0f -> { logColor = Color(0xFFD32F2F); "Đòn đánh rất hiệu quả!" }
                    typeMod < 1.0f -> { logColor = Color.Gray; "Đòn đánh không hiệu quả lắm..." }
                    else -> { logColor = Color.Black; "" }
                }

                currentEnemyHp = (currentEnemyHp - damage).coerceAtLeast(0)
                battleLog = "${playerMonster.name} dùng ${chosenSkill.name}!"
                if (effText.isNotEmpty()) battleLog += "\n$effText"
                battleLog += "\nGây $damage sát thương!"

                tryApplyBuffEffect(chosenSkill)
                if (enemyStatus != StatusEffect.NONE && enemyStatusTurns > 0) battleLog += "\nĐịch dính: $enemyStatus!"
            } else {
                logColor = Color(0xFF1976D2)
                battleLog = "${playerMonster.name} dùng ${chosenSkill.name}!"
                if (chosenSkill.name == "Quang Hợp" || chosenSkill.name == "Hồi Máu" || chosenSkill.name == "Mưa Rào") {
                    val heal = 30
                    currentPlayerHp = (currentPlayerHp + heal).coerceAtMost(playerMonster.hp)
                    battleLog += "\nĐã hồi phục 30 HP!"
                } else {
                    playerBuffTurns = 3
                    battleLog += "\n${getSkillEffectDescription(chosenSkill.name)}"
                }
            }
            delay(2000)

            if (currentEnemyHp <= 0) {
                logColor = Color(0xFFFFD700)
                battleLog = "CHIẾN THẮNG!"
                currentState = BattleState.BATTLE_END
                return@launch
            }

            currentState = BattleState.ENEMY_ATTACKING

            if (enemyStatus == StatusEffect.STUN) {
                logColor = Color.Magenta
                battleLog = "Địch bị CHOÁNG! Mất lượt!"
                delay(1500)
            } else {
                if (enemyStatus == StatusEffect.BURN) {
                    currentEnemyHp = (currentEnemyHp - 10).coerceAtLeast(0)
                    logColor = Color(0xFFFF5722)
                    battleLog = "Địch bị BỎNG! Mất 10 HP."
                    delay(1000)
                }

                if (currentEnemyHp > 0) {
                    val availableEnemySkills = currentEnemySkills.filter { it.currentPp > 0 }
                    if (availableEnemySkills.isNotEmpty()) {
                        val enemySkill = availableEnemySkills.random()
                        enemySkill.currentPp--

                        if (enemySkill.power > 0) {
                            launch { enemyOffsetX.animateTo(-150f, tween(200)); enemyOffsetX.animateTo(0f, tween(200)) }
                            val damage = BattleMechanics.calculateDamage(enemyMonster, playerMonster, enemySkill, enemyBuffTurns > 0, playerBuffTurns > 0)

                            val enemyTypeMod = BattleMechanics.getTypeEffectiveness(enemySkill.type, playerMonster.type)
                            val enemyEffText = if (enemyTypeMod > 1.0f) "Đòn đánh rất hiệu quả!" else if (enemyTypeMod < 1.0f) "Đòn đánh không hiệu quả lắm..." else ""
                            logColor = if(enemyTypeMod > 1f) Color(0xFFD32F2F) else Color.Black

                            currentPlayerHp = (currentPlayerHp - damage).coerceAtLeast(0)
                            battleLog = "Địch dùng ${enemySkill.name}!"
                            if (enemyEffText.isNotEmpty()) battleLog += "\n$enemyEffText"
                            battleLog += "\nBạn mất $damage máu!"
                        } else {
                            logColor = Color(0xFF1976D2)
                            battleLog = "Địch dùng ${enemySkill.name}!"
                            if (enemySkill.name == "Quang Hợp" || enemySkill.name == "Hồi Máu" || enemySkill.name == "Mưa Rào") {
                                val heal = 30
                                currentEnemyHp = (currentEnemyHp + heal).coerceAtMost(enemyMonster.hp)
                                battleLog += "\nĐịch hồi phục 30 HP!"
                            } else {
                                enemyBuffTurns = 3
                                battleLog += "\n${getSkillEffectDescription(enemySkill.name)}"
                            }
                        }
                    } else {
                        logColor = Color.Gray
                        battleLog = "Địch đã hết PP! Nó đứng yên!"
                    }
                }
                delay(2000)
            }

            if (playerBuffTurns > 0) playerBuffTurns--
            if (enemyBuffTurns > 0) enemyBuffTurns--
            if (enemyStatusTurns > 0) { enemyStatusTurns--; if (enemyStatusTurns == 0) enemyStatus = StatusEffect.NONE }

            if (currentPlayerHp <= 0) {
                logColor = Color.Red
                battleLog = "THẤT BẠI!"
                currentState = BattleState.BATTLE_END
                return@launch
            }

            currentState = BattleState.MAIN_MENU
            logColor = Color.Black
            battleLog = "Bạn sẽ làm gì?"
        }
    }

    fun startEntranceAnimation() {
        currentState = BattleState.ENTERING
        scope.launch { launch { playerOffsetX.animateTo(0f, tween(1000)) }; launch { enemyOffsetX.animateTo(0f, tween(1000)) }; currentState = BattleState.MAIN_MENU; battleLog = "Trận đấu bắt đầu!" }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F8F8))) {
        Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
            if (currentState != BattleState.INTRO_DIALOGUE) {
                Column(modifier = Modifier.align(Alignment.TopEnd).padding(top=40.dp, end=20.dp).graphicsLayer { translationX = enemyOffsetX.value }, horizontalAlignment = Alignment.CenterHorizontally) {
                    InfoBox(enemyMonster.name, currentEnemyHp, enemyMonster.hp)
                    if (enemyBuffTurns > 0) Text("▲ BUFF ($enemyBuffTurns)", color = Color.Red, fontWeight = FontWeight.Bold)
                    if (enemyStatus != StatusEffect.NONE) Text(enemyStatus.name, color = Color.Magenta, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(painter = painterFor(enemyMonster.name), contentDescription = null, modifier = Modifier.size(150.dp), contentScale = ContentScale.Fit)
                }
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(bottom=20.dp, start=20.dp).graphicsLayer { translationX = playerOffsetX.value }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painter = painterFor("${playerMonster.name}_back"), contentDescription = null, modifier = Modifier.size(180.dp), contentScale = ContentScale.Fit)
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoBox(playerMonster.name, currentPlayerHp, playerMonster.hp)
                    if (playerBuffTurns > 0) Text("▲ BUFF ($playerBuffTurns)", color = Color.Blue, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(modifier = Modifier.weight(0.4f).fillMaxWidth().background(Color(0xFF2D3436)).padding(16.dp)) {
            if (currentState == BattleState.PLAYER_ATTACKING || currentState == BattleState.ENEMY_ATTACKING || currentState == BattleState.BATTLE_END) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White, RoundedCornerShape(8.dp)).padding(16.dp)) {
                    Text(battleLog, fontSize = 18.sp, color = logColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                    if (currentState == BattleState.BATTLE_END) {
                        Button(onClick = { onBattleEnd(currentEnemyHp <= 0, currentPlayerHp) }, modifier = Modifier.align(Alignment.BottomEnd)) { Text(if(currentEnemyHp <= 0) "Tiếp tục" else "Thoát") }
                    }
                }
                return@Column
            }
            when (currentState) {
                BattleState.INTRO_DIALOGUE -> DialogueBox(fullText = "Đối thủ: $opponentDialogue", onNext = { startEntranceAnimation() })
                BattleState.ENTERING -> {}
                BattleState.MAIN_MENU -> {
                    Text(battleLog.ifEmpty { "Lựa chọn hành động:" }, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                    Row(modifier = Modifier.fillMaxSize()) {
                        ActionButton("ĐÁNH ⚔️", Color(0xFFE53935), { currentState = BattleState.SKILL_SELECT }, Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        ActionButton("QUÁI THÚ 🐉", Color(0xFF43A047), { currentState = BattleState.MONSTER_SELECT }, Modifier.weight(1f))
                    }
                }
                BattleState.SKILL_SELECT -> {
                    Column {
                        Row(modifier = Modifier.weight(1f)) {
                            SkillButton(currentPlayerSkills.getOrNull(0), { performTurn(0) }, Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            SkillButton(currentPlayerSkills.getOrNull(1), { performTurn(1) }, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.weight(1f)) {
                            SkillButton(currentPlayerSkills.getOrNull(2), { performTurn(2) }, Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            SkillButton(currentPlayerSkills.getOrNull(3), { performTurn(3) }, Modifier.weight(1f))
                        }
                        Button(onClick = { currentState = BattleState.MAIN_MENU }, modifier = Modifier.align(Alignment.End).padding(top = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Back") }
                    }
                }
                BattleState.MONSTER_SELECT -> { Button(onClick = { currentState = BattleState.MAIN_MENU }) { Text("Back") } }
                BattleState.WARNING_DIALOGUE -> DialogueBox(fullText = warningMessage, onNext = { currentState = BattleState.MAIN_MENU })
                else -> {}
            }
        }
    }
}

// --- Helper Functions ---
@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) { Button(onClick = onClick, modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = color), elevation = ButtonDefaults.buttonElevation(8.dp)) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
@Composable
fun InfoBox(name: String, hp: Int, maxHp: Int) { Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) { Column(modifier = Modifier.padding(8.dp)) { Text(text = name.uppercase(), fontWeight = FontWeight.Bold); LinearProgressIndicator(progress = { if(maxHp>0) hp.toFloat()/maxHp.toFloat() else 0f }, modifier = Modifier.width(100.dp).height(6.dp), color = Color.Green, trackColor = Color.LightGray); Text(text = "$hp / $maxHp", fontSize = 12.sp, color = Color.Gray) } } }
@Composable
fun SkillButton(skill: Skill?, onClick: (Skill) -> Unit, modifier: Modifier = Modifier) {
    val isEnabled = skill != null && skill.currentPp > 0
    // FIXED HERE: Passing 'skill' to 'onClick'
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
fun getTypeColor(type: String): Color { return when(type) { "Fire" -> Color(0xFFD32F2F); "Water" -> Color(0xFF1976D2); "Leaf" -> Color(0xFF388E3C); else -> Color.DarkGray } }