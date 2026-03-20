package com.peekr.data.remote.telegram

import android.content.Context
import com.peekr.core.logger.AppLogger
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.dao.ApiKeyDao
import com.peekr.data.local.dao.PostDao
import com.peekr.data.local.entities.AccountEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class TelegramAuthState {
    object Idle : TelegramAuthState()
    object WaitingPhone : TelegramAuthState()
    object WaitingCode : TelegramAuthState()
    object WaitingPassword : TelegramAuthState()
    object Authorized : TelegramAuthState()
    data class Error(val message: String) : TelegramAuthState()
}

@Singleton
class TelegramClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyDao: ApiKeyDao,
    private val accountDao: AccountDao,
    private val postDao: PostDao,
    private val logger: AppLogger
) {
    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Idle)
    val authState: StateFlow<TelegramAuthState> = _authState

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiId = apiKeyDao.getApiKeyByPlatform("telegram_id")?.keyValue?.toIntOrNull()
            val apiHash = apiKeyDao.getApiKeyByPlatform("telegram_hash")?.keyValue
            if (apiId == null || apiHash.isNullOrEmpty()) {
                logger.warning("Telegram API ID أو Hash غير موجود", "telegram")
                _authState.value = TelegramAuthState.Error("أضف API ID و API Hash في الإعدادات أولاً")
                return@withContext false
            }
            _authState.value = TelegramAuthState.WaitingPhone
            logger.info("تليجرام: جاهز لتسجيل الدخول", "telegram")
            true
        } catch (e: Exception) {
            logger.error("فشل تهيئة تليجرام", "telegram", e)
            _authState.value = TelegramAuthState.Error(e.message ?: "خطأ غير معروف")
            false
        }
    }

    suspend fun sendPhoneNumber(phone: String) = withContext(Dispatchers.IO) {
        if (phone.isBlank()) {
            _authState.value = TelegramAuthState.Error("أدخل رقم الهاتف")
            return@withContext
        }
        _authState.value = TelegramAuthState.WaitingCode
    }

    suspend fun sendCode(code: String) = withContext(Dispatchers.IO) {
        if (code.isBlank()) {
            _authState.value = TelegramAuthState.Error("أدخل كود التحقق")
            return@withContext
        }
        accountDao.insertAccount(
            AccountEntity(
                platformId = "telegram",
                accountName = "تليجرام",
                isConnected = true,
                connectedAt = System.currentTimeMillis(),
                extraData = "stub_session"
            )
        )
        _authState.value = TelegramAuthState.Authorized
        logger.info("تليجرام: تم تسجيل الدخول المحلي", "telegram")
    }

    suspend fun sendPassword(password: String) = withContext(Dispatchers.IO) {
        if (_authState.value == TelegramAuthState.WaitingPassword && password.isBlank()) {
            _authState.value = TelegramAuthState.Error("أدخل كلمة المرور")
        }
    }

    suspend fun syncChats(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (_authState.value != TelegramAuthState.Authorized) {
                return@withContext Result.failure(Exception("تليجرام مش متصل"))
            }
            logger.info("تليجرام: مزامنة تجريبية بدون TDLib في هذه النسخة", "telegram")
            Result.success(0)
        } catch (e: Exception) {
            logger.error("خطأ في مزامنة تليجرام", "telegram", e)
            Result.failure(e)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            accountDao.deleteAccountByPlatform("telegram")
            _authState.value = TelegramAuthState.Idle
            logger.info("تليجرام: تم قطع الاتصال", "telegram")
        } catch (e: Exception) {
            logger.error("خطأ في قطع الاتصال", "telegram", e)
        }
    }

    fun isAuthorized() = _authState.value == TelegramAuthState.Authorized
}
