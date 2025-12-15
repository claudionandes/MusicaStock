package ipca.example.musicastock.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.domain.repository.ILoginRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,

    val isForgotPasswordLoading: Boolean = false,
    val forgotPasswordMessage: String? = null,
    val forgotPasswordSent: Boolean = false // ✅ necessário para o auto-navigate após 5s
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: ILoginRepository
) : ViewModel() {

    var uiState = mutableStateOf(LoginUiState())
        private set

    fun setEmail(value: String) {
        uiState.value = uiState.value.copy(
            email = value,
            error = null,
            success = false,
            forgotPasswordMessage = null,
            forgotPasswordSent = false
        )
    }

    fun setPassword(value: String) {
        uiState.value = uiState.value.copy(
            password = value,
            error = null,
            success = false
        )
    }

    fun login(onSuccess: () -> Unit) {
        val email = uiState.value.email.trim()
        val pass = uiState.value.password.trim()

        if (email.isBlank() || pass.isBlank()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Email e palavra-passe são obrigatórios.",
                success = false
            )
            return
        }

        viewModelScope.launch {
            repository.login(email, pass).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> uiState.value = uiState.value.copy(
                        isLoading = true,
                        error = null,
                        success = false
                    )

                    is ResultWrapper.Error -> uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Erro no login.",
                        success = false
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

        if (email.isBlank() || pass.isBlank()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Email e palavra-passe são obrigatórios.",
                success = false
            )
            return
        }

        viewModelScope.launch {
            repository.register(email, pass).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> uiState.value = uiState.value.copy(
                        isLoading = true,
                        error = null,
                        success = false
                    )

                    is ResultWrapper.Error -> uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Erro no registo.",
                        success = false
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

    fun clearForgotMessage() {
        uiState.value = uiState.value.copy(forgotPasswordMessage = null)
    }

    fun clearForgotPasswordSent() {
        uiState.value = uiState.value.copy(forgotPasswordSent = false)
    }

    fun forgotPassword() {
        val email = uiState.value.email.trim()

        if (email.isBlank()) {
            uiState.value = uiState.value.copy(
                forgotPasswordMessage = "Introduza o e-mail para recuperar a password.",
                forgotPasswordSent = false
            )
            return
        }

        viewModelScope.launch {
            uiState.value = uiState.value.copy(
                isForgotPasswordLoading = true,
                forgotPasswordMessage = null,
                forgotPasswordSent = false,
                error = null
            )

            // ILoginRepository deve expor: suspend fun forgotPassword(email: String): ResultWrapper<Unit>
            when (val result = repository.forgotPassword(email)) {
                is ResultWrapper.Success -> {
                    uiState.value = uiState.value.copy(
                        isForgotPasswordLoading = false,
                        forgotPasswordMessage = "Se existir uma conta com este email, foi enviado um email com o token de recuperação.",
                        forgotPasswordSent = true
                    )
                }

                is ResultWrapper.Error -> {
                    uiState.value = uiState.value.copy(
                        isForgotPasswordLoading = false,
                        forgotPasswordMessage = result.message
                            ?: "Ocorreu um erro ao pedir recuperação de password.",
                        forgotPasswordSent = false
                    )
                }

                is ResultWrapper.Loading -> {
                    // não aplicável num método suspend (ignorável)
                    uiState.value = uiState.value.copy(isForgotPasswordLoading = true)
                }
            }
        }
    }
}
