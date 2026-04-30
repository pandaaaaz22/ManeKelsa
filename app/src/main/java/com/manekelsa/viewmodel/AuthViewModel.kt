package com.manekelsa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manekelsa.model.User
import com.manekelsa.model.UserRole
import com.manekelsa.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.signIn(email, pass).fold(
                onSuccess = { _authState.value = AuthState.Authenticated(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun register(email: String, pass: String, name: String, role: UserRole) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.signUp(email, pass, name, role).fold(
                onSuccess = { _authState.value = AuthState.Authenticated(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun checkUserSession() {
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                val userData = repository.getUserData(currentUser.uid)
                if (userData != null) {
                    _authState.value = AuthState.Authenticated(userData)
                } else {
                    _authState.value = AuthState.Idle
                }
            }
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }
}
