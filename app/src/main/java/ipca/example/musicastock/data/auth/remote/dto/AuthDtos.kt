package ipca.example.musicastock.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterRequestDto(
    val email: String,
    val password: String
)

data class AuthResponseDto(
    // Aceita nomes diferentes que a API possa devolver
    @SerializedName(value = "token", alternate = ["accessToken", "jwt", "Token", "AccessToken"])
    val token: String? = null,

    @SerializedName(value = "userId", alternate = ["uid", "UserId"])
    val userId: String? = null,

    @SerializedName(value = "email", alternate = ["Email"])
    val email: String? = null
)
