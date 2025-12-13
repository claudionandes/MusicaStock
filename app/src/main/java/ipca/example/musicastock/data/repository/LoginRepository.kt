package ipca.example.musicastock.data.repository

import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.auth.TokenStore
import ipca.example.musicastock.data.remote.api.AuthApi
import ipca.example.musicastock.data.remote.dto.LoginRequestDto
import ipca.example.musicastock.data.remote.dto.RegisterRequestDto
import ipca.example.musicastock.domain.repository.ILoginRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore
) : ILoginRepository {

    override fun login(email: String, password: String): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        try {
            val response = authApi.login(LoginRequestDto(email, password))

            if (!response.isSuccessful) {
                emit(ResultWrapper.Error("Login inválido (${response.code()})."))
                return@flow
            }

            val body = response.body()
            val token = body?.token

            if (token.isNullOrBlank()) {
                emit(ResultWrapper.Error("Login efetuado, mas a API não devolveu token (campo 'token')."))
                return@flow
            }

            tokenStore.saveToken(token)
            emit(ResultWrapper.Success(Unit))

        } catch (e: Exception) {
            emit(ResultWrapper.Error(e.message ?: "Erro desconhecido no login"))
        }
    }.flowOn(Dispatchers.IO)

    override fun register(email: String, password: String): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        try {
            val response = authApi.register(RegisterRequestDto(email, password))

            if (!response.isSuccessful) {
                emit(ResultWrapper.Error("Registo inválido (${response.code()})."))
                return@flow
            }

            val body = response.body()
            val token = body?.token

            // Se a API devolver token no registo, guarda-se e fica logo autenticado
            if (!token.isNullOrBlank()) {
                tokenStore.saveToken(token)
            }

            emit(ResultWrapper.Success(Unit))

        } catch (e: Exception) {
            emit(ResultWrapper.Error(e.message ?: "Erro ao registar"))
        }
    }.flowOn(Dispatchers.IO)
}
