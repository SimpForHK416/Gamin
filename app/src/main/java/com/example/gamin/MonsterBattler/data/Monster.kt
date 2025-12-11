package com.example.gamin.MonsterBattler.data

data class Monster(
    val name: String,
    val hp: Int,
    val atk: Int,
    val def: Int,
    val speed: Int,
    val type: String,
    val ability: String,
    val description: String
)