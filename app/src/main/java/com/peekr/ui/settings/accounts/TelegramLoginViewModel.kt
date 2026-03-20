package com.peekr.ui.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.remote.telegram.TelegramAuthState
import com.peekr.data.remote.telegram.TelegramClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TelegramLoginViewModel @Inject constructor(
    private val telegramClient: TelegramClient
) : ViewModel() {

    val authState: StateFlow<TelegramAuthState> = telegramClient.authState

    init {
        initialize()
    }

    fun initialize() {
        viewModelScope.launch {
            telegramClient.initialize()
        }
    }

    fun sendPhone(phone: String) {
        viewModelScope.launch {
            telegramClient.sendPhoneNumber(phone)
        }
    }

    fun sendCode(code: String) {
        viewModelScope.launch {
            telegramClient.sendCode(code)
        }
    }

    fun sendPassword(password: String) {
        viewModelScope.launch {
            telegramClient.sendPassword(password)
        }
    }
}
