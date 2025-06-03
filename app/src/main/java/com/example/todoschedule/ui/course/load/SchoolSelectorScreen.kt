package com.example.todoschedule.ui.course.load

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoschedule.ui.navigation.NavigationState
import net.sourceforge.pinyin4j.PinyinHelper
import java.net.URLEncoder
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SchoolSelectorScreen(
    tableId: UUID,
    navigationState: NavigationState
) {
    val viewModel: SchoolViewModel = viewModel()
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // 观察分组数据变化
    val groupedSchools by viewModel.groupedSchools.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 优化搜索栏
        SearchBar(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
                viewModel.updateSearchQuery(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // 学校列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            groupedSchools.forEach { (initial, schools) ->
                stickyHeader {
                    SectionHeader(initial = if (initial == '#') "其他" else initial.toString())
                }
                items(schools) { school ->
                    SchoolListItemV2(
                        school = school,
                        onClick = {
                            try {
                                if (school.name == "郑州大学") {
                                    val encodedUrl = URLEncoder.encode(school.url, "UTF-8")
                                    navigationState.navigateWebViewScreen(encodedUrl, tableId)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "该学校暂未适配，敬请期待！",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e("Navigation", "URL编码失败", e)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        placeholder = {
            Text("输入学校名称...")
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier,
        singleLine = true
    )
}

// 新版分组标题
@Composable
private fun SectionHeader(initial: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(end = 8.dp)
            )
            Divider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
        }
    }
}

// 优化后的列表项
@Composable
private fun SchoolListItemV2(school: School, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 首字母标签
            Text(
                text = school.pinyinInitial.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.width(24.dp) // 固定宽度保证对齐
            )

            Text(
                text = school.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 拼音处理工具函数
private fun getPinyinInitial(name: String): String {
    return try {
        // 使用中文转拼音库实现，这里使用伪代码示例
        PinyinHelper.toHanyuPinyinStringArray(name[0])?.firstOrNull()?.first()?.uppercaseChar()
            ?.toString() ?: "#"
    } catch (e: Exception) {
        "#"
    }
}
