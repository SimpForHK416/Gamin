package com.example.gamin.MonsterBattler.data

data class Skill(
    val name: String,
    val type: String,
    val power: Int,      // Sát thương (10-20)
    val maxPp: Int,      // Mặc định là 10
    var currentPp: Int,  // <-- QUAN TRỌNG: Phải là var để trừ PP
    val description: String
)