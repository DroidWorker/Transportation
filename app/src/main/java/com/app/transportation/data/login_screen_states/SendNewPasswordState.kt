package com.app.transportation.data.login_screen_states

sealed class SendNewPasswordState(val message: String = "") {
    object Success : SendNewPasswordState()
    class InvalidEmail(message: String) : SendNewPasswordState(message)
    class EmailNotFound(message: String) : SendNewPasswordState(message)
    class PhoneNotFound(message: String) : SendNewPasswordState(message)
    class InvalidPhone(message: String) : SendNewPasswordState(message)
    class RestoreExpired(message: String) : SendNewPasswordState(message)
    class UnexpectedError(message: String) : SendNewPasswordState(message)
}