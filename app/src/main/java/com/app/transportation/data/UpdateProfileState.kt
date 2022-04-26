package com.app.transportation.data

sealed class UpdateProfileState {
    object Updating : UpdateProfileState()
    object Success : UpdateProfileState()
    class Failure(val message: String) : UpdateProfileState()
}