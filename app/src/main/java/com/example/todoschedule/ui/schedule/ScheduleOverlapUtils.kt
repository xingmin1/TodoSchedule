package com.example.todoschedule.ui.schedule

import com.example.todoschedule.domain.model.TimeSlot
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 日程卡片位置信息数据类
 */
data class EventSlotInfo(
    val startMinutes: Int,
    val endMinutes: Int,
    val column: Int = 0,
    val maxColumns: Int = 1
)

/**
 * 判断两个时间段是否重叠
 */
fun isOverlapping(a: EventSlotInfo, b: EventSlotInfo): Boolean {
    val buffer = 1
    return a.startMinutes < (b.endMinutes - buffer) && (a.endMinutes - buffer) > b.startMinutes
}

/**
 * 计算事件位置，处理重叠事件（通用算法，供日/周视图复用）
 */
@OptIn(kotlin.ExperimentalStdlibApi::class)
fun calculateEventPositions(
    events: List<TimeSlot>,
    date: LocalDate
): List<Pair<TimeSlot, EventSlotInfo>> {
    if (events.isEmpty()) return emptyList()
    val minDurationMinutes = 20
    val sortedEvents = events.sortedBy { it.startTime }
    val eventSlots = sortedEvents.map { event ->
        val start = Instant.fromEpochMilliseconds(event.startTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val end = Instant.fromEpochMilliseconds(event.endTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val gridStartHour = 0
        val startMinutes = (start.hour - gridStartHour) * 60 + start.minute
        val originalEndMinutes = (end.hour - gridStartHour) * 60 + end.minute
        val durationMinutes = originalEndMinutes - startMinutes
        val endMinutes = if (durationMinutes < minDurationMinutes) {
            startMinutes + minDurationMinutes
        } else {
            originalEndMinutes
        }
        event to EventSlotInfo(startMinutes, endMinutes)
    }.toMutableList()
    val overlapMatrix = Array(eventSlots.size) { BooleanArray(eventSlots.size) { false } }
    for (i in eventSlots.indices) {
        for (j in i + 1 until eventSlots.size) {
            if (isOverlapping(eventSlots[i].second, eventSlots[j].second)) {
                overlapMatrix[i][j] = true
                overlapMatrix[j][i] = true
            }
        }
    }
    val visited = BooleanArray(eventSlots.size) { false }
    val overlapGroups = mutableListOf<List<Int>>()
    for (i in eventSlots.indices) {
        if (!visited[i]) {
            val group = mutableListOf<Int>()
            val queue = java.util.ArrayDeque<Int>()
            queue.addLast(i)
            visited[i] = true
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                group.add(current)
                for (j in eventSlots.indices) {
                    if (!visited[j] && overlapMatrix[current][j]) {
                        queue.addLast(j)
                        visited[j] = true
                    }
                }
            }
            overlapGroups.add(group)
        }
    }
    val result = eventSlots.toMutableList()
    for (group in overlapGroups) {
        if (group.size == 1) {
            val index = group[0]
            val (event, slotInfo) = result[index]
            result[index] = event to slotInfo.copy(column = 0, maxColumns = 1)
            continue
        }
        val sortedIndices = group.sortedBy { result[it].second.startMinutes }
        val columnEndTimes = mutableListOf<Int>()
        for (eventIndex in sortedIndices) {
            val (event, slotInfo) = result[eventIndex]
            var assignedColumn = -1
            for (col in columnEndTimes.indices) {
                if (slotInfo.startMinutes >= columnEndTimes[col]) {
                    assignedColumn = col
                    break
                }
            }
            if (assignedColumn == -1) {
                assignedColumn = columnEndTimes.size
                columnEndTimes.add(slotInfo.endMinutes)
            } else {
                columnEndTimes[assignedColumn] = slotInfo.endMinutes
            }
            result[eventIndex] = event to slotInfo.copy(
                column = assignedColumn,
                maxColumns = columnEndTimes.size
            )
        }
        val finalMaxColumns = columnEndTimes.size
        for (eventIndex in group) {
            val (event, slotInfo) = result[eventIndex]
            result[eventIndex] = event to slotInfo.copy(maxColumns = finalMaxColumns)
        }
    }
    return result
} 