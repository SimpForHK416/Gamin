package com.example.gamin.MonsterBattler.data

data class Skill(
    val name: String,
    val type: String,
    val power: Int,
    val maxPp: Int,
    var currentPp: Int,
    val description: String
)