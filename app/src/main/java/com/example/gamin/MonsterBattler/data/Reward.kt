package com.example.gamin.MonsterBattler.data

sealed class Reward {
    data class StatUpgrade(val statName: String, val value: Int, val description: String) : Reward()
    data class Heal(val amountPercent: Int, val description: String) : Reward()
    data class SkillEffect(val buff: Buff) : Reward()
}