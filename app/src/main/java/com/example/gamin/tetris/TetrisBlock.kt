package com.example.gamin.tetris

import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

// Class thường (không phải data class) để tự quản lý copy và equals
class TetrisBlock(
    var shape: Array<IntArray>,
    var color: Int,
    var x: Int = 3,
    var y: Int = 0
) {

    fun rotate() {
        val n = shape.size
        val m = shape[0].size
        val rotated = Array(m) { IntArray(n) }
        for (i in shape.indices) {
            for (j in shape[0].indices) {
                rotated[j][n - i - 1] = shape[i][j]
            }
        }
        shape = rotated
    }

    fun getPaint(): Paint {
        val paint = Paint()
        paint.color = color
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        return paint
    }

    companion object {
        private val colors = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.CYAN,
            Color.MAGENTA,
            Color.YELLOW
        )

        private val shapes = listOf(
            arrayOf( // I
                intArrayOf(1, 1, 1, 1)
            ),
            arrayOf( // O
                intArrayOf(1, 1),
                intArrayOf(1, 1)
            ),
            arrayOf( // T
                intArrayOf(0, 1, 0),
                intArrayOf(1, 1, 1)
            ),
            arrayOf( // L
                intArrayOf(1, 0),
                intArrayOf(1, 0),
                intArrayOf(1, 1)
            ),
            arrayOf( // J
                intArrayOf(0, 1),
                intArrayOf(0, 1),
                intArrayOf(1, 1)
            ),
            arrayOf( // S
                intArrayOf(0, 1, 1),
                intArrayOf(1, 1, 0)
            ),
            arrayOf( // Z
                intArrayOf(1, 1, 0),
                intArrayOf(0, 1, 1)
            )
        )

        fun randomBlock(): TetrisBlock {
            val index = Random.nextInt(shapes.size)
            // Clone mảng để tránh tham chiếu tĩnh
            val shape = shapes[index].map { it.clone() }.toTypedArray()
            val color = colors[Random.nextInt(colors.size)]
            return TetrisBlock(shape, color)
        }
    }

    // Hàm copy thủ công (Deep copy cho shape)
    fun copy(
        shape: Array<IntArray> = this.shape.map { it.clone() }.toTypedArray(),
        color: Int = this.color,
        x: Int = this.x,
        y: Int = this.y
    ): TetrisBlock {
        return TetrisBlock(shape, color, x, y)
    }

    // Override equals & hashCode để so sánh nội dung mảng
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TetrisBlock) return false
        if (color != other.color) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (!shape.contentDeepEquals(other.shape)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = color
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + shape.contentDeepHashCode()
        return result
    }
}