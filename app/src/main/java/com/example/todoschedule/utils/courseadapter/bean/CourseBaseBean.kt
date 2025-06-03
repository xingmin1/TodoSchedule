package bean

import com.example.todoschedule.ui.theme.ColorSchemeEnum
import java.util.UUID

data class CourseBaseBean(
    var id: UUID,
    var courseName: String,
    var color: ColorSchemeEnum,
    var tableId: UUID,
    var note: String,
    var credit: Float = 0f,
    var courseID: String = ""
)