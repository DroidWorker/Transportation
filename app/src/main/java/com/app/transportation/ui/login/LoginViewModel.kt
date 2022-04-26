package com.app.transportation.ui.login

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.transportation.R
import com.app.transportation.data.LoginRepository
import com.app.transportation.data.Repository
import com.app.transportation.data.api.AuthResponse
import com.app.transportation.data.api.RegisterResponse
import com.app.transportation.data.login_screen_states.AuthState
import com.app.transportation.data.login_screen_states.RegistrationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class LoginViewModel(val app: Application) : AndroidViewModel(app), KoinComponent {

    private val repository: LoginRepository by inject()
    private val mainRepository: Repository by inject()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private var authToken: String?
        get() = prefs.getString("authToken", null)
        set(value) = prefs.edit { putString("authToken", value) }

    val authState = MutableSharedFlow<AuthState>(extraBufferCapacity = 1)
    val registrationState = MutableSharedFlow<RegistrationState>(extraBufferCapacity = 1)


    fun authorize(login: String, password: String) = viewModelScope.launch(Dispatchers.IO) {
        authState.tryEmit(AuthState.Authorizing)
        when (val result = repository.authorize(login, password)) {
            is AuthResponse.Success -> {
                mainRepository.saveLoginInProfile(login)
                authState.tryEmit(AuthState.Success)
            }
            is AuthResponse.Failure -> {
                when (result.message) {
                    "User not found" -> authState.tryEmit(AuthState.UserNotFound)
                    "Password not correct" -> authState.tryEmit(AuthState.IncorrectPassword)
                    "Unexpected error" -> authState.tryEmit(AuthState.UnexpectedError)
                }
            }
        }
    }

    fun register(login: String, password: String, name: String) =
        viewModelScope.launch(Dispatchers.IO) {
            registrationState.tryEmit(RegistrationState.Registering)
            when (val result = repository.register(login, password, name)) {
                is RegisterResponse.Success -> {
                    when (result.message) {
                        "User added" -> registrationState.tryEmit(
                            RegistrationState.Success(
                                app.getString(R.string.successful_registration)
                            ))
                    }
                }
                is RegisterResponse.Failure -> {
                    when (result.message) {
                        "Empty field" -> registrationState.tryEmit(
                            RegistrationState.Warning(
                                app.getString(R.string.not_all_fields_filled)
                            ))
                        "User login exists." -> registrationState.tryEmit(
                            RegistrationState.Warning(
                                app.getString(R.string.login_exists)
                            ))
                        "Invalid email" -> registrationState.tryEmit(
                            RegistrationState.Warning(
                                app.getString(R.string.invalid_email)
                            ))
                        "Invalid phone" -> registrationState.tryEmit(
                            RegistrationState.Warning(
                                app.getString(R.string.invalid_phone)
                            ))
                        "Database error" -> registrationState.tryEmit(
                            RegistrationState.Warning(
                                app.getString(R.string.database_error)
                            ))
                        else -> registrationState.tryEmit(
                            RegistrationState.Failure(app.getString(R.string.unexpected_error))
                        )
                    }
                    //TODO write exception data from 'it.message' into something
                }
            }
        }

}