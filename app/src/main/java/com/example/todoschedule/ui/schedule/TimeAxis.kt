package com.example.todoschedule.ui.schedule

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 显示左侧时间轴的 Composable。
 * 包含从 `startHour` 到 `endHour` 的时间标签。
 * 垂直偏移由外部通过 Modifier 控制以实现同步滚动。
 *
 * @param startHour 时间轴开始的小时 (包含)。
 * @param endHour 时间轴结束的小时 (不包含)。
 * @param hourHeight 每个小时标签占据的高度。
 * @param scrollState 共享的滚动状态
 * @param modifier Modifier，通常包含垂直偏移量。
 */
@Composable
fun TimeAxis(
    startHour: Int,
    endHour: Int,
    hourHeight: Dp,
    scrollState: ScrollState,
    modifier: Modifier = Modifier // 外部可以设置宽度等
) {
    Column(
        modifier = modifier
            .padding(end = 4.dp)
            .verticalScroll(scrollState) // 应用垂直滚动
    ) {
        // 绘制从 startHour 到 endHour - 1 的时间标签
        (startHour until endHour).forEach { hour ->
            TimeAxisLabel(hour = hour, hourHeight = hourHeight)
        }
        // 可选：添加一个小的占位符或最后一条线，使 24:00 对齐更好？
        // Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.LightGray)) // 示例：底部线
    }
}

/**
 * 单个时间轴标签。
 */
@Composable
private fun TimeAxisLabel(
    hour: Int,
    hourHeight: Dp
) {
    Box(
        contentAlignment = Alignment.TopCenter, // 标签内容顶部居中
        modifier = Modifier
            .height(hourHeight) // 每个标签高度固定
            .fillMaxWidth()
            .padding(top = 2.dp) // 向上微调以对齐网格线
    ) {
        Text(
            text = String.format("%02d:00", hour),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), // 调整字体
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}