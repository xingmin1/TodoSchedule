package bean

import com.example.todoschedule.ui.theme.ColorSchemeEnum

data class CourseBaseBean(
    var id: Int,
    var courseName: String,
    var color: ColorSchemeEnum,
    var tableId: Int,
    var note: String,
    var credit: Float = 0f,
    var courseID: String = ""
)