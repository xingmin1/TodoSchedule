package bean

data class CourseDetailBean(
    var Id: UUID,
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