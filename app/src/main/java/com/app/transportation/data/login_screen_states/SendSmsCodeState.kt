package com.app.transportation.data.login_screen_states

sealed class SendSmsCodeState(val message: String = "") {
    object Success : SendSmsCodeState()
    class InvalidEmail(message: String) : SendSmsCodeState(message)
    class EmailNotFound(message: String) : SendSmsCodeState(message)
    class PhoneNotFound(message: String) : SendSmsCodeState(message)
    class InvalidPhone(message: String) : SendSmsCodeState(message)
    class InvalidCode(message: String) : SendSmsCodeState(message)
    class RestoreExpired(message: String) : SendSmsCodeState(message)
    class UnexpectedError(message: String) : SendSmsCodeState(message)
}