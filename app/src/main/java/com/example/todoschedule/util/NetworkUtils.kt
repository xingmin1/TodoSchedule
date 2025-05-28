package com.example.todoschedule.util

import android.util.Log
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * 网络工具类
 * 提供网络请求重试和错误处理功能
 */
object NetworkUtils {

    private const val TAG = "NetworkUtils"

    /**
     * 带有重试功能的网络请求包装器
     *
     * @param maxRetries 最大重试次数
     * @param initialDelayMs 初始延迟毫秒数
     * @param tag 日志标签
     * @param block 要执行的挂起函数块
     * @return 函数块的返回值
     */
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 500,
        tag: String = TAG,
        block: suspend () -> T
    ): T {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    Log.d(tag, "尝试第${retryCount}次重试...")
                }

                // 执行网络请求
                return block()

            } catch (e: IOException) {
                // 网络错误，通常可以重试
                lastException = e
                Log.e(tag, "网络错误: ${e.message}", e)

                if (retryCount < maxRetries) {
                    retryCount++
                    val delayMs = (initialDelayMs * (1 shl (retryCount - 1)))
                    Log.d(tag, "将在${delayMs}ms后重试")
                    delay(delayMs)
                    continue
                } else {
                    throw e
                }
            } catch (e: HttpException) {
                // HTTP错误
                lastException = e
                Log.e(tag, "HTTP错误: ${e.code()}", e)

                if (isRetryableHttpCode(e.code()) && retryCount < maxRetries) {
                    retryCount++
                    val delayMs = (initialDelayMs * (1 shl (retryCount - 1)))
                    Log.d(tag, "将在${delayMs}ms后重试")
                    delay(delayMs)
                    continue
                } else {
                    throw e
                }
            } catch (e: Exception) {
                // 其他错误
                lastException = e
                Log.e(tag, "未知错误: ${e.message}", e)

                // 对于未知异常，保守起见只重试一次
                if (retryCount == 0) {
                    retryCount++
                    Log.d(tag, "将在${initialDelayMs * 2}ms后重试")
                    delay(initialDelayMs * 2)
                    continue
                } else {
                    throw e
                }
            }
        }

        // 这行代码不应该被执行到，但为了编译器而保留
        throw lastException ?: IllegalStateException("未知重试错误")
    }

    /**
     * 判断是否是可重试的HTTP状态码
     */
    fun isRetryableHttpCode(code: Int): Boolean {
        // 5xx服务器错误通常可以重试
        // 429 Too Many Requests也可以重试
        // 部分408 Request Timeout可以重试
        return code >= 500 || code == 429 || code == 408
    }

    /**
     * 判断是否是可重试的错误消息
     */
    fun isRetryableError(errorMsg: String): Boolean {
        // 包含这些关键词的错误通常可以重试
        val retryableKeywords = listOf(
            "timeout", "超时",
            "connection", "连接",
            "temporary", "暂时",
            "overloaded", "过载",
            "try again", "重试"
        )

        return retryableKeywords.any { errorMsg.contains(it, ignoreCase = true) }
    }
}
