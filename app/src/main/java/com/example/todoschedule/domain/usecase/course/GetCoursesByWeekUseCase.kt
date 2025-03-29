package com.example.todoschedule.domain.usecase.course

import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取周课表用例
 */
class GetCoursesByWeekUseCase @Inject constructor(
    private val courseRepository: CourseRepository
) {
    /**
     * 执行用例
     */
    operator fun invoke(tableId: Int, week: Int): Flow<List<Course>> {
        return courseRepository.getCoursesByWeek(tableId, week)
    }
} 