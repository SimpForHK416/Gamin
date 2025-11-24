package com.example.gamin.MonsterBattler

import java.util.UUID

enum class NodeType {
    BATTLE, ELITE, MYSTERY, BOSS
}

data class MapNode(
    val id: String = UUID.randomUUID().toString(),
    val type: NodeType,
    val level: Int,
    val indexInRow: Int, // Vị trí của node trong hàng (0, 1, 2...)
    // Danh sách các index của tầng tiếp theo mà node này nối tới
    val connectedIndices: List<Int> = emptyList()
)

object MapGenerator {

    fun generateMap(): List<List<MapNode>> {
        return listOf(
            // Tầng 1 (Start): 1 node nối với 2 node ở tầng 2 (index 0 và 1)
            listOf(
                MapNode(type = NodeType.BATTLE, level = 1, indexInRow = 0, connectedIndices = listOf(0, 1))
            ),

            // Tầng 2: 2 node.
            // Node 0 nối với (0, 1) tầng 3. Node 1 nối với (1, 2) tầng 3.
            listOf(
                MapNode(type = NodeType.BATTLE, level = 2, indexInRow = 0, connectedIndices = listOf(0, 1)),
                MapNode(type = NodeType.MYSTERY, level = 2, indexInRow = 1, connectedIndices = listOf(1, 2))
            ),

            // Tầng 3: 3 node.
            listOf(
                MapNode(type = NodeType.ELITE, level = 3, indexInRow = 0, connectedIndices = listOf(0)),
                MapNode(type = NodeType.BATTLE, level = 3, indexInRow = 1, connectedIndices = listOf(0)),
                MapNode(type = NodeType.BATTLE, level = 3, indexInRow = 2, connectedIndices = listOf(0))
            ),

            // Tầng 4: 1 node (Hồi phục/Sự kiện)
            listOf(
                MapNode(type = NodeType.MYSTERY, level = 4, indexInRow = 0, connectedIndices = listOf(0))
            ),

            // Tầng 5: BOSS
            listOf(
                MapNode(type = NodeType.BOSS, level = 5, indexInRow = 0)
            )
        )
    }
}