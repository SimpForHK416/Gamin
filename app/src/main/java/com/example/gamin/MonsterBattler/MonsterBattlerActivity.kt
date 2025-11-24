package com.example.gamin.MonsterBattler

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.gamin.MonsterBattler.data.Buff
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.MonsterDbHelper
import com.example.gamin.MonsterBattler.data.Reward
import com.example.gamin.MonsterBattler.data.Skill
import com.example.gamin.MonsterBattler.ui.BattleScreen
import com.example.gamin.MonsterBattler.ui.BuffSelectionScreen
import com.example.gamin.MonsterBattler.ui.GauntletMapScreen
import com.example.gamin.MonsterBattler.ui.IntroScreen
import com.example.gamin.MonsterBattler.ui.PickingScreen
import com.example.gamin.ui.theme.GaminTheme

class MonsterBattlerActivity : ComponentActivity() {

    private val dbHelper by lazy { MonsterDbHelper(this.applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    var currentScreen by remember { mutableStateOf("INTRO") }
                    var playerMonsterName by remember { mutableStateOf("") }
                    var playerMonster: Monster? by remember { mutableStateOf(null) }
                    var playerSkills by remember { mutableStateOf(emptyList<Skill>()) }
                    var playerCurrentHp by remember { mutableIntStateOf(0) }

                    var collectedBuffs by remember { mutableStateOf(listOf<Buff>()) }
                    var optionStat by remember { mutableStateOf<Reward.StatUpgrade?>(null) }
                    var optionHeal by remember { mutableStateOf<Reward.Heal?>(null) }
                    var optionBuff by remember { mutableStateOf<Reward.SkillEffect?>(null) }

                    var mapData by remember { mutableStateOf(MapGenerator.generateMap()) }
                    var currentNode by remember { mutableStateOf<MapNode?>(null) }
                    var enemyMonster: Monster? by remember { mutableStateOf(null) }
                    var enemySkills by remember { mutableStateOf(emptyList<Skill>()) }

                    val starters = remember { dbHelper.getStartingMonsters() }

                    fun resetGame() {
                        playerMonsterName = ""
                        playerMonster = null
                        playerSkills = emptyList()
                        collectedBuffs = emptyList()
                        playerCurrentHp = 0
                        mapData = MapGenerator.generateMap()
                        currentNode = null
                        currentScreen = "INTRO"
                    }

                    when (currentScreen) {
                        "INTRO" -> IntroScreen(onIntroFinished = { currentScreen = "PICKING" })
                        "PICKING" -> PickingScreen(
                            monsters = starters,
                            onMonsterSelected = { monsterName ->
                                playerMonsterName = monsterName
                                playerMonster = dbHelper.getMonsterByName(monsterName)
                                playerSkills = dbHelper.getSkillsForMonster(monsterName)
                                playerCurrentHp = playerMonster!!.hp
                                currentScreen = "MAP"
                            }
                        )
                        "MAP" -> GauntletMapScreen(
                            mapLevels = mapData,
                            currentNode = currentNode,
                            onNodeClicked = { node ->
                                if (node.type == NodeType.BATTLE || node.type == NodeType.ELITE || node.type == NodeType.BOSS) {
                                    val allMonsters = dbHelper.getAllMonsters()
                                    val bannedNames = listOf("CRISHY", "CONFLEVOUR", "RHINPLINK", "RHITAIN", "DOREWEE", "DOPERAMI")
                                    val wildEnemies = allMonsters.filter { !bannedNames.contains(it.name) }
                                    if (wildEnemies.isNotEmpty()) {
                                        val randomEnemy = wildEnemies.random()
                                        enemyMonster = randomEnemy
                                        enemySkills = dbHelper.getSkillsForMonster(randomEnemy.name)
                                        currentNode = node
                                        currentScreen = "BATTLE"
                                    }
                                } else if (node.type == NodeType.MYSTERY) { currentNode = node }
                            }
                        )
                        "BATTLE" -> {
                            if (playerMonster != null && enemyMonster != null) {
                                BattleScreen(
                                    playerMonster = playerMonster!!,
                                    enemyMonster = enemyMonster!!,
                                    playerSkills = playerSkills,
                                    enemySkills = enemySkills,
                                    activeBuffs = collectedBuffs,
                                    opponentDialogue = "Ta là ${enemyMonster!!.name}!",
                                    initialPlayerHp = playerCurrentHp,
                                    onBattleEnd = { isWin, remainingHp ->
                                        if (isWin) {
                                            playerCurrentHp = remainingHp
                                            val statTypes = listOf("HP", "Atk", "Def", "Speed")
                                            val randomStat = statTypes.random()
                                            val amount = if (randomStat == "HP") 20 else 10
                                            optionStat = Reward.StatUpgrade(randomStat, amount, "Tăng $amount $randomStat cho ${playerMonster!!.name}")
                                            optionHeal = Reward.Heal(50, "Hồi phục 50% HP")
                                            val randomBuff = dbHelper.getRandomOneBuff()
                                            optionBuff = if(randomBuff != null) Reward.SkillEffect(randomBuff) else null
                                            currentScreen = "BUFF_SELECT"
                                        } else { resetGame() }
                                    }
                                )
                            }
                        }
                        "BUFF_SELECT" -> {
                            if (optionStat != null && optionHeal != null && optionBuff != null) {
                                BuffSelectionScreen(
                                    option1 = optionStat!!, option2 = optionHeal!!, option3 = optionBuff!!,
                                    onRewardSelected = { reward ->
                                        when(reward) {
                                            is Reward.StatUpgrade -> {
                                                playerMonster = when(reward.statName) {
                                                    "HP" -> playerMonster!!.copy(hp = playerMonster!!.hp + reward.value)
                                                    "Atk" -> playerMonster!!.copy(atk = playerMonster!!.atk + reward.value)
                                                    "Def" -> playerMonster!!.copy(def = playerMonster!!.def + reward.value)
                                                    "Speed" -> playerMonster!!.copy(speed = playerMonster!!.speed + reward.value)
                                                    else -> playerMonster
                                                }
                                                if(reward.statName == "HP") playerCurrentHp += reward.value
                                            }
                                            is Reward.Heal -> {
                                                val healAmount = (playerMonster!!.hp * 0.5).toInt()
                                                playerCurrentHp = (playerCurrentHp + healAmount).coerceAtMost(playerMonster!!.hp)
                                            }
                                            is Reward.SkillEffect -> collectedBuffs = collectedBuffs + reward.buff
                                        }
                                        currentScreen = "MAP"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}