package ipca.example.musicastock.data.remote.api

import ipca.example.musicastock.data.remote.dto.MusicCollectionDto
import ipca.example.musicastock.data.remote.dto.MusicDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CollectionsApi {

    @GET("api/Collections")
    suspend fun getAll(): List<MusicCollectionDto>

    @GET("api/Collections/{id}")
    suspend fun getById(@Path("id") id: String): MusicCollectionDto

    @GET("api/Collections/owner/{ownerId}")
    suspend fun getByOwner(@Path("ownerId") ownerId: String): List<MusicCollectionDto>

    @GET("api/Collections/search")
    suspend fun searchByTitle(@Query("title") title: String): List<MusicCollectionDto>

    @POST("api/Collections")
    suspend fun create(@Body body: MusicCollectionDto): MusicCollectionDto

    /**
     * A API devolve 204 NoContent quando atualiza com sucesso.
     * Usamos Response<Unit> para validar isSuccessful e obter code().
     */
    @PUT("api/Collections/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: MusicCollectionDto
    ): Response<Unit>

    /**
     * A API devolve 204 NoContent quando apaga com sucesso.
     * Usamos Response<Unit> para validar isSuccessful e obter code().
     */
    @DELETE("api/Collections/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    // -----------------------------
    // Relação Coleções <-> Músicas
    // -----------------------------

    @GET("api/Collections/{collectionId}/musics")
    suspend fun getMusicsForCollection(
        @Path("collectionId") collectionId: String
    ): List<MusicDto>

    /**
     * Cria associação Música -> Coleção (201 Created quando sucesso).
     * Response<Unit> para validar isSuccessful e code().
     */
    @POST("api/Collections/{collectionId}/musics/{musicId}")
    suspend fun addMusicToCollection(
        @Path("collectionId") collectionId: String,
        @Path("musicId") musicId: String
    ): Response<Unit>

    /**
     * Remove associação Música -> Coleção (204 NoContent quando sucesso).
     * Response<Unit> para validar isSuccessful e code().
     */
    @DELETE("api/Collections/{collectionId}/musics/{musicId}")
    suspend fun removeMusicFromCollection(
        @Path("collectionId") collectionId: String,
        @Path("musicId") musicId: String
    ): Response<Unit>
}
