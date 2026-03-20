package com.peekr.ui.settings.apikeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.dao.ApiKeyDao
import com.peekr.data.local.entities.ApiKeyEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject

data class PlatformStatusItem(
    val id: String,
    val name: String,
    val color: Long,
    val hasRequiredKeys: Boolean,
    val isConnected: Boolean?,   // null = not tested yet
    val isTesting: Boolean = false
)

data class ApiKeysUiState(
    val keyValues: Map<String, String> = emptyMap(),
    val platformStatuses: List<PlatformStatusItem> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)

// minimal YouTube ping interface
private interface YoutubePingApi {
    @GET("channels")
    suspend fun ping(
        @Query("part") part: String = "id",
        @Query("mine") mine: Boolean = false,
        @Query("id") id: String = "UCVHFbw7woebKtGBsxKtjHKg", // YouTube official channel
        @Query("key") apiKey: String
    ): Map<String, Any>
}

@HiltViewModel
class ApiKeysViewModel @Inject constructor(
    private val apiKeyDao: ApiKeyDao,
    private val accountDao: AccountDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeysUiState())
    val uiState: StateFlow<ApiKeysUiState> = _uiState.asStateFlow()

    private val youtubePingApi: YoutubePingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubePingApi::class.java)
    }

    init {
        loadKeys()
    }

    // ==============================
    // تحميل المفاتيح من DB
    // ==============================
    private fun loadKeys() {
        viewModelScope.launch {
            val allKeys = apiKeyDao.getAllApiKeys()
            val keyMap = mutableMapOf<String, String>()
            allKeys.collect { keys ->
                keys.forEach { keyMap[it.platformId] = it.keyValue }
                _uiState.update { it.copy(keyValues = keyMap.toMap()) }
                refreshPlatformStatuses(keyMap)
            }
        }
    }

    // ==============================
    // تحديث قيمة مفتاح في الـ UI state
    // ==============================
    fun updateKeyValue(platformId: String, value: String) {
        _uiState.update { state ->
            state.copy(
                keyValues = state.keyValues + (platformId to value),
                savedSuccess = false
            )
        }
    }

    // ==============================
    // حفظ كل المفاتيح في DB
    // ==============================
    fun saveAllKeys() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                _uiState.value.keyValues.forEach { (id, value) ->
                    if (value.isNotBlank()) {
                        apiKeyDao.insertApiKey(
                            ApiKeyEntity(
                                platformId = id,
                                keyName = id,
                                keyValue = value.trim()
                            )
                        )
                    }
                }
                _uiState.update { it.copy(isSaving = false, savedSuccess = true) }
                // أعد تحميل المفاتيح ليتحدث الـ status
                delay(300)
                refreshStatusesFromDb()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // ==============================
    // اختبار الاتصال لمنصة معينة
    // ==============================
    fun testConnection(platformId: String) {
        viewModelScope.launch {
            // ضع في حالة "جاري الاختبار"
            updateStatus(platformId, isTesting = true, isConnected = null)

            val result = when (platformId) {
                "youtube" -> testYouTube()
                "telegram_id", "telegram_hash" -> testTelegram()
                "facebook" -> testFacebook()
                else -> false
            }
            updateStatus(platformId, isTesting = false, isConnected = result)
        }
    }

    private suspend fun testYouTube(): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = apiKeyDao.getApiKeyByPlatform("youtube")?.keyValue ?: return@withContext false
            youtubePingApi.ping(apiKey = key)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun testTelegram(): Boolean {
        val id = apiKeyDao.getApiKeyByPlatform("telegram_id")?.keyValue
        val hash = apiKeyDao.getApiKeyByPlatform("telegram_hash")?.keyValue
        return !id.isNullOrBlank() && !hash.isNullOrBlank() &&
               id.toIntOrNull() != null && hash.length >= 10
    }

    private suspend fun testFacebook(): Boolean {
        val token = apiKeyDao.getApiKeyByPlatform("facebook")?.keyValue
        return !token.isNullOrBlank() && token.length > 20
    }

    // ==============================
    // Helpers
    // ==============================
    private fun updateStatus(platformId: String, isTesting: Boolean, isConnected: Boolean?) {
        _uiState.update { state ->
            state.copy(
                platformStatuses = state.platformStatuses.map { item ->
                    if (item.id == platformId || platformId == "telegram_id" || platformId == "telegram_hash") {
                        val matches = item.id == platformId ||
                            (item.id == "telegram" && (platformId == "telegram_id" || platformId == "telegram_hash"))
                        if (matches) item.copy(isTesting = isTesting, isConnected = isConnected)
                        else item
                    } else item
                }
            )
        }
    }

    private suspend fun refreshStatusesFromDb() {
        val keyMap = mutableMapOf<String, String>()
        val allKeys = apiKeyDao.getAllApiKeys()
        allKeys.collect { keys ->
            keys.forEach { keyMap[it.platformId] = it.keyValue }
            refreshPlatformStatuses(keyMap)
        }
    }

    private fun refreshPlatformStatuses(keyMap: Map<String, String>) {
        val statuses = listOf(
            PlatformStatusItem(
                id = "telegram",
                name = "تليجرام",
                color = 0xFF0088CC,
                hasRequiredKeys = keyMap.containsKey("telegram_id") && keyMap.containsKey("telegram_hash"),
                isConnected = null
            ),
            PlatformStatusItem(
                id = "youtube",
                name = "يوتيوب",
                color = 0xFFFF0000,
                hasRequiredKeys = keyMap.containsKey("youtube"),
                isConnected = null
            ),
            PlatformStatusItem(
                id = "facebook",
                name = "فيسبوك",
                color = 0xFF1877F2,
                hasRequiredKeys = keyMap.containsKey("facebook"),
                isConnected = null
            ),
            PlatformStatusItem(
                id = "rss",
                name = "RSS",
                color = 0xFFFF6600,
                hasRequiredKeys = true, // RSS لا يحتاج API Key
                isConnected = true
            ),
        )
        _uiState.update { it.copy(platformStatuses = statuses) }
    }
}
