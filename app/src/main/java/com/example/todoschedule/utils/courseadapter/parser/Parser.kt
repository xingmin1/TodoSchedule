package parser

import Common
import android.util.Log
import bean.Course
import bean.CourseBaseBean
import bean.CourseDetailBean
import bean.TimeTable
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import com.example.todoschedule.utils.courseadapter.bean.ParserResult

abstract class Parser(val source: String) {

    private val _baseList: ArrayList<CourseBaseBean> = arrayListOf()
    private val _detailList: ArrayList<CourseDetailBean> = arrayListOf()

    abstract fun generateCourseList(): List<Course>

    // TimeTable中的name属性将起到标识作用，如果在数据库中发现同名时间表，则不再覆盖写入
    open fun generateTimeTable(): TimeTable? = null

    open fun getTableName(): String? = null

    open fun getNodes(): Int? = null

    open fun getStartDate(): String? = null

    open fun getMaxWeek(): Int? = null

    private fun convertCourse() {
        generateCourseList().forEach { course ->
            var id = Common.findExistedCourseId(_baseList, course.name)
            if (id == -1) {
                id = _baseList.size
                _baseList.add(
                    CourseBaseBean(
                        id = id, courseName = course.name,
                        color = AppConstants.DEFAULT_COURSE_COLOR,
                        tableId = 0,
                        note = course.note,
                        credit = course.credit,
                        courseID = course.courseID
                    )
                )
            }
            _detailList.add(
                CourseDetailBean(
                    id = id, room = course.room,
                    teacher = course.teacher, day = course.day,
                    step = course.endNode - course.startNode + 1,
                    startWeek = course.startWeek, endWeek = course.endWeek,
                    type = course.type, startNode = course.startNode,
                    credit = course.credit,
                    tableId = 0
                )
            )
        }
    }

    /**
     * 为课程生成柔和的浅色调 ARGB 颜色（格式如 #FFxxxxxx），确保深色文字可读。
     * @param index 课程索引，用于生成不同颜色。
     * @return 返回 ARGB 格式的颜色字符串，例如 "#FFB3E5FC"。
     */
    fun generateCourseColor(index: Int): ColorSchemeEnum {
        val colorList = ColorSchemeEnum.getColorList()
        return colorList[index % colorList.size]
//
//        // --- 参数调整 ---
//        // 目标明度 (Value): 调高以获得浅色 (0.0 - 1.0)
//        val targetValue = 0.95f // 非常接近白色，产生很浅的颜色
//        // 目标饱和度 (Saturation): 适度降低以获得柔和色彩 (0.0 - 1.0)
//        val targetSaturation = 0.6f // 降低饱和度，避免过于刺眼，更像粉彩色
//        // 灰色明度 (Gray Value): 确保灰色也足够浅
//        val grayValue = 0.95f // 非常浅的灰色
//
//        // --- 颜色生成逻辑 ---
//        val isGray = index % 7 == 0 // 可以调整产生灰色的频率，例如改为 7 或 10
//
//        val hsv = FloatArray(3).apply {
//            if (isGray) {
//                // 生成浅灰色
//                this[0] = 0f // 色相无关紧要
//                this[1] = 0f // 饱和度为 0 才是灰色
//                this[2] = grayValue // 使用设定的浅灰色明度
//            } else {
//                // 生成彩色
//                // 使用黄金分割角来尽可能分散颜色
//                val goldenAngle = 137.5
//                this[0] = (index * goldenAngle % 360).toFloat() // 色相 (0-360)
//                this[1] = targetSaturation // 使用设定的饱和度
//                this[2] = targetValue // 使用设定的明度
//            }
//        }
//
//        // 将 HSV 转换为 ARGB 整数
//        val colorInt = Color.HSVToColor(hsv) // 注意：默认 Alpha 是 FF (不透明)
//
//        // 格式化为 #AARRGGBB 字符串 (确保 Alpha 是 FF)
//        // Color.HSVToColor 返回的是带 FF Alpha 的 ARGB 整数
//        // toUInt().toString(16) 可以正确处理负数整数表示的颜色
//        // padStart(8, '0') 确保总是 8 位十六进制数 (包含 Alpha)
//        return "#${colorInt.toUInt().toString(16).padStart(8, '0').uppercase()}"
    }

    fun saveCourse(): ParserResult {
        convertCourse()
        for (i in 0 until _baseList.size) {
            _baseList[i].color = generateCourseColor(i)
            Log.d(
                "Parser",
                "课程名称: ${_baseList[i].courseName}, 颜色: ${_baseList[i].color}, 索引: $i"
            )
        }
        return ParserResult(baseList = _baseList, detailList = _detailList)
    }

}
