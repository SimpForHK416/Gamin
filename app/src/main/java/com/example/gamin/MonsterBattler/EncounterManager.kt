package com.example.gamin.MonsterBattler

import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.MonsterDbHelper
import com.example.gamin.MonsterBattler.data.Skill

object EncounterManager {

    fun generateEncounter(
        nodeType: NodeType,
        dbHelper: MonsterDbHelper
    ): Pair<Monster, List<Skill>>? {

        val allMonsters = dbHelper.getAllMonsters()
        val bannedNames = listOf("CRISHY", "CONFLEVOUR", "RHINPLINK", "RHITAIN", "DOREWEE", "DOPERAMI")
        val wildEnemies = allMonsters.filter { !bannedNames.contains(it.name) }

        if (wildEnemies.isEmpty()) return null

        var enemy = wildEnemies.random()

        when (nodeType) {
            NodeType.ELITE -> {
                enemy = enemy.copy(
                    hp = (enemy.hp * 1.2).toInt(),
                    atk = (enemy.atk * 1.2).toInt(),
                    def = (enemy.def * 1.2).toInt(),
                    speed = (enemy.speed * 1.2).toInt()
                )
            }
            NodeType.BOSS -> {
                enemy = enemy.copy(
                    hp = (enemy.hp * 1.3).toInt(),
                    atk = (enemy.atk * 1.3).toInt(),
                    def = (enemy.def * 1.3).toInt(),
                    speed = (enemy.speed * 1.3).toInt()
                )
            }
            else -> {

            }
        }

        val skills = dbHelper.getSkillsForMonster(enemy.name)

        return Pair(enemy, skills)
    }
}