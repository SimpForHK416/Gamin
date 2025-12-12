package com.example.gamin.MonsterBattler.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.gamin.MonsterBattler.GauntletMapGenerator
import com.example.gamin.MonsterBattler.MapNode
import com.example.gamin.MonsterBattler.NodeType
import com.example.gamin.MonsterBattler.data.Buff
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.MonsterDbHelper
import com.example.gamin.MonsterBattler.data.Reward
import com.example.gamin.MonsterBattler.data.Skill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

val EVO_CHECK_MAP = mapOf("CRISHY" to "CONFLEVOUR", "RHINPLINK" to "RHITAIN", "DOREWEE" to "DOPERAMI")

data class GameState(
    val currentScreen: String = "INTRO",
    val previousScreen: String? = null,

    val playerMonster: Monster? = null,
    val team: List<Monster> = emptyList(),
    val playerSkills: List<Skill> = emptyList(),
    val currentHp: Int = 0,
    val collectedBuffs: List<Buff> = emptyList(),
    val teamHp: Map<String, Int> = emptyMap(),
    val mapData: List<List<MapNode>> = emptyList(),
    val currentNode: MapNode? = null,
    val rewardsToPick: Int = 0,
    val optionStat: Reward.StatUpgrade? = null,
    val optionHeal: Reward.Heal? = null,
    val optionBuff: Reward.SkillEffect? = null,
    val lastBattledNodeType: NodeType? = null,
    val isDemoMode: Boolean = false,
    val currentScore: Int = 0
)

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()
    private lateinit var dbHelper: MonsterDbHelper

    fun init(context: Context) { dbHelper = MonsterDbHelper(context) }

    fun openLeaderboard() {
        val current = _gameState.value.currentScreen
        _gameState.value = _gameState.value.copy(
            previousScreen = current,
            currentScreen = "LEADERBOARD"
        )
    }

    fun closeLeaderboard() {
        val prev = _gameState.value.previousScreen
        if (prev != null && prev != "GAME_OVER") {
            _gameState.value = _gameState.value.copy(currentScreen = prev, previousScreen = null)
        } else {
            _gameState.value = GameState(currentScreen = "INTRO")
        }
    }

    fun showLeaderboardEndGame() {
        _gameState.value = _gameState.value.copy(
            currentScreen = "LEADERBOARD",
            previousScreen = null
        )
    }

    fun onIntroFinished() { _gameState.value = GameState(currentScreen = "GAME_MODE") }

    fun selectGameMode(isDemo: Boolean) {
        _gameState.value = _gameState.value.copy(isDemoMode = isDemo, currentScreen = "PICKING", mapData = GauntletMapGenerator.generateRandomMap())
    }

    fun pickStarter(monsterName: String) {
        var monster = dbHelper.getMonsterByName(monsterName) ?: return
        val skills = dbHelper.getSkillsForMonster(monsterName)
        if (_gameState.value.isDemoMode) {
            monster = monster.copy(hp = monster.hp * 2, atk = monster.atk * 2, def = monster.def * 2, speed = monster.speed * 2, description = "${monster.description} (DEMO)")
        }
        _gameState.value = _gameState.value.copy(playerMonster = monster, team = listOf(monster), playerSkills = skills, currentHp = monster.hp, teamHp = mapOf(monster.name to monster.hp), currentScreen = "MAP", currentScore = 0)
    }

    fun onNodeClicked(node: MapNode) {
        _gameState.value = _gameState.value.copy(currentNode = node)
        when (node.type) {
            NodeType.BATTLE, NodeType.ELITE, NodeType.BOSS -> { _gameState.value = _gameState.value.copy(currentScreen = "BATTLE") }
            NodeType.MYSTERY -> { _gameState.value = _gameState.value.copy(currentScreen = "MYSTERY") }
        }
    }

    fun onMysteryFinished(newHp: Int) {
        val currentName = _gameState.value.playerMonster?.name ?: return
        val newTeamHp = _gameState.value.teamHp.toMutableMap()
        newTeamHp[currentName] = newHp
        _gameState.value = _gameState.value.copy(currentHp = newHp, teamHp = newTeamHp, currentScreen = "MAP")
    }

    fun onBattleWin(remainingHp: Int, nodeType: NodeType) {
        val currentName = _gameState.value.playerMonster?.name ?: return
        val newTeamHp = _gameState.value.teamHp.toMutableMap()
        newTeamHp[currentName] = remainingHp
        val scoreBonus = when(nodeType) { NodeType.BOSS -> 1000; NodeType.ELITE -> 500; else -> 100 }
        val newScore = _gameState.value.currentScore + scoreBonus + remainingHp

        if (nodeType == NodeType.BOSS) {
            _gameState.value = _gameState.value.copy(currentHp = remainingHp, teamHp = newTeamHp, lastBattledNodeType = nodeType, currentScreen = "MAJOR_UPGRADE", currentScore = newScore)
            return
        }
        val statTypes = listOf("HP", "Atk", "Def", "Speed"); val randomStat = statTypes.random(); val amount = if (randomStat == "HP") 20 else 10
        val statReward = Reward.StatUpgrade(randomStat, amount, "Tăng $amount $randomStat"); val healReward = Reward.Heal(50, "Hồi phục 50% HP"); val buffReward = dbHelper.getRandomOneBuff()?.let { Reward.SkillEffect(it) }
        val picks = if (nodeType == NodeType.ELITE) 2 else 1
        _gameState.value = _gameState.value.copy(currentHp = remainingHp, teamHp = newTeamHp, currentScreen = "BUFF_SELECT", optionStat = statReward, optionHeal = healReward, optionBuff = buffReward, rewardsToPick = picks, lastBattledNodeType = nodeType, currentScore = newScore)
    }

    fun onGameOver() { _gameState.value = _gameState.value.copy(currentScreen = "GAME_OVER") }
    fun onBattleLost() { _gameState.value = GameState(currentScreen = "INTRO") }

    fun updateHpInBattle(hp: Int) {
        val currentName = _gameState.value.playerMonster?.name ?: return
        val newTeamHp = _gameState.value.teamHp.toMutableMap()
        newTeamHp[currentName] = hp
        _gameState.value = _gameState.value.copy(currentHp = hp, teamHp = newTeamHp)
    }

    fun applyReward(reward: Reward, targetMonster: Monster) {
        val currentState = _gameState.value
        var hp = currentState.currentHp
        var buffs = currentState.collectedBuffs
        var optStat = currentState.optionStat; var optHeal = currentState.optionHeal; var optBuff = currentState.optionBuff
        val newTeamHp = currentState.teamHp.toMutableMap()

        val updatedTeam = currentState.team.map { m ->
            if (m.name == targetMonster.name) {
                when(reward) {
                    is Reward.StatUpgrade -> {
                        optStat = null; val newM = when(reward.statName) { "HP" -> m.copy(hp = m.hp + reward.value); "Atk" -> m.copy(atk = m.atk + reward.value); "Def" -> m.copy(def = m.def + reward.value); "Speed" -> m.copy(speed = m.speed + reward.value); else -> m }
                        if (m.name == currentState.playerMonster?.name && reward.statName == "HP") hp += reward.value
                        newM
                    }
                    is Reward.Heal -> {
                        optHeal = null; val currentMonsterHp = newTeamHp[m.name] ?: m.hp; val healAmount = (m.hp * 0.5).toInt(); val newMonsterHp = (currentMonsterHp + healAmount).coerceAtMost(m.hp)
                        newTeamHp[m.name] = newMonsterHp; if (m.name == currentState.playerMonster?.name) hp = newMonsterHp
                        m
                    }
                    else -> m
                }
            } else m
        }
        if (reward is Reward.SkillEffect) { buffs = buffs + reward.buff; optBuff = null }
        val updatedPlayer = updatedTeam.find { it.name == currentState.playerMonster?.name } ?: currentState.playerMonster
        val newPicks = currentState.rewardsToPick - 1

        val nextScreen = if (newPicks <= 0) { if (currentState.lastBattledNodeType == NodeType.BOSS) "MAJOR_UPGRADE" else "MAP" } else "BUFF_SELECT"
        _gameState.value = currentState.copy(team = updatedTeam, playerMonster = updatedPlayer, currentHp = hp, teamHp = newTeamHp, collectedBuffs = buffs, rewardsToPick = newPicks, optionStat = optStat, optionHeal = optHeal, optionBuff = optBuff, currentScreen = nextScreen)
    }

    fun onMajorUpgradeFinished(newTeam: List<Monster>) {
        val upgradedPlayer = newTeam.first(); val newMap = GauntletMapGenerator.generateRandomMap(); val newTeamHp = _gameState.value.teamHp.toMutableMap()
        newTeam.forEach { newTeamHp[it.name] = it.hp }
        val newScore = _gameState.value.currentScore + 2000
        _gameState.value = _gameState.value.copy(playerMonster = upgradedPlayer, team = newTeam, currentHp = upgradedPlayer.hp, teamHp = newTeamHp, mapData = newMap, currentNode = null, currentScreen = "MAP", currentScore = newScore)
    }

    fun getStarters(): List<Monster> { return dbHelper.getStartingMonsters() }

    fun getAvailableRecruits(): List<Monster> {
        val currentTeamNames = _gameState.value.team.map { it.name }; val allStarters = dbHelper.getStartingMonsters()
        return allStarters.filter { starter -> val evoName = EVO_CHECK_MAP[starter.name]; val isAlreadyInTeam = currentTeamNames.contains(starter.name); val isEvoInTeam = evoName != null && currentTeamNames.contains(evoName); !isAlreadyInTeam && !isEvoInTeam }
    }

    fun openTeamManagement() { _gameState.value = _gameState.value.copy(currentScreen = "TEAM_MANAGEMENT") }
    fun closeTeamManagement() { _gameState.value = _gameState.value.copy(currentScreen = "MAP") }

    fun swapTeamMembers(index1: Int, index2: Int) {
        val currentTeam = _gameState.value.team.toMutableList()
        if (index1 in currentTeam.indices && index2 in currentTeam.indices) {
            val temp = currentTeam[index1]; currentTeam[index1] = currentTeam[index2]; currentTeam[index2] = temp
            val newActive = currentTeam[0]; val newSkills = dbHelper.getSkillsForMonster(newActive.name); val newHp = _gameState.value.teamHp[newActive.name] ?: newActive.hp
            _gameState.value = _gameState.value.copy(team = currentTeam, playerMonster = newActive, playerSkills = newSkills, currentHp = newHp)
        }
    }

    fun switchActiveMonsterInBattle(newMonster: Monster) {
        val newSkills = dbHelper.getSkillsForMonster(newMonster.name); val newHp = _gameState.value.teamHp[newMonster.name] ?: newMonster.hp
        _gameState.value = _gameState.value.copy(playerMonster = newMonster, playerSkills = newSkills, currentHp = newHp)
    }

    fun getSkills(monsterName: String): List<Skill> { return dbHelper.getSkillsForMonster(monsterName) }

    fun getSurvivors(): List<Monster> {
        val teamHp = _gameState.value.teamHp
        return _gameState.value.team.filter { (teamHp[it.name] ?: it.hp) > 0 }
    }
}