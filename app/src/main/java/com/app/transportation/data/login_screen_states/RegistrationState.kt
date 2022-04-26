package com.app.transportation.data.login_screen_states

sealed class RegistrationState(val message: String = "") {
    object Registering : RegistrationState()
    class Success(message: String) : RegistrationState(message)
    class Warning(message: String) : RegistrationState(message)
    class Failure(message: String) : RegistrationState(message)
}