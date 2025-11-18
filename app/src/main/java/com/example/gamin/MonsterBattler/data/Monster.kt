package com.example.gamin.MonsterBattler.data

// Một data class đơn giản để chứa dữ liệu, không cần @Entity của Room
data class Monster(
    val name: String,
    val hp: Int,
    val atk: Int,
    val def: Int,
    val speed: Int,
    val type: String,         // Hệ
    val ability: String,      // Nội tại
    val description: String   // Mô tả
)