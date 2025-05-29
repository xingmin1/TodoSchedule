package com.example.todoschedule.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * HTTP调试工具类
 *
 * 提供低级别的HTTP请求功能，用于直接测试API连接，绕过Retrofit
 * 主要用于调试网络问题
 */
object HttpDebugger {
    private const val TAG = "HttpDebugger"

    /**
     * 发送测试GET请求
     *
     * @param url 请求URL
     * @param headers 请求头
     * @return 响应结果，包含状态码和响应体
     */
    suspend fun testGet(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): HttpTestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "执行测试GET请求: $url")

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // 添加请求头
            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
                Log.d(TAG, "添加请求头: $key: $value")
            }

            // 记录请求详情
            Log.d(TAG, "请求URL: $url")
            Log.d(TAG, "请求方法: GET")
            Log.d(TAG, "请求头: ${connection.requestProperties}")

            // 获取响应
            val statusCode = connection.responseCode
            Log.d(TAG, "响应状态码: $statusCode")

            val responseBody = if (statusCode >= 400) {
                connection.errorStream?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                } ?: "No error body"
            } else {
                connection.inputStream.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                }
            }

            Log.d(
                TAG,
                "响应体: ${responseBody.take(500)}${if (responseBody.length > 500) "..." else ""}"
            )

            HttpTestResult(
                successful = statusCode in 200..299,
                statusCode = statusCode,
                responseBody = responseBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "测试GET请求失败: ${e.message}", e)
            HttpTestResult(
                successful = false,
                statusCode = -1,
                responseBody = "错误: ${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    /**
     * 发送测试POST请求
     *
     * @param url 请求URL
     * @param body 请求体
     * @param headers 请求头
     * @param contentType 内容类型
     * @return 响应结果，包含状态码和响应体
     */
    suspend fun testPost(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        contentType: String = "application/json"
    ): HttpTestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "执行测试POST请求: $url")

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Content-Type", contentType)

            // 添加请求头
            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
                Log.d(TAG, "添加请求头: $key: $value")
            }

            // 记录请求详情
            Log.d(TAG, "请求URL: $url")
            Log.d(TAG, "请求方法: POST")
            Log.d(TAG, "请求头: ${connection.requestProperties}")
            Log.d(TAG, "请求体: ${body.take(500)}${if (body.length > 500) "..." else ""}")

            // 写入请求体
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
                writer.flush()
            }

            // 获取响应
            val statusCode = connection.responseCode
            Log.d(TAG, "响应状态码: $statusCode")

            val responseBody = if (statusCode >= 400) {
                connection.errorStream?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                } ?: "No error body"
            } else {
                connection.inputStream.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                }
            }

            Log.d(
                TAG,
                "响应体: ${responseBody.take(500)}${if (responseBody.length > 500) "..." else ""}"
            )

            HttpTestResult(
                successful = statusCode in 200..299,
                statusCode = statusCode,
                responseBody = responseBody
            )
        } catch (e: Exception) {
            Log.e(TAG, "测试POST请求失败: ${e.message}", e)
            HttpTestResult(
                successful = false,
                statusCode = -1,
                responseBody = "错误: ${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    /**
     * HTTP测试结果类
     */
    data class HttpTestResult(
        val successful: Boolean,
        val statusCode: Int,
        val responseBody: String
    )
}
