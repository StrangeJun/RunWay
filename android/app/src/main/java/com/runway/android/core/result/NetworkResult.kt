package com.runway.android.core.result

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.runway.android.core.model.ApiResponse
import retrofit2.HttpException

/**
 * 모든 네트워크 호출 결과를 표현하는 sealed class.
 *
 * ViewModel에서 다음과 같이 처리한다:
 * when (result) {
 *     is NetworkResult.Success   -> // result.data 사용
 *     is NetworkResult.ApiError  -> // result.errorCode로 UI 메시지 결정
 *     is NetworkResult.NetworkError -> // 연결 오류 처리
 * }
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()

    data class ApiError(
        val statusCode: Int,
        val errorCode: String?,
        val message: String,
    ) : NetworkResult<Nothing>()

    data class NetworkError(val throwable: Throwable) : NetworkResult<Nothing>()
}

/**
 * Retrofit suspend API 호출을 NetworkResult로 변환하는 helper.
 *
 * - 성공(2xx + success=true): NetworkResult.Success
 * - 서버 에러(4xx/5xx): 응답 body에서 errorCode 파싱 → NetworkResult.ApiError
 * - 네트워크 오류(IOException 등): NetworkResult.NetworkError
 *
 * Retrofit이 4xx/5xx에서 HttpException을 throw하므로,
 * errorCode를 얻으려면 error body를 직접 파싱한다.
 */
suspend fun <T : Any> safeApiCall(call: suspend () -> ApiResponse<T>): NetworkResult<T> {
    return try {
        val response = call()
        if (response.success && response.data != null) {
            NetworkResult.Success(response.data)
        } else {
            NetworkResult.ApiError(
                statusCode = 0,
                errorCode = response.errorCode,
                message = response.message,
            )
        }
    } catch (e: HttpException) {
        val errorBody = e.response()?.errorBody()?.string()
        if (errorBody != null) {
            runCatching {
                val json: JsonObject = JsonParser.parseString(errorBody).asJsonObject
                NetworkResult.ApiError(
                    statusCode = e.code(),
                    errorCode = json.get("errorCode")?.takeIf { !it.isJsonNull }?.asString,
                    message = json.get("message")?.asString ?: "HTTP ${e.code()}",
                )
            }.getOrElse {
                NetworkResult.ApiError(e.code(), null, "HTTP ${e.code()}")
            }
        } else {
            NetworkResult.ApiError(e.code(), null, "HTTP ${e.code()}")
        }
    } catch (e: Exception) {
        NetworkResult.NetworkError(e)
    }
}
