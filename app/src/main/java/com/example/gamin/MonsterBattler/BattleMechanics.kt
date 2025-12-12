package com.example.gamin.MonsterBattler

import com.example.gamin.MonsterBattler.data.Buff
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.Skill
import kotlin.math.roundToInt

enum class StatusEffect {
    NONE, STUN, BURN, BREAK_DEF, WEAKEN
}

object BattleMechanics {

    fun getTypeEffectiveness(attackType: String, defenseType: String): Float {
        return when (attackType) {
            "Fire" -> when (defenseType) { "Leaf" -> 1.5f; "Water" -> 0.5f; "Fire" -> 0.5f; else -> 1.0f }
            "Water" -> when (defenseType) { "Fire" -> 1.5f; "Leaf" -> 0.5f; "Water" -> 0.5f; else -> 1.0f }
            "Leaf" -> when (defenseType) { "Water" -> 1.5f; "Fire" -> 0.5f; "Leaf" -> 0.5f; else -> 1.0f }
            else -> 1.0f
        }
    }

    fun calculateDamage(
        attacker: Monster,
        defender: Monster,
        skill: Skill,
        isAttackerBuffed: Boolean = false,
        isDefenderBuffed: Boolean = false,
        isDefenderBroken: Boolean = false,
        isAttackerWeakened: Boolean = false
    ): Int {
        if (skill.power == 0) return 0

        val typeMod = getTypeEffectiveness(skill.type, defender.type)

        var atk = attacker.atk.toFloat()
        var def = defender.def.toFloat()

        if (isAttackerBuffed) atk *= 1.5f
        if (isDefenderBuffed) def *= 1.5f

        if (isDefenderBroken) def *= 0.5f
        if (isAttackerWeakened) atk *= 0.5f

        val rawDamage = (atk + skill.power) - (def * 0.5f)

        val finalDamage = (rawDamage * typeMod).roundToInt()
        return finalDamage.coerceAtLeast(10)
    }

    fun calculateStatusChance(skill: Skill, activeBuffs: List<Buff>): Pair<StatusEffect, Int> {
        val applicableBuff = activeBuffs.find { it.targetType == "Any" || it.targetType == skill.type }

        if (applicableBuff != null) {
            val chance = 0.1
            if (Math.random() < chance) {
                return when(applicableBuff.effectType) {
                    "STUN" -> Pair(StatusEffect.STUN, 1)
                    "BURN" -> Pair(StatusEffect.BURN, 2)
                    "BREAK_DEF" -> Pair(StatusEffect.BREAK_DEF, 2)
                    "WEAKEN" -> Pair(StatusEffect.WEAKEN, 2)
                    else -> Pair(StatusEffect.NONE, 0)
                }
            }
        }
        return Pair(StatusEffect.NONE, 0)
    }

    fun decideEnemyMove(
        enemyMonster: Monster,
        currentHp: Int,
        skills: List<Skill>
    ): Skill? {
        val availableSkills = skills.filter { it.currentPp > 0 }
        if (availableSkills.isEmpty()) return null

        val hpPercentage = currentHp.toFloat() / enemyMonster.hp.toFloat()
        if (hpPercentage < 0.3f) {
            val healSkill = availableSkills.find {
                it.power == 0 && (it.name == "Quang Hợp" || it.name == "Hồi Máu" || it.name == "Mưa Rào")
            }
            if (healSkill != null) return healSkill
        }

        return availableSkills.random()
    }

    fun getSkillEffectDescription(skillName: String): String {
        return when(skillName) {
            "Nóng Giận", "Phấn Hoa", "Thiền Định", "Gầm Gừ", "Buff Công" -> "Đã TĂNG Tấn Công!"
            "Vỏ Cứng", "Thu Mình", "Giáp Gai", "Tích Tụ", "Giáp" -> "Đã TĂNG Phòng Thủ!"
            "Vũ Điệu", "Vũ Điệu Mưa" -> "Đã TĂNG Tốc Độ!"
            "Quang Hợp", "Mưa Rào", "Hồi Máu" -> "Đã Hồi Phục Máu!"
            else -> "Chỉ số bản thân đã tăng!"
        }
    }
}