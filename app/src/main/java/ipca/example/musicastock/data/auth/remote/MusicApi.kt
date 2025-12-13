package ipca.example.musicastock.data.remote.api

import ipca.example.musicastock.data.remote.dto.MusicDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MusicApi {

    @GET("api/Musics")
    suspend fun getAll(): List<MusicDto>

    @GET("api/Musics/{id}")
    suspend fun getById(@Path("id") id: String): MusicDto

    @POST("api/Musics")
    suspend fun create(@Body body: MusicDto): MusicDto

    /**
     * A API devolve 204 NoContent quando atualiza com sucesso.
     * Usamos Response<Unit> para conseguir validar isSuccessful e ler o code().
     */
    @PUT("api/Musics/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: MusicDto
    ): Response<Unit>

    /**
     * A API devolve 204 NoContent quando apaga com sucesso.
     * Usamos Response<Unit> para conseguir validar isSuccessful e ler o code().
     */
    @DELETE("api/Musics/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
