package com.deepseek.balancewidget

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * DeepSeek API client for fetching account balance.
 * Uses built-in HttpURLConnection — zero external dependencies.
 */
object DeepSeekApi {

    private const val BALANCE_URL = "https://api.deepseek.com/user/balance"
    private const val TIMEOUT_MS = 15_000L

    /**
     * Result of an API call.
     * @param T the data type on success.
     */
    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val message: String, val isAuthError: Boolean = false) : ApiResult<Nothing>()
    }

    /**
     * Fetch the current account balance.
     * @param apiKey DeepSeek API key
     * @return ApiResult with BalanceData or error info
     */
    fun fetchBalance(apiKey: String): ApiResult<BalanceData> {
        if (apiKey.isBlank()) {
            return ApiResult.Error("API Key 未设置")
        }

        return try {
            val url = URL(BALANCE_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = TIMEOUT_MS.toInt()
                readTimeout = TIMEOUT_MS.toInt()
                useCaches = false
            }

            val responseCode = connection.responseCode

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val balance = BalanceData.fromJson(json)
                    ApiResult.Success(balance)
                }

                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    ApiResult.Error("API Key 无效，请检查密钥", isAuthError = true)
                }

                HttpURLConnection.HTTP_FORBIDDEN -> {
                    ApiResult.Error("API Key 无权限访问", isAuthError = true)
                }

                429 -> {
                    ApiResult.Error("请求过于频繁，请稍后再试")
                }

                else -> {
                    ApiResult.Error("服务器错误 ($responseCode)")
                }
            }.also {
                connection.disconnect()
            }
        } catch (e: java.net.UnknownHostException) {
            ApiResult.Error("网络连接失败，请检查网络")
        } catch (e: java.net.SocketTimeoutException) {
            ApiResult.Error("请求超时，请稍后重试")
        } catch (e: IOException) {
            ApiResult.Error("网络错误：${e.localizedMessage ?: "未知错误"}")
        } catch (e: Exception) {
            ApiResult.Error("解析错误：${e.localizedMessage ?: "未知错误"}")
        }
    }
}
