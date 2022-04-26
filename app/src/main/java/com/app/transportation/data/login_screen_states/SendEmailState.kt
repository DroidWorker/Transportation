package com.app.transportation.data.login_screen_states

sealed class SendEmailState(val message: String = "") {
    class Success(val code: List<Char>) : SendEmailState()
    class InvalidEmail(message: String) : SendEmailState(message)
    class EmailNotFound(message: String) : SendEmailState(message)
    class PhoneNotFound(message: String) : SendEmailState(message)
    class UnexpectedError(message: String) : SendEmailState(message)
}