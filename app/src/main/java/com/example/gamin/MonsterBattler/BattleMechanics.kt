package com.example.gamin.MonsterBattler

import com.example.gamin.MonsterBattler.data.Buff
import com.example.gamin.MonsterBattler.data.Monster
import com.example.gamin.MonsterBattler.data.Skill
import kotlin.math.roundToInt

// Enum trạng thái (Giữ nguyên hoặc đặt ở đây đều được)
enum class StatusEffect {
    NONE, STUN, BURN, BREAK_DEF, WEAKEN
}

object BattleMechanics {

    // 1. TÍNH KHẮC HỆ
    fun getTypeEffectiveness(attackType: String, defenseType: String): Float {
        return when (attackType) {
            "Fire" -> when (defenseType) { "Leaf" -> 1.5f; "Water" -> 0.5f; "Fire" -> 0.5f; else -> 1.0f }
            "Water" -> when (defenseType) { "Fire" -> 1.5f; "Leaf" -> 0.5f; "Water" -> 0.5f; else -> 1.0f }
            "Leaf" -> when (defenseType) { "Water" -> 1.5f; "Fire" -> 0.5f; "Leaf" -> 0.5f; else -> 1.0f }
            else -> 1.0f
        }
    }

    // 2. TÍNH SÁT THƯƠNG
    fun calculateDamage(
        attacker: Monster,
        defender: Monster,
        skill: Skill,
        isAttackerBuffed: Boolean = false,
        isDefenderBuffed: Boolean = false,
        isDefenderBroken: Boolean = false, // Thêm tham số thủng giáp
        isAttackerWeakened: Boolean = false // Thêm tham số bị yếu
    ): Int {
        if (skill.power == 0) return 0

        val typeMod = getTypeEffectiveness(skill.type, defender.type)

        var atk = attacker.atk.toFloat()
        var def = defender.def.toFloat()

        // Xử lý Buff từ skill (Tăng công/thủ 3 lượt)
        if (isAttackerBuffed) atk *= 1.5f
        if (isDefenderBuffed) def *= 1.5f

        // Xử lý Debuff từ hiệu ứng (StatusEffect)
        if (isDefenderBroken) def *= 0.5f // Giảm 1/2 thủ
        if (isAttackerWeakened) atk *= 0.5f // Giảm 1/2 công

        // Công thức: (Atk + Power) - (Def / 2)
        val rawDamage = (atk + skill.power) - (def * 0.5f)

        val finalDamage = (rawDamage * typeMod).roundToInt()
        return finalDamage.coerceAtLeast(10) // Sàn damage là 10
    }

    // 3. TÍNH XÁC SUẤT GÂY HIỆU ỨNG (Logic RNG)
    // Trả về Pair: (Loại hiệu ứng, Số lượt tồn tại)
    fun calculateStatusChance(skill: Skill, activeBuffs: List<Buff>): Pair<StatusEffect, Int> {
        // Tìm buff phù hợp với hệ của skill vừa dùng
        val applicableBuff = activeBuffs.find { it.targetType == "Any" || it.targetType == skill.type }

        if (applicableBuff != null) {
            // Tỷ lệ 10%
            val chance = 0.1
            if (Math.random() < chance) {
                return when(applicableBuff.effectType) {
                    "STUN" -> Pair(StatusEffect.STUN, 1)      // Choáng 1 lượt
                    "BURN" -> Pair(StatusEffect.BURN, 2)      // Bỏng 2 lượt
                    "BREAK_DEF" -> Pair(StatusEffect.BREAK_DEF, 2) // Phá giáp 2 lượt
                    "WEAKEN" -> Pair(StatusEffect.WEAKEN, 2)  // Yếu 2 lượt
                    else -> Pair(StatusEffect.NONE, 0)
                }
            }
        }
        return Pair(StatusEffect.NONE, 0)
    }

    // 4. LOGIC AI (Máy chọn chiêu)
    fun decideEnemyMove(
        enemyMonster: Monster,
        currentHp: Int,
        skills: List<Skill>
    ): Skill? {
        // Lọc ra các skill còn PP
        val availableSkills = skills.filter { it.currentPp > 0 }
        if (availableSkills.isEmpty()) return null

        // Logic AI:
        // Nếu máu dưới 30%, ưu tiên tìm skill hồi máu
        val hpPercentage = currentHp.toFloat() / enemyMonster.hp.toFloat()
        if (hpPercentage < 0.3f) {
            val healSkill = availableSkills.find {
                it.power == 0 && (it.name == "Quang Hợp" || it.name == "Hồi Máu" || it.name == "Mưa Rào")
            }
            if (healSkill != null) return healSkill
        }

        // Mặc định: Chọn ngẫu nhiên (hoặc sau này có thể nâng cấp chọn skill khắc hệ)
        return availableSkills.random()
    }

    // 5. LẤY MÔ TẢ HIỆU ỨNG SKILL (Logic String)
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