package com.example.todoschedule.ui.study

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 学习数据
 */
data class StudyStat(
    val title: String,  // 日/周标题
    val value: Float,  // 时长
    val type: StatType  // 类型
) {
    val displayValue: String
        get() {
            if (value < 0f) return "时长无效"
            if (value == 0.0f) {
                return "暂无专注"
            }

            val totalSeconds = (value * 3600).toInt()
            if (totalSeconds == 0) {
                return "不足1秒"
            }

            if (totalSeconds < 60) {
                return "${totalSeconds}秒"
            }

            val totalMinutes = totalSeconds / 60
            val hours = totalMinutes / 60
            val minutesInHour = totalMinutes % 60

            return when {
                hours > 0 -> {
                    if (minutesInHour > 0) {
                        "${hours}小时${minutesInHour}分钟"
                    } else {
                        "${hours}小时"
                    }
                }

                else -> {
                    "${totalMinutes}分钟"
                }
            }
        }
}

/**
 * 数据类型
 */
enum class StatType {
    DAILY,  // 日
    WEEKLY  // 周
}

// 时间格式化
fun LocalDateTime.format(formatter: DateTimeFormatter): String {
    return this.toJavaLocalDateTime().format(formatter)
}

/**
 * 学习计划
 */
data class StudyPlan(
    val id: Int,  // ID
    val title: String,  // 标题
    val startTime: LocalDateTime, // 开始时间
    val endTime: LocalDateTime,  // 结束时间
    val description: String? = null,  // 描述
    val subject: String,  // 科目
    val location: String? = null,  // 地点
) {
    val timeDisplay: String  // 格式化显示时间
        get() {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val todayDate = now.date
            val tomorrowDate = todayDate.plus(DatePeriod(days = 1))

            val deadlineDate = endTime.date
            val deadlineTime = endTime.time

            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")

            return when (deadlineDate) {
                todayDate -> {
                    "今日${deadlineTime.toJavaLocalTime().format(timeFormatter)}"
                }

                tomorrowDate -> {
                    "明日${deadlineTime.toJavaLocalTime().format(timeFormatter)}"
                }

                else -> {
                    "${deadlineDate.toJavaLocalDate().format(dateFormatter)}"
                }
            }
        }
}

/**
 * 计时器控制
 */
enum class TimerControlAction {
    START_INITIAL,  // 初始
    PAUSE,  // 暂停
    RESUME,  // 重新开始
    RESTART_COMPLETED,  // 重新开始完成
    RESET  // 重置
}

/**
 * 番茄钟状态
 */
data class TimerState(
    // 时间配置参数
    val focusDurationMinutes: Int = 25,  // 专注时长（分钟）
    val shortBreakMinutes: Int = 5,  // 短休息时长（分钟）
    val longBreakMinutes: Int = 15,  // 长休息时长（分钟）
    val longBreakInterval: Int = 4,  // 长休息间隔（轮次）
    val autoStartNextRound: Boolean = false,  // 是否自动开始下一轮

    // 运行时状态
    val currentRound: Int = 1,  // 当前轮次
    val isFocusPhase: Boolean = true,  // 当前是否专注阶段
    val isLongBreak: Boolean = false,  // 当前是否长休息

    // 计时器状态
    val totalTimeSeconds: Int = if (isFocusPhase) focusDurationMinutes * 60
    else if (isLongBreak) longBreakMinutes * 60
    else shortBreakMinutes * 60,  // 总计时时长
    val isInitial: Boolean = true,  // 初始状态
    val isRunning: Boolean = false,  // 计时状态
    val isPaused: Boolean = false,  // 暂停状态
    val remainingTime: Int = totalTimeSeconds,  // 计时剩余时间
    val selectedTask: String? = null,  // 选择的任务

    // 统计数据
    val todayFocusMinutes: Int = 0,  // 今日累计专注分钟数
    val currentSessionStartTime: LocalDateTime? = null,  // 当前专注阶段的开始时间（用于计算实际时长）
    val currentSequenceId: String? = null
)

/**
 * 专注历史记录
 */
data class FocusSessionRecord(
    val startTime: LocalDateTime,  // 开始时间
    val endTime: LocalDateTime,  // 结束时间
    val durationSeconds: Int,  // 实际的专注秒数
    val taskName: String?, // 专注的任务名称，可为空
    val sessionStatus: SessionStatus,  // 区分完成/中断类型
    val roundId: String,  // 关联同一轮次的专注和休息
    val isFocusPhase: Boolean  // 标记是否为专注阶段（避免统计休息时间）
) {
    enum class SessionStatus {
        COMPLETED,  // 专注时间自然完成
        INTERRUPTED,  // 用户手动停止
        RESET  // 被重置（算作新一轮）
    }

    val timeRangeDisplay: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            return "${startTime.format(formatter)}-${endTime.format(formatter)}"
        }

    val durationDisplay: String
        get() {
            if (durationSeconds < 0) return "时长无效"
            if (durationSeconds < 60) {
                return "${durationSeconds}秒"
            }
            val totalMinutes = durationSeconds / 60
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) {
                "${hours}小时${minutes}分钟"
            } else {
                "${totalMinutes}分钟"
            }
        }
}