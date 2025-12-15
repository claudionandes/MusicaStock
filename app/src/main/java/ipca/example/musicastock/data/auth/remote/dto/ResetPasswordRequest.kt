package ipca.example.musicastock.data.auth.dto

data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)
