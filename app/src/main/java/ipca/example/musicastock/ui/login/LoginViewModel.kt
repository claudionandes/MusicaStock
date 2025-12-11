package ipca.example.musicastock.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.repository.LoginRepository
import ipca.example.musicastock.domain.repository.ILoginRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: ILoginRepository
) : ViewModel() {

    var uiState = mutableStateOf(LoginUiState())
        private set

    fun setEmail(value: String) {
        uiState.value = uiState.value.copy(email = value)
    }

    fun setPassword(value: String) {
        uiState.value = uiState.value.copy(password = value)
    }

    fun login(onSuccess: () -> Unit) {
        val email = uiState.value.email.trim()
        val pass = uiState.value.password.trim()

        viewModelScope.launch {
            repository.login(email, pass).collect { result ->
                when (result) {

                    is ResultWrapper.Loading ->
                        uiState.value = uiState.value.copy(
                            isLoading = true,
                            error = null
                        )

                    is ResultWrapper.Error ->
                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )

                    is ResultWrapper.Success -> {
                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            success = true,
                            error = null
                        )
                        onSuccess()
                    }
                }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val email = uiState.value.email.trim()
        val pass = uiState.value.password.trim()

        viewModelScope.launch {
            repository.register(email, pass).collect { result ->
                when (result) {

                    is ResultWrapper.Loading ->
                        uiState.value = uiState.value.copy(
                            isLoading = true,
                            error = null
                        )

                    is ResultWrapper.Error ->
                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )

                    is ResultWrapper.Success -> {
                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            success = true,
                            error = null
                        )
                        onSuccess()
                    }
                }
            }
        }
    }
}
