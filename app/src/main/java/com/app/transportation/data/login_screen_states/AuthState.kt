package com.app.transportation.data.login_screen_states

sealed class AuthState {
    object Authorizing : AuthState()
    object Success : AuthState()
    object IncorrectPassword : AuthState()
    object UserNotFound : AuthState()
    object UnexpectedError : AuthState()
}