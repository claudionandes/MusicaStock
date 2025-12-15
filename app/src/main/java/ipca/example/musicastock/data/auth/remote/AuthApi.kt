package ipca.example.musicastock.data.remote.api

import ipca.example.musicastock.data.auth.dto.ForgotPasswordRequest
import ipca.example.musicastock.data.auth.dto.ResetPasswordRequest
import ipca.example.musicastock.data.remote.dto.AuthResponseDto
import ipca.example.musicastock.data.remote.dto.LoginRequestDto
import ipca.example.musicastock.data.remote.dto.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    // TODO: ajustar rota para a rota real do Swagger (ex.: api/auth/login)
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<AuthResponseDto>

    // TODO: ajustar rota para a rota real do Swagger (ex.: api/auth/register)
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<AuthResponseDto>
    @POST("api/Auth/forgot-password")

    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<Unit>

    @POST("api/Auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<Unit>
}
