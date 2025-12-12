package com.example.gamin.MonsterBattler

import java.util.UUID
import kotlin.random.Random

enum class NodeType {
    BATTLE, ELITE, MYSTERY, BOSS
}

data class MapNode(
    val id: String = UUID.randomUUID().toString(),
    val type: NodeType,
    val level: Int,
    val indexInRow: Int,
    var connectedIndices: List<Int> = emptyList()
)

object GauntletMapGenerator {

    fun generateRandomMap(): List<List<MapNode>> {
        val map = mutableListOf<List<MapNode>>()
        val random = Random.Default

        val level1 = listOf(MapNode(type = NodeType.BATTLE, level = 1, indexInRow = 0))
        map.add(level1)

        for (level in 2..4) {
            val nodeCount = random.nextInt(2, 4)
            val nodes = mutableListOf<MapNode>()

            for (i in 0 until nodeCount) {
                val roll = random.nextFloat()
                val type = when {
                    roll < 0.5 -> NodeType.BATTLE
                    roll < 0.8 -> NodeType.MYSTERY
                    else -> NodeType.ELITE
                }
                nodes.add(MapNode(type = type, level = level, indexInRow = i))
            }
            map.add(nodes)
        }

        val level5 = listOf(MapNode(type = NodeType.BOSS, level = 5, indexInRow = 0))
        map.add(level5)

        for (i in 0 until map.size - 1) {
            val currentLevel = map[i]
            val nextLevel = map[i+1]

            currentLevel.forEach { currentNode ->
                val connections = mutableListOf<Int>()

                val leftTarget = (currentNode.indexInRow).coerceAtMost(nextLevel.lastIndex)
                val rightTarget = (currentNode.indexInRow + 1).coerceAtMost(nextLevel.lastIndex)

                connections.add(leftTarget)
                if (leftTarget != rightTarget) {
                    connections.add(rightTarget)
                }

                currentNode.connectedIndices = connections
            }

            nextLevel.forEach { targetNode ->
                val isConnected = currentLevel.any { it.connectedIndices.contains(targetNode.indexInRow) }

                if (!isConnected) {
                    val parentIndexToForce = targetNode.indexInRow.coerceAtMost(currentLevel.lastIndex)

                    val parentNode = currentLevel[parentIndexToForce]

                    val newConnections = parentNode.connectedIndices.toMutableList()
                    newConnections.add(targetNode.indexInRow)

                    parentNode.connectedIndices = newConnections.distinct().sorted()
                }
            }
        }

        map[3].forEach { it.connectedIndices = listOf(0) }

        return map
    }
}