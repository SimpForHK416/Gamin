package com.example.gamin.MonsterBattler.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.gamin.MonsterBattler.MapNode
import com.example.gamin.MonsterBattler.NodeType

// Màu sắc đường nối
val PathColorActive = Color(0xFFFFFFFF)
val PathColorInactive = Color(0xFF555555)

@Composable
fun GauntletMapScreen(
    mapLevels: List<List<MapNode>>, // <-- THAY ĐỔI: Nhận map từ bên ngoài
    currentNode: MapNode?,
    onNodeClicked: (MapNode) -> Unit
) {
    // Scroll state
    val scrollState = rememberScrollState()

    // Tự động cuộn xuống dưới cùng (nơi bắt đầu) khi mở map
    LaunchedEffect(Unit) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        Text(
            text = "BẢN ĐỒ ẢI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .zIndex(2f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 80.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Vẽ map theo thứ tự ngược (Tầng cao ở trên)
            mapLevels.reversed().forEachIndexed { reverseIndex, levelNodes ->

                val realLevelIndex = mapLevels.size - 1 - reverseIndex
                val prevLevelNodes = mapLevels.getOrNull(realLevelIndex - 1)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (prevLevelNodes != null) {
                        ConnectionsCanvas(
                            currentLevelNodes = levelNodes,
                            prevLevelNodes = prevLevelNodes,
                            currentNode = currentNode
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        levelNodes.forEach { node ->
                            // Logic khóa/mở đường đi
                            val isClickable = if (currentNode == null) {
                                node.level == 1
                            } else {
                                (node.level == currentNode.level + 1) &&
                                        (currentNode.connectedIndices.contains(node.indexInRow))
                            }

                            val isCurrentPos = (node.id == currentNode?.id)

                            MapNodeItem(
                                node = node,
                                isClickable = isClickable,
                                isCurrentPos = isCurrentPos,
                                onClick = { onNodeClicked(node) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionsCanvas(
    currentLevelNodes: List<MapNode>,
    prevLevelNodes: List<MapNode>,
    currentNode: MapNode?
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        fun getX(index: Int, total: Int): Float {
            val sectionWidth = canvasWidth / total
            return (sectionWidth * index) + (sectionWidth / 2)
        }

        prevLevelNodes.forEach { prevNode ->
            val startX = getX(prevNode.indexInRow, prevLevelNodes.size)
            val startY = canvasHeight

            prevNode.connectedIndices.forEach { targetIndex ->
                val targetNode = currentLevelNodes.find { it.indexInRow == targetIndex }

                if (targetNode != null) {
                    val endX = getX(targetNode.indexInRow, currentLevelNodes.size)
                    val endY = canvasHeight / 2

                    val isActivePath = (currentNode != null && currentNode.id == prevNode.id)
                    val lineColor = if (isActivePath) PathColorActive else PathColorInactive
                    val strokeWidth = if (isActivePath) 6f else 3f

                    drawLine(
                        color = lineColor,
                        start = Offset(startX, startY + 50f),
                        end = Offset(endX, endY),
                        strokeWidth = strokeWidth,
                        pathEffect = if (!isActivePath) PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) else null
                    )
                }
            }
        }
    }
}

@Composable
fun MapNodeItem(
    node: MapNode,
    isClickable: Boolean,
    isCurrentPos: Boolean,
    onClick: () -> Unit
) {
    val (icon, color) = when (node.type) {
        NodeType.BATTLE -> Icons.Default.PlayArrow to Color(0xFFE53935)
        NodeType.ELITE -> Icons.Default.Warning to Color(0xFFFFB300)
        NodeType.MYSTERY -> Icons.Default.Info to Color(0xFF039BE5)
        NodeType.BOSS -> Icons.Default.Star to Color(0xFF8E24AA)
    }

    val alpha = if (isClickable || isCurrentPos) 1f else 0.3f
    val borderColor = if (isCurrentPos) Color.Green else if (isClickable) Color.White else Color.Transparent
    val borderWidth = if (isCurrentPos) 4.dp else if (isClickable) 2.dp else 0.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(borderWidth, borderColor, CircleShape)
                .clickable(enabled = isClickable) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if(isClickable) 0.3f else 0.1f))
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = alpha),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}