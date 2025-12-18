package ipca.example.musicastock.data.auth.remote // <-- TROCA para o package igual ao AuthApi.kt

import ipca.example.musicastock.data.remote.dto.CreateEnvironmentRequestDto
import ipca.example.musicastock.data.remote.dto.EnvironmentStatusDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EnvironmentsApi {

    @GET("api/Environments/{id}/status")
    suspend fun getEnvironmentStatus(
        @Path("id") id: String
    ): EnvironmentStatusDto

    @GET("api/Environments")
    suspend fun getEnvironmentsRaw(): Response<ResponseBody>

    @POST("api/Environments")
    suspend fun createEnvironmentRaw(
        @Body body: CreateEnvironmentRequestDto
    ): Response<ResponseBody>
}
