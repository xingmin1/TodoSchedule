package com.example.todoschedule.domain.use_case.course

import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.repository.CourseRepository
import java.util.UUID
import javax.inject.Inject

/**
 * 添加课程用例
 */
class AddCourseUseCase @Inject constructor(
    private val courseRepository: CourseRepository
) {
    /**
     * 添加课程
     * @param course 课程信息
     * @param tableId 课表ID
     * @return 课程ID
     */
    suspend operator fun invoke(course: Course, tableId: UUID): UUID {
        return courseRepository.addCourse(course, tableId)
    }
} 