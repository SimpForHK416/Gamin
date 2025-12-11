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

        // TẦNG 1: LUÔN LÀ 1 Ô BATTLE (START)
        val level1 = listOf(MapNode(type = NodeType.BATTLE, level = 1, indexInRow = 0))
        map.add(level1)

        // TẦNG 2, 3, 4: RANDOM SỐ LƯỢNG VÀ LOẠI NODE
        for (level in 2..4) {
            val nodeCount = random.nextInt(2, 4) // Random từ 2 đến 3 node mỗi tầng
            val nodes = mutableListOf<MapNode>()

            for (i in 0 until nodeCount) {
                // Tỷ lệ: 50% Battle, 30% Mystery (Xanh), 20% Elite (Vàng)
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

        // TẦNG 5: LUÔN LÀ BOSS
        val level5 = listOf(MapNode(type = NodeType.BOSS, level = 5, indexInRow = 0))
        map.add(level5)

        // --- TẠO ĐƯỜNG NỐI (ĐÃ SỬA LOGIC CHỐNG MỒ CÔI) ---
        for (i in 0 until map.size - 1) {
            val currentLevel = map[i]      // Tầng dưới
            val nextLevel = map[i+1]       // Tầng trên

            // BƯỚC 1: Nối từ dưới lên (Logic cũ)
            // Mỗi node dưới nối với các node gần nó ở trên
            currentLevel.forEach { currentNode ->
                val connections = mutableListOf<Int>()

                val leftTarget = (currentNode.indexInRow).coerceAtMost(nextLevel.lastIndex)
                val rightTarget = (currentNode.indexInRow + 1).coerceAtMost(nextLevel.lastIndex)

                connections.add(leftTarget)
                if (leftTarget != rightTarget) {
                    connections.add(rightTarget)
                }

                // Lưu tạm
                currentNode.connectedIndices = connections
            }

            // BƯỚC 2: Kiểm tra ngược (FIX LỖI MỒ CÔI)
            // Duyệt qua tất cả node ở tầng trên, xem có node nào chưa được nối không
            nextLevel.forEach { targetNode ->
                // Kiểm tra xem có node nào ở tầng dưới nối tới targetNode này không
                val isConnected = currentLevel.any { it.connectedIndices.contains(targetNode.indexInRow) }

                if (!isConnected) {
                    // NẾU KHÔNG CÓ AI NỐI TỚI -> BẮT BUỘC NỐI
                    // Tìm node ở tầng dưới có vị trí (index) gần node này nhất
                    // indexInRow của node dưới tối đa chỉ bằng size-1 của tầng dưới
                    val parentIndexToForce = targetNode.indexInRow.coerceAtMost(currentLevel.lastIndex)

                    val parentNode = currentLevel[parentIndexToForce]

                    // Thêm kết nối mới vào danh sách kết nối của node cha
                    val newConnections = parentNode.connectedIndices.toMutableList()
                    newConnections.add(targetNode.indexInRow)

                    // Cập nhật lại và sắp xếp cho đẹp
                    parentNode.connectedIndices = newConnections.distinct().sorted()
                }
            }
        }

        // Fix cứng: Tất cả node tầng 4 phải nối về Boss (index 0 tầng 5)
        // Dù logic trên đã bao phủ, nhưng dòng này đảm bảo chắc chắn 100% không lỗi boss
        map[3].forEach { it.connectedIndices = listOf(0) }

        return map
    }
}