package com.example.application.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T, val response: Response? = null) : ApiResult<T>
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>
    data class RateLimited(val retryAfterSeconds: Int) : ApiResult<Nothing>
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>
}

object ApiService {

    // Sviluppo: serve "adb reverse tcp:8443 tcp:8443" dopo ogni riavvio di emulatore/adb.
    // Senza, l'app mostra il popup ERR-101 (comportamento corretto).
    const val BASE_URL = "https://localhost:8443"

    val gson = Gson()
    private val JSON = "application/json".toMediaType()
    private val refreshLock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val TAG = "ApiService"

    private fun reportNetworkFailure() {
        scope.launch { if (!isBackendReachable()) AppStatus.showNetworkError() }
    }

    @Volatile var onSessionExpired: (() -> Unit)? = null

    private val bareClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val authenticator = Authenticator { _, response ->
        if (response.priorResponse != null) return@Authenticator null
        val sentHeader = response.request.header("Authorization") ?: return@Authenticator null
        val sentToken = sentHeader.removePrefix("Bearer ")

        synchronized(refreshLock) {
            val current = UserSession.accessToken
            if (current != null && current != sentToken) {
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $current").build()
            }
            when (refreshTokens()) {
                RefreshOutcome.OK -> response.request.newBuilder()
                    .header("Authorization", "Bearer ${UserSession.accessToken}").build()
                RefreshOutcome.INVALID -> {
                    forceLogout()
                    null
                }
                RefreshOutcome.NETWORK -> { reportNetworkFailure(); null }
            }
        }
    }

    private val client = bareClient.newBuilder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            UserSession.accessToken?.takeIf { it.isNotBlank() }?.let {
                builder.header("Authorization", "Bearer $it")
            }
            chain.proceed(builder.build())
        }
        .authenticator(authenticator)
        .build()

    // ---------- API pubblica ----------

    suspend fun <T> get(path: String, type: Class<T>) = call("GET", path, null, type)
    suspend fun <T> post(path: String, body: Any?, type: Class<T>) = call("POST", path, body, type)
    suspend fun <T> put(path: String, body: Any?, type: Class<T>) = call("PUT", path, body, type)
    suspend fun <T> delete(path: String, type: Class<T>) = call("DELETE", path, null, type)

    /** Uses the same authenticated client and token refresh as ordinary API calls. */
    suspend fun uploadPhoto(path: String, field: String, bytes: ByteArray, mimeType: String): ApiResult<Unit> =
        uploadPhoto(path, field, bytes, mimeType, Unit::class.java)

    suspend fun <T> uploadPhoto(path: String, field: String, bytes: ByteArray, mimeType: String, type: Class<T>): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val body = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart(field, "photo", bytes.toRequestBody(mimeType.toMediaType())).build()
                val request = Request.Builder().url(resolveUrl(path)).post(body).build()
                client.newCall(request).execute().use { mapResponse(it, type) }
            } catch (exception: IOException) {
                reportNetworkFailure()
                ApiResult.NetworkError(exception)
            } catch (exception: com.google.gson.JsonParseException) {
                ApiResult.HttpError(502, "Risposta JSON non valida")
            }
        }

    fun resolveUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return BASE_URL + if (path.startsWith("/")) path else "/$path"
    }

    suspend fun <T> call(method: String, path: String, body: Any?, type: Class<T>): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(buildRequest(method, path, body)).execute().use { response ->
                    mapResponse(response, type)
                }
            } catch (e: IOException) {
                reportNetworkFailure()
                ApiResult.NetworkError(e)
            } catch (e: com.google.gson.JsonParseException) {
                ApiResult.HttpError(502, "Risposta JSON non valida")
            }
        }

    suspend fun postRaw(path: String, body: Any?): ApiResult<Pair<String, okhttp3.Headers>> =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(buildRequest("POST", path, body)).execute().use { response ->
                    when {
                        response.isSuccessful ->
                            ApiResult.Success(Pair(response.body?.string().orEmpty(), response.headers))
                        response.code == 429 -> rateLimited(response)
                        else -> ApiResult.HttpError(response.code, response.body?.string().orEmpty().take(300))
                    }
                }
            } catch (e: IOException) {
                reportNetworkFailure()
                ApiResult.NetworkError(e)
            }
        }

    suspend fun getRaw(
        path: String
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            client.newCall(
                buildRequest("GET", path, null)
            ).execute().use { response ->
                when {
                    response.isSuccessful ->
                        ApiResult.Success(
                            response.body?.string().orEmpty()
                        )

                    response.code == 429 ->
                        rateLimited(response)

                    else ->
                        ApiResult.HttpError(
                            response.code,
                            response.body?.string().orEmpty().take(300)
                        )
                }
            }
        } catch (e: IOException) {
            reportNetworkFailure()
            ApiResult.NetworkError(e)
        }
    }

    suspend fun isBackendReachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            bareClient.newCall(Request.Builder().url("$BASE_URL/api/health").build())
                .execute().use { Log.d(TAG, "Health check: HTTP ${it.code}") }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Health check fallito verso $BASE_URL: ${e::class.simpleName}: ${e.message}")
            false
        }
    }

    // ---------- interni ----------

    private fun buildRequest(method: String, path: String, body: Any?): Request {
        val json = when (body) {
            null -> null
            is String -> body
            else -> gson.toJson(body)
        }
        val requestBody = json?.toRequestBody(JSON)
        return Request.Builder()
            .url(resolveUrl(path))
            .method(method, requestBody ?: if (method == "POST" || method == "PUT") "".toRequestBody(JSON) else null)
            .build()
    }

    private fun <T> mapResponse(response: Response, type: Class<T>): ApiResult<T> = when {
        response.isSuccessful -> {
            val text = response.body?.string().orEmpty()
            @Suppress("UNCHECKED_CAST")
            val data = if (type == Unit::class.java) Unit as T else {
                if (text.isBlank() || text.trim() == "null") throw com.google.gson.JsonParseException("Risposta vuota")
                gson.fromJson(text, type)
            }
            ApiResult.Success(data, response)
        }
        response.code == 429 -> rateLimited(response)
        else -> ApiResult.HttpError(response.code, response.body?.string().orEmpty().take(300))
    }

    private fun rateLimited(response: Response): ApiResult.RateLimited {
        val retry = response.header("Retry-After")?.trim()?.toIntOrNull() ?: 60
        Log.w(TAG, "Rate limited, riprova tra $retry s")
        return ApiResult.RateLimited(retry)
    }

    internal enum class RefreshOutcome { OK, INVALID, NETWORK }

    private fun refreshTokens(): RefreshOutcome {
        val refresh = UserSession.refreshToken
        if (refresh.isNullOrBlank()) return RefreshOutcome.INVALID
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/users/refresh")
                .header("X-Refresh-Token", refresh)
                .post("".toRequestBody(JSON))
                .build()
            bareClient.newCall(request).execute().use { r ->
                val auth = r.header("Authorization").orEmpty()
                when {
                    r.code == 200 && auth.startsWith("Bearer ") -> {
                        UserSession.setTokens(auth.removePrefix("Bearer "), refresh)
                        RefreshOutcome.OK
                    }
                    r.code == 401 || r.code == 403 -> RefreshOutcome.INVALID
                    else -> RefreshOutcome.NETWORK
                }
            }
        } catch (e: IOException) {
            RefreshOutcome.NETWORK
        }
    }

    private fun forceLogout() {
        UserSession.clear()
        TokenStorage.clear()
        AppStatus.showAuthExpired()
        onSessionExpired?.invoke()
    }

    internal fun refreshBlocking(): RefreshOutcome = synchronized(refreshLock) { refreshTokens() }
}