package ipca.example.musicastock.data.auth

import kotlinx.coroutines.flow.Flow

interface TokenStore {
    val tokenFlow: Flow<String?>

    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}
