package com.example.todoschedule.ui.course.load

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import net.sourceforge.pinyin4j.PinyinHelper

// 增强版 School 数据类
data class School(
    val id: String,
    val name: String,
    val url: String,
    var pinyinInitial: Char = '#' // 新增拼音首字母字段
) {
    // 在构造时自动生成拼音首字母
    init {
        pinyinInitial = getPinyinInitial(name)
    }

    private fun getPinyinInitial(name: String): Char {
        return try {
            name.firstOrNull()?.let { char ->
                PinyinHelper.toHanyuPinyinStringArray(char)?.firstOrNull()?.first()
            }?.uppercaseChar() ?: '#'
        } catch (e: Exception) {
            '#'
        }
    }
}

class SchoolViewModel : ViewModel() {
    // 私有数据源
    private val _rawSchools = listOf(
        School("1", "清华大学", "https://www.tsinghua.edu.cn"),
        School("2", "北京大学", "https://www.pku.edu.cn"),
        School("3", "郑州大学", "https://jw.v.zzu.edu.cn/eams/login.action"),
        // 添加更多学校...
    )

    // 搜索查询的 Flow
    private val _searchQuery = MutableStateFlow("")

    // 对外暴露的分组学校数据
    val groupedSchools: StateFlow<Map<Char, List<School>>> = _searchQuery
        .combine(flow { emit(_rawSchools) }) { query, schools ->
            filterAndGroupSchools(query, schools)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // 更新搜索查询
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 核心处理逻辑：过滤+分组+排序
    private fun filterAndGroupSchools(
        query: String,
        schools: List<School>
    ): Map<Char, List<School>> {
        return schools
            .filter { school ->
                // 前缀匹配（支持拼音首字母）
                school.name.startsWith(query, ignoreCase = true) ||
                        school.pinyinInitial.uppercaseChar() == query.uppercase().firstOrNull()
            }
            .sortedBy { it.pinyinInitial } // 按拼音首字母排序
            .groupBy { it.pinyinInitial.uppercaseChar() }
    }

    // 获取所有首字母（用于侧边导航）
    fun getAllInitials(): List<Char> {
        return _rawSchools
            .map { it.pinyinInitial.uppercaseChar() }
            .distinct()
            .sorted()
    }
}