package com.app.transportation.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.transportation.R
import com.app.transportation.data.LoginRepository
import com.app.transportation.data.login_screen_states.SendSmsCodeState
import com.app.transportation.data.login_screen_states.SendEmailState
import com.app.transportation.data.api.SmsCodeResponse
import com.app.transportation.data.login_screen_states.SendNewPasswordState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PasswordRestorationVM(private val app: Application) : AndroidViewModel(app), KoinComponent {

    private val repository: LoginRepository by inject()

    var tempLogin: String? = null

    val sendEmailStateSF = MutableSharedFlow<SendEmailState>(extraBufferCapacity = 1)
    val sendSmsCodeStateSF = MutableSharedFlow<SendSmsCodeState>(extraBufferCapacity = 1)
    val sendNewPasswordStateSF = MutableSharedFlow<SendNewPasswordState>(extraBufferCapacity = 1)

    fun requestSmsCode(login: String) = viewModelScope.launch(Dispatchers.IO) {
        tempLogin = login
        when (val result = repository.requestSmsCode(login)) {
            is SmsCodeResponse.Success -> sendEmailStateSF.tryEmit(
                SendEmailState.Success(result.code.toString().toList())
            )
            is SmsCodeResponse.Failure -> {
                when (result.message) {
                    "Invalid email" -> sendEmailStateSF.tryEmit(
                        SendEmailState.InvalidEmail(app.getString(R.string.invalid_email))
                    )
                    "Email not found" -> sendEmailStateSF.tryEmit(
                        SendEmailState.EmailNotFound(app.getString(R.string.email_not_found))
                    )
                    "Phone not found" -> sendEmailStateSF.tryEmit(
                        SendEmailState.PhoneNotFound(app.getString(R.string.phone_not_found))
                    )
                    else -> sendEmailStateSF.tryEmit(
                        SendEmailState.UnexpectedError(app.getString(R.string.unexpected_error))
                    )
                }
            }
        }
    }

    fun sendSmsCode(code: List<String>) = viewModelScope.launch(Dispatchers.IO) {
        tempLogin?.let { login ->
            when (repository.sendSmsCode(login, code.joinToString(""))) {
                "Code accepted, specify new password." -> SendSmsCodeState.Success
                "Invalid email" -> SendSmsCodeState.InvalidEmail(app.getString(R.string.invalid_email))
                "Email not found" -> SendSmsCodeState.EmailNotFound(app.getString(R.string.email_not_found))
                "Phone not found" -> SendSmsCodeState.PhoneNotFound(app.getString(R.string.phone_not_found))
                "Invalid phone" -> SendSmsCodeState.InvalidPhone(app.getString(R.string.invalid_phone))
                "Invalid code" -> SendSmsCodeState.InvalidCode(app.getString(R.string.invalid_code))
                "Restore expired" -> SendSmsCodeState.RestoreExpired(app.getString(R.string.restoration_expired))
                else -> SendSmsCodeState.UnexpectedError(app.getString(R.string.unexpected_error))
            }.let { sendSmsCodeStateSF.tryEmit(it) }
        } ?: kotlin.run { /*TODO*/ }
    }

    fun sendNewPassword(login:String, password: String) = viewModelScope.launch(Dispatchers.IO) {
        when (repository.sendNewPassword(login, password)) {
            "Password saved." -> SendNewPasswordState.Success
            "Invalid email" -> SendNewPasswordState.InvalidEmail(app.getString(R.string.invalid_email))
            "Email not found" -> SendNewPasswordState.EmailNotFound(app.getString(R.string.email_not_found))
            "Phone not found" -> SendNewPasswordState.PhoneNotFound(app.getString(R.string.phone_not_found))
            "Invalid phone" -> SendNewPasswordState.InvalidPhone(app.getString(R.string.invalid_phone))
            "Restore expired" -> SendNewPasswordState.RestoreExpired(app.getString(R.string.restoration_expired))
            else -> SendNewPasswordState.UnexpectedError(app.getString(R.string.unexpected_error))
        }.let { sendNewPasswordStateSF.tryEmit(it) }
    }

    fun String.passwordIsOk() = length >= 8

}