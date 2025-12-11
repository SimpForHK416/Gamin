package com.example.gamin.MonsterBattler.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamin.MonsterBattler.BattleMechanics
import com.example.gamin.MonsterBattler.StatusEffect
import com.example.gamin.MonsterBattler.data.Buff
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.Skill
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- CẬP NHẬT ENUM (THÊM PLAYER_FAINTED) ---
enum class BattleState {
    INTRO_DIALOGUE, ENTERING, MAIN_MENU, SKILL_SELECT, MONSTER_SELECT,
    PLAYER_ATTACKING, ENEMY_ATTACKING, BATTLE_END, WARNING_DIALOGUE,
    PLAYER_FAINTED // <-- QUAN TRỌNG: Phải có dòng này thì Activity mới chạy được
}

data class BattleUiState(
    val playerMonster: Monster? = null,
    val enemyMonster: Monster? = null,
    val playerHp: Int = 0,
    val enemyHp: Int = 0,
    val playerSkills: List<Skill> = emptyList(),
    val enemySkills: List<Skill> = emptyList(),

    val battleState: BattleState = BattleState.INTRO_DIALOGUE,
    val logMessage: String = "",
    val logColor: Color = Color.Black,

    val playerBuffTurns: Int = 0,
    val enemyBuffTurns: Int = 0,
    val enemyStatus: StatusEffect = StatusEffect.NONE,
    val enemyStatusTurns: Int = 0,

    val dialogueText: String = "",
    val isBoss: Boolean = false
)

class BattleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())
    val uiState = _uiState.asStateFlow()

    private var activeBuffs: List<Buff> = emptyList()
    private var onBattleEndCallback: ((Boolean, Int) -> Unit)? = null

    // Hàm Init
    fun initBattle(
        player: Monster, enemy: Monster, pSkills: List<Skill>, eSkills: List<Skill>,
        buffs: List<Buff>, currentHp: Int, isBoss: Boolean, onEnd: (Boolean, Int) -> Unit
    ) {
        activeBuffs = buffs
        onBattleEndCallback = onEnd
        _uiState.value = BattleUiState(
            playerMonster = player, enemyMonster = enemy, playerHp = currentHp, enemyHp = enemy.hp,
            playerSkills = pSkills.map { it.copy() }, enemySkills = eSkills.map { it.copy() },
            battleState = BattleState.INTRO_DIALOGUE,
            dialogueText = "Ta là ${enemy.name}! Ngươi không có cửa thắng đâu!",
            isBoss = isBoss
        )
    }

    // --- HÀM ĐỔI QUÁI CHỦ ĐỘNG (Mất lượt) ---
    fun switchPlayerMonster(newMonster: Monster, newSkills: List<Skill>) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            playerMonster = newMonster,
            playerSkills = newSkills.map { it.copy() },
            playerHp = newMonster.hp,
            playerBuffTurns = 0, // Xóa buff khi đổi
            battleState = BattleState.ENEMY_ATTACKING, // Đổi xong địch đánh luôn
            logMessage = "Bạn gọi ${newMonster.name} ra trận!",
            logColor = Color.Blue
        )
        // Kích hoạt lượt địch sau khi đổi
        viewModelScope.launch {
            delay(1500)
            performEnemyTurn()
        }
    }

    // --- HÀM ĐỔI QUÁI KHI CHẾT (Không mất lượt) ---
    fun replaceFaintedMonster(newMonster: Monster, newSkills: List<Skill>) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            playerMonster = newMonster,
            playerSkills = newSkills.map { it.copy() },
            playerHp = newMonster.hp,
            playerBuffTurns = 0,
            battleState = BattleState.MAIN_MENU, // Về menu để đánh tiếp
            logMessage = "Cố lên ${newMonster.name}!",
            logColor = Color.Black
        )
    }

    // --- HÀM KẾT THÚC TRẬN ĐẤU (THUA HẲN) ---
    fun triggerDefeat() {
        endBattle(false)
    }

    fun onDialogueNext() {
        if (_uiState.value.battleState == BattleState.INTRO_DIALOGUE) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(battleState = BattleState.ENTERING)
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    battleState = BattleState.MAIN_MENU, logMessage = "Trận đấu bắt đầu!", logColor = Color.Black
                )
            }
        }
    }

    fun onPlayerSkillSelected(skillIndex: Int) {
        val currentState = _uiState.value
        val player = currentState.playerMonster ?: return
        val enemy = currentState.enemyMonster ?: return
        val skill = currentState.playerSkills[skillIndex]

        if (skill.currentPp <= 0) return

        viewModelScope.launch {
            val updatedSkills = currentState.playerSkills.toMutableList().apply { this[skillIndex] = skill.copy(currentPp = skill.currentPp - 1) }
            _uiState.value = currentState.copy(battleState = BattleState.PLAYER_ATTACKING, playerSkills = updatedSkills)

            var log = "${player.name} dùng ${skill.name}!"
            var color = Color.Black
            var newEnemyHp = currentState.enemyHp
            var newPlayerHp = currentState.playerHp
            var newEnemyStatus = currentState.enemyStatus
            var newEnemyStatusTurns = currentState.enemyStatusTurns
            var newPlayerBuffTurns = currentState.playerBuffTurns

            if (skill.power > 0) {
                val damage = BattleMechanics.calculateDamage(player, enemy, skill, currentState.playerBuffTurns > 0, currentState.enemyBuffTurns > 0, currentState.enemyStatus == StatusEffect.BREAK_DEF, false)
                val typeMod = BattleMechanics.getTypeEffectiveness(skill.type, enemy.type)
                if (typeMod > 1f) { log += "\nĐòn đánh rất hiệu quả!"; color = Color(0xFFD32F2F) }
                else if (typeMod < 1f) { log += "\nĐòn đánh không hiệu quả..."; color = Color.Gray }
                log += "\nGây $damage sát thương!"
                newEnemyHp = (newEnemyHp - damage).coerceAtLeast(0)
                val (effect, turns) = BattleMechanics.calculateStatusChance(skill, activeBuffs)
                if (currentState.enemyStatus != StatusEffect.STUN && effect != StatusEffect.NONE) { newEnemyStatus = effect; newEnemyStatusTurns = turns; log += "\nĐịch dính hiệu ứng: $effect!" }
            } else {
                color = Color(0xFF1976D2)
                if (skill.name == "Quang Hợp" || skill.name == "Hồi Máu" || skill.name == "Mưa Rào") { newPlayerHp = (newPlayerHp + 30).coerceAtMost(player.hp); log += "\n" + BattleMechanics.getSkillEffectDescription(skill.name) }
                else { newPlayerBuffTurns = 3; log += "\n" + BattleMechanics.getSkillEffectDescription(skill.name) }
            }

            _uiState.value = _uiState.value.copy(logMessage = log, logColor = color, enemyHp = newEnemyHp, playerHp = newPlayerHp, enemyStatus = newEnemyStatus, enemyStatusTurns = newEnemyStatusTurns, playerBuffTurns = newPlayerBuffTurns)
            delay(2000)

            if (newEnemyHp <= 0) { endBattle(true); return@launch }
            performEnemyTurn()
        }
    }

    private suspend fun performEnemyTurn() {
        _uiState.value = _uiState.value.copy(battleState = BattleState.ENEMY_ATTACKING)
        val state = _uiState.value
        val enemy = state.enemyMonster ?: return
        val player = state.playerMonster ?: return
        var currentEnemyHp = state.enemyHp

        if (state.enemyStatus == StatusEffect.STUN) {
            _uiState.value = state.copy(logMessage = "Địch bị CHOÁNG! Mất lượt!", logColor = Color.Magenta)
            delay(1500)
            endTurnCleanup()
            return
        }

        if (state.enemyStatus == StatusEffect.BURN) {
            currentEnemyHp = (currentEnemyHp - 10).coerceAtLeast(0)
            _uiState.value = state.copy(enemyHp = currentEnemyHp, logMessage = "Địch bị BỎNG! Mất 10 HP.", logColor = Color(0xFFFF5722))
            delay(1000)
            if (currentEnemyHp <= 0) { endBattle(true); return }
        }

        val enemySkill = BattleMechanics.decideEnemyMove(enemy, currentEnemyHp, state.enemySkills)

        if (enemySkill != null) {
            val updatedEnemySkills = state.enemySkills.toMutableList()
            val index = updatedEnemySkills.indexOf(enemySkill)
            if (index != -1) updatedEnemySkills[index] = enemySkill.copy(currentPp = enemySkill.currentPp - 1)
            _uiState.value = _uiState.value.copy(enemySkills = updatedEnemySkills)

            var log = "${enemy.name} dùng ${enemySkill.name}!"
            var color = Color.Black
            var newPlayerHp = state.playerHp
            var newEnemyBuffTurns = state.enemyBuffTurns

            if (enemySkill.power > 0) {
                val damage = BattleMechanics.calculateDamage(enemy, player, enemySkill, state.enemyBuffTurns > 0, state.playerBuffTurns > 0, false, state.enemyStatus == StatusEffect.WEAKEN)
                val typeMod = BattleMechanics.getTypeEffectiveness(enemySkill.type, player.type)
                if (typeMod > 1f) { log += "\nĐòn đánh rất hiệu quả!"; color = Color(0xFFD32F2F) }
                log += "\nGây $damage sát thương lên bạn!"
                newPlayerHp = (newPlayerHp - damage).coerceAtLeast(0)
            } else {
                color = Color(0xFF1976D2)
                if (enemySkill.name == "Quang Hợp" || enemySkill.name == "Hồi Máu" || enemySkill.name == "Mưa Rào") { currentEnemyHp = (currentEnemyHp + 30).coerceAtMost(enemy.hp); log += "\nĐịch hồi phục 30 HP!" }
                else { newEnemyBuffTurns = 3; log += "\n" + BattleMechanics.getSkillEffectDescription(enemySkill.name) }
            }

            _uiState.value = _uiState.value.copy(logMessage = log, logColor = color, playerHp = newPlayerHp, enemyHp = currentEnemyHp, enemyBuffTurns = newEnemyBuffTurns)
        } else {
            _uiState.value = state.copy(logMessage = "Địch hết PP! Đứng yên.", logColor = Color.Gray)
        }

        delay(2000)

        // --- NẾU THUA: CHUYỂN SANG TRẠNG THÁI PLAYER_FAINTED ---
        if (_uiState.value.playerHp <= 0) {
            _uiState.value = _uiState.value.copy(
                battleState = BattleState.PLAYER_FAINTED,
                logMessage = "${player.name} đã gục ngã!",
                logColor = Color.Red
            )
            return
        }

        endTurnCleanup()
    }

    private fun endTurnCleanup() {
        var pBuff = _uiState.value.playerBuffTurns
        var eBuff = _uiState.value.enemyBuffTurns
        var eStatusTurns = _uiState.value.enemyStatusTurns
        var eStatus = _uiState.value.enemyStatus

        if (pBuff > 0) pBuff--
        if (eBuff > 0) eBuff--
        if (eStatusTurns > 0) {
            eStatusTurns--
            if (eStatusTurns == 0) eStatus = StatusEffect.NONE
        }

        _uiState.value = _uiState.value.copy(
            battleState = BattleState.MAIN_MENU, logMessage = "Bạn sẽ làm gì?", logColor = Color.Black,
            playerBuffTurns = pBuff, enemyBuffTurns = eBuff, enemyStatusTurns = eStatusTurns, enemyStatus = eStatus
        )
    }

    private fun endBattle(isWin: Boolean) {
        val log = if (isWin) "CHIẾN THẮNG!" else "THẤT BẠI!"
        val color = if (isWin) Color(0xFFFFD700) else Color.Red
        _uiState.value = _uiState.value.copy(battleState = BattleState.BATTLE_END, logMessage = log, logColor = color)
    }

    fun onEndBattleConfirmed() {
        val state = _uiState.value
        val isWin = state.enemyHp <= 0
        onBattleEndCallback?.invoke(isWin, state.playerHp)
    }

    fun toSkillSelect() { _uiState.value = _uiState.value.copy(battleState = BattleState.SKILL_SELECT) }
    fun toMonsterSelect() { _uiState.value = _uiState.value.copy(battleState = BattleState.MONSTER_SELECT) }
    fun toMainMenu() { _uiState.value = _uiState.value.copy(battleState = BattleState.MAIN_MENU) }
}