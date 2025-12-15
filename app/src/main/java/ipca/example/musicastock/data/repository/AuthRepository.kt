package ipca.example.musicastock.data.auth

import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.auth.dto.ForgotPasswordRequest
import ipca.example.musicastock.data.remote.api.AuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi
) {

    // Já deves ter algo assim para o login...
    // suspend fun login(...): ResultWrapper<LoginResponse> { ... }

    suspend fun forgotPassword(email: String): ResultWrapper<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.forgotPassword(ForgotPasswordRequest(email))

                if (response.isSuccessful) {
                    ResultWrapper.Success(Unit)
                } else {
                    ResultWrapper.Error("Erro ao pedir recuperação de password. Código: ${response.code()}")
                }
            } catch (e: Exception) {
                ResultWrapper.Error("Falha na comunicação com o servidor: ${e.message}")
            }
        }
}
