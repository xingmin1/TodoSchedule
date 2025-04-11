package com.example.todoschedule.ui.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 绘制课表背景网格线的 Composable。
 * 使用 Canvas 绘制以获得更好的控制和性能。
 *
 * @param startHour 网格开始的小时。
 * @param endHour 网格结束的小时。
 * @param hourHeight 网格中每小时的高度。
 * @param dayWidth 网格中每天的宽度。
 * @param modifier Modifier。
 */
@Composable
fun WeekGrid(
    startHour: Int, // 通常是 0
    endHour: Int,   // 通常是 24
    hourHeight: Dp,
    dayWidth: Dp,
    modifier: Modifier = Modifier
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val dayWidthPx = with(density) { dayWidth.toPx() }
    val lineWidthPx = with(density) { 0.6.dp.toPx() }
    val totalHours = endHour - startHour
    val totalDays = 7

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height // 使用实际 Canvas 高度

        // --- 绘制水平线 --- //
        (0..totalHours).forEach { hourIndex ->
            val y = hourIndex * hourHeightPx
            // 确保线条在 Canvas 内绘制
            if (y <= canvasHeight) {
                drawLine(
                    color = dividerColor,
                    start = Offset(x = 0f, y = y),
                    end = Offset(x = canvasWidth, y = y),
                    strokeWidth = lineWidthPx
                )
            }
        }

        // --- 绘制垂直线 --- //
        (0..totalDays).forEach { dayIndex ->
            val x = dayIndex * dayWidthPx
            // 确保线条在 Canvas 内绘制
            if (x <= canvasWidth) {
                drawLine(
                    color = dividerColor,
                    start = Offset(x = x, y = 0f),
                    end = Offset(x = x, y = canvasHeight),
                    strokeWidth = lineWidthPx
                )
            }
        }
    }
} 