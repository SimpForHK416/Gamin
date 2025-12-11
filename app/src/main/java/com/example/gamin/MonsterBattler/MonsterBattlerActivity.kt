package com.example.gamin.MonsterBattler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.MonsterDbHelper
import com.example.gamin.MonsterBattler.data.Reward
import com.example.gamin.MonsterBattler.ui.BattleScreen
import com.example.gamin.MonsterBattler.ui.BattleState
import com.example.gamin.MonsterBattler.ui.BattleViewModel
import com.example.gamin.MonsterBattler.ui.BuffSelectionScreenModified
import com.example.gamin.MonsterBattler.ui.GameModeScreen
import com.example.gamin.MonsterBattler.ui.GameViewModel
import com.example.gamin.MonsterBattler.ui.GauntletMapScreen
import com.example.gamin.MonsterBattler.ui.IntroScreen
import com.example.gamin.MonsterBattler.ui.LeaderboardScreen
import com.example.gamin.MonsterBattler.ui.MajorUpgradeScreen
import com.example.gamin.MonsterBattler.ui.MysteryScreen
import com.example.gamin.MonsterBattler.ui.PickingScreen
import com.example.gamin.MonsterBattler.ui.SaveScoreDialog
import com.example.gamin.MonsterBattler.ui.TeamManagementScreen
import com.example.gamin.MonsterBattler.ui.painterFor
import com.example.gamin.ui.theme.GaminTheme

const val GAME_ID_MONSTER_BATTLER = "monster_battler"

class MonsterBattlerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this.applicationContext

        setContent {
            GaminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val gameViewModel: GameViewModel = viewModel()
                    LaunchedEffect(Unit) { gameViewModel.init(context) }
                    val gameState by gameViewModel.gameState.collectAsState()

                    val battleViewModel: BattleViewModel = viewModel()
                    val battleState by battleViewModel.uiState.collectAsState()

                    val dbHelper = remember { MonsterDbHelper(context) }

                    var pendingReward by remember { mutableStateOf<Reward?>(null) }
                    var showForceSwapDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(battleState.battleState) {
                        if (battleState.battleState == BattleState.PLAYER_FAINTED) {
                            val survivors = gameViewModel.getSurvivors()
                            if (survivors.isNotEmpty()) {
                                showForceSwapDialog = true
                            } else {
                                gameViewModel.onGameOver()
                            }
                        }
                    }

                    LaunchedEffect(battleState.playerHp) {
                        if (gameState.currentScreen == "BATTLE" && gameState.playerMonster != null) {
                            gameViewModel.updateHpInBattle(battleState.playerHp)
                        }
                    }

                    if (showForceSwapDialog) {
                        val survivors = remember(gameState.teamHp) { gameViewModel.getSurvivors() }
                        SwitchMonsterDialog(
                            title = "Đồng đội đã gục ngã!",
                            availableMonsters = survivors,
                            onDismiss = { },
                            onMonsterSelected = { monster ->
                                gameViewModel.switchActiveMonsterInBattle(monster)
                                battleViewModel.replaceFaintedMonster(monster, gameViewModel.getSkills(monster.name))
                                showForceSwapDialog = false
                            }
                        )
                    }

                    if (pendingReward != null) {
                        TargetSelectionDialog(
                            team = gameState.team,
                            reward = pendingReward!!,
                            onDismiss = { pendingReward = null },
                            onTargetSelected = { target ->
                                gameViewModel.applyReward(pendingReward!!, target)
                                pendingReward = null
                            }
                        )
                    }

                    when (gameState.currentScreen) {
                        "INTRO" -> IntroScreen(onIntroFinished = { gameViewModel.onIntroFinished() })
                        "GAME_MODE" -> GameModeScreen(onModeSelected = { isDemo -> gameViewModel.selectGameMode(isDemo) })
                        "PICKING" -> PickingScreen(
                            monsters = remember { gameViewModel.getStarters() },
                            onMonsterSelected = { name -> gameViewModel.pickStarter(name) }
                        )

                        "MAP" -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                GauntletMapScreen(
                                    mapLevels = gameState.mapData,
                                    currentNode = gameState.currentNode,
                                    onNodeClicked = { node ->
                                        gameViewModel.onNodeClicked(node)
                                        if (node.type == NodeType.BATTLE || node.type == NodeType.ELITE || node.type == NodeType.BOSS) {
                                            val encounter = EncounterManager.generateEncounter(node.type, dbHelper)
                                            if (encounter != null && gameState.playerMonster != null) {
                                                val (enemy, enemySkills) = encounter
                                                battleViewModel.initBattle(
                                                    player = gameState.playerMonster!!,
                                                    enemy = enemy,
                                                    pSkills = gameState.playerSkills,
                                                    eSkills = enemySkills,
                                                    buffs = gameState.collectedBuffs,
                                                    currentHp = gameState.currentHp,
                                                    isBoss = (node.type == NodeType.BOSS),
                                                    onEnd = { isWin, remainingHp ->
                                                        if (isWin) gameViewModel.onBattleWin(remainingHp, node.type)
                                                        else gameViewModel.onBattleLost()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                                FloatingActionButton(
                                    onClick = { gameViewModel.openTeamManagement() },
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                                    containerColor = Color(0xFF009688)
                                ) {
                                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Team", tint = Color.White)
                                }
                                FloatingActionButton(
                                    onClick = { gameViewModel.openLeaderboard() },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp).size(48.dp),
                                    containerColor = Color(0xFFFFC107)
                                ) {
                                    Icon(imageVector = Icons.Default.Star, contentDescription = "Leaderboard", tint = Color.White)
                                }
                            }
                        }

                        "TEAM_MANAGEMENT" -> {
                            TeamManagementScreen(
                                team = gameState.team,
                                onSwap = { i1, i2 -> gameViewModel.swapTeamMembers(i1, i2) },
                                onBack = { gameViewModel.closeTeamManagement() }
                            )
                        }

                        "MYSTERY" -> MysteryScreen(currentHp = gameState.currentHp, maxHp = gameState.playerMonster!!.hp, onFinished = { newHp -> gameViewModel.onMysteryFinished(newHp) })

                        "BATTLE" -> {
                            val livingTeam = remember(gameState.teamHp) { gameViewModel.getSurvivors() }
                            BattleScreen(
                                viewModel = battleViewModel,
                                team = livingTeam,
                                onSwitchMonster = { newMonster ->
                                    gameViewModel.switchActiveMonsterInBattle(newMonster)
                                    battleViewModel.switchPlayerMonster(newMonster, gameViewModel.getSkills(newMonster.name))
                                }
                            )
                        }

                        "BUFF_SELECT" -> {
                            BuffSelectionScreenModified(
                                option1 = gameState.optionStat,
                                option2 = gameState.optionHeal,
                                option3 = gameState.optionBuff,
                                pickCount = gameState.rewardsToPick,
                                onRewardSelected = { reward ->
                                    if (gameState.team.size > 1) pendingReward = reward
                                    else gameViewModel.applyReward(reward, gameState.team[0])
                                }
                            )
                        }

                        "MAJOR_UPGRADE" -> {
                            val currentTeam = gameState.team
                            Box(modifier = Modifier.fillMaxSize()) {
                                MajorUpgradeScreen(
                                    currentTeam = currentTeam,
                                    availableStarters = remember { gameViewModel.getAvailableRecruits() },
                                    onUpgradeFinished = { newTeam -> gameViewModel.onMajorUpgradeFinished(newTeam) }
                                )
                                Button(
                                    onClick = { gameViewModel.openLeaderboard() },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                                ) {
                                    Text("Xem BXH")
                                }
                            }
                        }

                        "GAME_OVER" -> {
                            SaveScoreDialog(
                                score = gameState.currentScore,
                                gameId = GAME_ID_MONSTER_BATTLER,
                                onDismiss = { gameViewModel.onBattleLost() },
                                onSaved = { gameViewModel.showLeaderboardEndGame() }
                            )
                        }

                        "LEADERBOARD" -> {
                            LeaderboardScreen(
                                gameId = GAME_ID_MONSTER_BATTLER,
                                onBack = { gameViewModel.closeLeaderboard() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchMonsterDialog(title: String, availableMonsters: List<Monster>, onDismiss: () -> Unit, onMonsterSelected: (Monster) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                if (availableMonsters.isEmpty()) Text("Không còn ai...", color = Color.Gray) else {
                    LazyColumn {
                        items(availableMonsters) { monster ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onMonsterSelected(monster) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(monster.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("HP: ${monster.hp}", color = Color.Gray)
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TargetSelectionDialog(team: List<Monster>, reward: Reward, onDismiss: () -> Unit, onTargetSelected: (Monster) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Chọn mục tiêu nhận thưởng:", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                val desc = when(reward) { is Reward.StatUpgrade -> reward.description; is Reward.Heal -> reward.description; is Reward.SkillEffect -> "Nhận hiệu ứng: ${reward.buff.name}" }
                Text(desc, color = Color.Blue, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn {
                    items(team) { monster ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onTargetSelected(monster) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(monster.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("HP: ${monster.hp}", color = Color.Gray)
                        }
                        Divider()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Hủy") }
            }
        }
    }
}