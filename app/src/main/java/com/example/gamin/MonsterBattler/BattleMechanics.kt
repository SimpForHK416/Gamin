package com.example.gamin.MonsterBattler

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
        isDefenderBuffed: Boolean = false
    ): Int {
        if (skill.power == 0) return 0

        val typeMod = getTypeEffectiveness(skill.type, defender.type)

        // 1. Chỉ số cơ bản
        var atk = attacker.atk.toFloat()
        var def = defender.def.toFloat()

        // 2. Áp dụng Buff (Tăng 50% chỉ số)
        if (isAttackerBuffed) atk *= 1.5f
        if (isDefenderBuffed) def *= 1.5f

        // 3. CÔNG THỨC MỚI: (Atk + Power) - (Def / 2)
        var rawDamage = (atk + skill.power) - (def * 0.5f)

        val finalDamage = (rawDamage * typeMod).roundToInt()
        return finalDamage.coerceAtLeast(10) // Sàn 10
    }

    fun getEffectivenessMessage(attackType: String, defenseType: String): String {
        val mod = getTypeEffectiveness(attackType, defenseType)
        return when {
            mod > 1.0f -> "Hiệu quả! 🔥"
            mod < 1.0f -> "Kháng... 🛡️"
            else -> ""
        }
    }
}