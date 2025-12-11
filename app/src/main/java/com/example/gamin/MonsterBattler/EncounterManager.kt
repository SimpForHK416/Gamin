package com.example.gamin.MonsterBattler

import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.MonsterDbHelper
import com.example.gamin.MonsterBattler.data.Skill

object EncounterManager {

    // Hàm trả về cặp: Quái vật (đã buff) và List Skill của nó
    fun generateEncounter(
        nodeType: NodeType,
        dbHelper: MonsterDbHelper
    ): Pair<Monster, List<Skill>>? {

        // 1. Lấy danh sách quái có thể gặp (Trừ Starter và Evo của Starter)
        val allMonsters = dbHelper.getAllMonsters()
        val bannedNames = listOf("CRISHY", "CONFLEVOUR", "RHINPLINK", "RHITAIN", "DOREWEE", "DOPERAMI")
        val wildEnemies = allMonsters.filter { !bannedNames.contains(it.name) }

        if (wildEnemies.isEmpty()) return null

        // 2. Random một con
        var enemy = wildEnemies.random()

        // 3. Xử lý Buff chỉ số theo loại Node
        // Logic: Elite tăng 20%, Boss tăng 30%. KHÔNG ĐỔI TÊN để giữ ảnh.
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
            else -> { /* Battle thường giữ nguyên */ }
        }

        // 4. Lấy Skill (Dùng tên gốc trong DB vì tên quái không đổi)
        val skills = dbHelper.getSkillsForMonster(enemy.name)

        return Pair(enemy, skills)
    }
}