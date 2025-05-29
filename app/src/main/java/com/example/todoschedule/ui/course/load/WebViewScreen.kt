package com.example.todoschedule.ui.course.load

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mykotlinapplication.utils.injectjs.injectHeadRepairScript
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.utils.courseadapter.bean.ParserResult
import main.java.parser.ZZUParser

@Composable
fun WebViewScreen(
    navigationState: NavigationState,
    url: String,
    tableId: Int,
    viewModel: WebViewScreenViewModel = hiltViewModel(),
) {
    var currentUrl by remember { mutableStateOf(url) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(true) } // 初始显示弹窗

    val context = LocalContext.current
    val saveState by viewModel.saveState.collectAsState()

    val webView = rememberWebView(
        initialUrl = url,
        onUrlChanged = { newUrl -> currentUrl = newUrl },
        onLoadingStateChanged = { loading -> isLoading = loading },
        isDesktopMode = isDesktopMode,
        onHistoryChanged = { canGoBack = it }
    )

    // 监听桌面模式变化并刷新WebView
    LaunchedEffect(isDesktopMode) {
        if (webView.url != null) {
            // 更新User-Agent
            webView.settings.userAgentString = if (isDesktopMode) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            } else {
                "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
            // 刷新当前页面以应用新的User-Agent
            webView.reload()
            Log.d("WebViewScreen", "模式已切换为: ${if (isDesktopMode) "电脑模式" else "手机模式"}")
        }
    }

    // 帮助弹窗内容
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(text = "使用提示", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text(
                    "1. 进入页面后用户需自行导航至课表页面再点击导入否则将导入失败\n2. 登录教务系统后如发生错误请点击刷新\n3. 如果刷新无效或出现错误请退出重进",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("知道了")
                }
            }
        )
    }

    // 根据保存状态显示不同内容或执行操作
    when (val state = saveState) {
        is SaveCourseState.Idle -> {
            // 初始状态或重置后的状态，可以显示保存按钮等
        }

        is SaveCourseState.Saving -> {
            // 显示加载指示器
            CircularProgressIndicator() // 或者其他加载 UI
        }

        is SaveCourseState.Success -> {
            // 保存成功
            // 可以显示一个 Snackbar 提示成功，然后导航回上一个屏幕
            LaunchedEffect(state) { // 使用 LaunchedEffect 在状态变为 Success 后执行一次性操作
                // scaffoldState.snackbarHostState.showSnackbar("保存成功！") // 假设你有 scaffoldState
                Log.d("WebViewScreen", "Save successful, table ID: ${state.tableId}")
                // 在这里执行导航操作，例如：
                // navController.popBackStack()
                // viewModel.resetSaveState() // 可选：导航后重置状态
            }
        }

        is SaveCourseState.Error -> {
            // 保存失败
            // 可以显示一个 Snackbar 提示错误信息
            LaunchedEffect(state) { // 使用 LaunchedEffect 在状态变为 Error 后执行一次性操作
                // scaffoldState.snackbarHostState.showSnackbar("保存失败: ${state.message}") // 假设你有 scaffoldState
                Log.e("WebViewScreen", "Save failed: ${state.message}")
                // 可以提供重试按钮，并在点击时调用 viewModel.saveCourse(...)
                // 用户处理完错误后，可能需要调用 viewModel.resetSaveState()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 美化后的URL输入栏
            var editingUrl by remember { mutableStateOf(currentUrl) }
            OutlinedTextField(
                value = editingUrl,
                onValueChange = { editingUrl = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors( // 使用正确的 Material 3 API
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Uri
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        currentUrl = editingUrl
                        webView.loadUrl(currentUrl)
                    }
                )
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            AndroidView(
                factory = { webView },
                modifier = Modifier.weight(1f)
            )
        }

        // 优化后的底部控制栏
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(56.dp) // 固定高度
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(
                onClick = { webView.goBack() },
                enabled = canGoBack,
                modifier = Modifier.size(40.dp) // 缩小按钮尺寸
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = if (canGoBack) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(24.dp) // 缩小图标尺寸
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 紧凑型电脑模式切换
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isDesktopMode = !isDesktopMode }
            ) {
                Switch(
                    checked = isDesktopMode,
                    onCheckedChange = { isDesktopMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .size(48.dp, 28.dp) // 自定义开关尺寸
                        .scale(0.8f) // 适当缩小
                )
                Text(
                    text = "电脑模式",
                    style = MaterialTheme.typography.labelMedium, // 使用更小的字体
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp) // 减少右边距
                )
            }
        }

        // 美化后的悬浮按钮列
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp) // 与底部栏保持间距
                .padding(end = 16.dp)
        ) {
            // 新增帮助按钮
            FloatingActionButton(
                onClick = { showHelpDialog = true },
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.AutoMirrored.Filled.Help, "帮助")
            }

            FloatingActionButton(
                onClick = { webView.reload() },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, "刷新")
            }

            FloatingActionButton(
                onClick = {
                    webView.evaluateJavascript("document.documentElement.outerHTML") { escapedHtml ->
                        // 处理转义字符
                        val unescapedHtml = escapedHtml
                            ?.removeSurrounding("\"")  // 去除首尾的双引号
                            ?.replace("\\\"", "\"")    // 替换转义双引号
                            ?.replace("\\n", "\n")     // 替换换行符
                            ?.replace("\\u003C", "<")    // Unicode 转义字符

                        if (unescapedHtml != null) {
                            try {
                                val parserResult = ZZUParser(unescapedHtml).saveCourse()
                                saveParserResult(
                                    tableId = tableId,
                                    parserResult = parserResult,
                                    viewModel = viewModel,
                                )
                                Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "导入失败", Toast.LENGTH_SHORT).show()
                            }

                        } else {
                            Toast.makeText(context, "导入失败", Toast.LENGTH_SHORT).show()
                        }
                        // 4. 返回导航操作
                        navigationState.navigateBack()
                        navigationState.navigateBack()
                        //navigationState.navigateToSchedule(isPop = true)
                    }
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(Icons.Default.Download, "下载")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun rememberWebView(
    initialUrl: String,
    onUrlChanged: (String) -> Unit,
    onLoadingStateChanged: (Boolean) -> Unit,
    isDesktopMode: Boolean,
    onHistoryChanged: (Boolean) -> Unit
): WebView {
    val context = LocalContext.current.applicationContext

    return remember(initialUrl) {
        WebView(context).apply {
            // 核心配置
            settings.apply {
                userAgentString = if (isDesktopMode) {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                } else {
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }

                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportMultipleWindows(true)
                defaultTextEncodingName = "UTF-8"

                useWideViewPort = true
                loadWithOverviewMode = true
                setGeolocationEnabled(true)
                allowContentAccess = true
                allowFileAccess = true

                supportZoom()
                builtInZoomControls = true
                displayZoomControls = true
                javaScriptCanOpenWindowsAutomatically = true
                textZoom = 100

                blockNetworkImage = false  // 不阻止网络图片
                blockNetworkLoads = false  // 不阻止网络请求

                // 性能优化
                cacheMode = WebSettings.LOAD_NO_CACHE

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

            }

            // Cookie管理
            CookieManager.getInstance().run {
                setAcceptThirdPartyCookies(this@apply, true)
                acceptCookie()
                flush()
            }

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient = object : WebViewClient() {

                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    onHistoryChanged(view?.canGoBack() ?: false)
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onLoadingStateChanged(true)
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)

                    if (!view.url.isNullOrEmpty() &&
                        view.progress == 100 &&
                        view.url!!.contains("zzu")
                    ) {
                        injectHeadRepairScript()
                    }
                    onLoadingStateChanged(false)
                    url?.let(onUrlChanged)
                }


                // 证书处理
                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    handler?.proceed()
                }

                // 优化错误处理
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Log.e("WebView", "加载错误：${error?.description}")
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress > 80) {
                        view?.settings?.run {
                            // 保持缩放设置稳定
                            textZoom = 100
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                    }
                }

                // 弹窗处理
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        (resultMsg?.obj as WebView.WebViewTransport).webView = this
                    }
                    resultMsg?.sendToTarget()
                    return true
                }
            }

            // 初始化加载
            clearCache(true)
            loadUrl(initialUrl)
        }
    }.also { webView ->
        // 添加生命周期监听更新历史状态
        DisposableEffect(webView) {
            val checkHistory = object : Runnable {
                override fun run() {
                    onHistoryChanged(webView.canGoBack())
                    webView.postDelayed(this, 500)
                }
            }
            webView.post(checkHistory)

            onDispose {
                // 清除 Cookie
                CookieManager.getInstance().run {
                    removeAllCookies(null) // 异步清除
                    flush() // 立即生效
                }

                // 清除 Web Storage 和数据库
                WebStorage.getInstance().deleteAllData()
                webView.clearFormData()

                // 完善销毁逻辑（参考原代码的onDestroyView）
                webView.stopLoading()
                webView.clearCache(true)
                webView.clearHistory()
                webView.destroy()
            }
        }

        // 远程调试支持
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
private fun saveParserResult(
    tableId: Int,
    parserResult: ParserResult,
    viewModel: WebViewScreenViewModel,
) {
    val courseList = parserResult.baseList.map { course ->
        Course(
            courseName = course.courseName,
            color = course.color,
            credit = course.credit,
            courseCode = course.courseID,
            nodes = parserResult.detailList
                .filter { it.id == course.id }
                .map {
                    CourseNode(
                        day = it.day,
                        startNode = it.startNode,
                        step = it.step,
                        startWeek = it.startWeek,
                        endWeek = it.endWeek,
                        weekType = it.type,
                        room = it.room ?: "",
                        teacher = it.teacher ?: ""
                    )
                }
        )
    }

    viewModel.saveCourse(tableId, courseList)
}
