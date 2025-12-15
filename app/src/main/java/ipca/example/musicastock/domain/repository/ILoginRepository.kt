package ipca.example.musicastock.domain.repository

import ipca.example.musicastock.data.ResultWrapper
import kotlinx.coroutines.flow.Flow

interface ILoginRepository {

    fun login(email: String, password: String): Flow<ResultWrapper<Unit>>

    fun register(email: String, password: String): Flow<ResultWrapper<Unit>>

    suspend fun forgotPassword(email: String): ResultWrapper<Unit>

    suspend fun resetPassword(token: String, newPassword: String): ResultWrapper<Unit>

}
