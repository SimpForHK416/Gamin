package com.example.gamin.MonsterBattler.data

data class Buff(
    val name: String,
    val description: String,
    val targetType: String, // "Any" (cho Stun), "Fire", "Water", "Leaf"
    val effectType: String  // "STUN", "BURN", "BREAK_DEF", "WEAKEN"
)