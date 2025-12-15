package ipca.example.musicastock.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.domain.repository.ILoginRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val resetDone: Boolean = false
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val repository: ILoginRepository
) : ViewModel() {

    var uiState = mutableStateOf(ResetPasswordUiState())
        private set

    fun setNewPassword(value: String) {
        uiState.value = uiState.value.copy(
            newPassword = value,
            error = null,
            successMessage = null,
            resetDone = false
        )
    }

    fun setConfirmPassword(value: String) {
        uiState.value = uiState.value.copy(
            confirmPassword = value,
            error = null,
            successMessage = null,
            resetDone = false
        )
    }

    fun clearResetDone() {
        uiState.value = uiState.value.copy(resetDone = false)
    }

    fun resetPassword(token: String) {
        val state = uiState.value
        val trimmedToken = token.trim()

        if (trimmedToken.isBlank()) {
            uiState.value = state.copy(
                error = "Deve inserir o token de recuperação recebido por email.",
                successMessage = null,
                resetDone = false
            )
            return
        }

        if (state.newPassword.isBlank() || state.confirmPassword.isBlank()) {
            uiState.value = state.copy(
                error = "Deve preencher ambos os campos de password.",
                successMessage = null,
                resetDone = false
            )
            return
        }

        if (state.newPassword != state.confirmPassword) {
            uiState.value = state.copy(
                error = "As palavras-passe não coincidem.",
                successMessage = null,
                resetDone = false
            )
            return
        }

        viewModelScope.launch {
            uiState.value = uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null,
                resetDone = false
            )

            // ILoginRepository deve expor: suspend fun resetPassword(token: String, newPassword: String): ResultWrapper<Unit>
            when (val result = repository.resetPassword(trimmedToken, state.newPassword)) {
                is ResultWrapper.Success -> {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        successMessage = "Password atualizada com sucesso. Já pode fazer login com a nova password.",
                        error = null,
                        resetDone = true
                    )
                }

                is ResultWrapper.Error -> {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Ocorreu um erro ao atualizar a password.",
                        successMessage = null,
                        resetDone = false
                    )
                }

                is ResultWrapper.Loading -> {
                    uiState.value = uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}
