package com.groupb.scpd.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groupb.scpd.data.AuthRepository
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.groupb.scpd.ScpdApp
import com.groupb.scpd.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Everything the sign-in screen shows. */
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val profile: UserProfile? = null,   // set when sign-in succeeds
)

class SignInViewModel(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() {
        val current = _state.value
        if (current.loading) return
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = MSG_WRONG_DETAILS) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val profile = repository.signIn(current.email, current.password)
                _state.update { it.copy(loading = false, profile = profile) }
            } catch (e: IOException) {
                _state.update { it.copy(loading = false, error = MSG_OFFLINE) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = MSG_WRONG_DETAILS) }
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _state.value = SignInUiState()
    }

    companion object {
        // The exact wording from Part 1 story S1.
        const val MSG_WRONG_DETAILS = "Check your university username and password."
        const val MSG_OFFLINE = "No internet connection. Connect and try again."
        // Builds the ViewModel with the repository from the AppContainer.
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScpdApp
                SignInViewModel(app.container.authRepository)
            }
        }
    }
}