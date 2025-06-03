package bean

import java.util.UUID

data class CourseDetailBean(
    var id: UUID,
    var day: Int,
    var room: String?,
    var teacher: String?,
    var startNode: Int,
    var step: Int,
    var startWeek: Int,
    var endWeek: Int,
    var type: Int,
    var tableId: UUID,
    var credit: Float = 0f
)