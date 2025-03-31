package com.example.todoschedule.utils.courseadapter.bean

import bean.CourseBaseBean
import bean.CourseDetailBean

data class ParserResult(
    val baseList: ArrayList<CourseBaseBean>,
    val detailList: ArrayList<CourseDetailBean>
)