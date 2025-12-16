package ipca.example.musicastock.data.repository

import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.remote.api.CollectionsApi
import ipca.example.musicastock.data.remote.api.MusicApi
import ipca.example.musicastock.data.remote.dto.MusicDto
import ipca.example.musicastock.domain.models.Music
import ipca.example.musicastock.domain.repository.IMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID
import javax.inject.Inject

class MusicRepository @Inject constructor(
    private val musicApi: MusicApi,
    private val collectionsApi: CollectionsApi,
    private val localRepository: MusicsLocalRepository
) : IMusicRepository {

    override fun fetchAllMusics(): Flow<ResultWrapper<List<Music>>> = flow {
        emit(ResultWrapper.Loading())
        try {
            val remote = musicApi.getAll().map { it.toDomain() }

            runCatching {
                localRepository.clearAll()
                localRepository.insertMusics(remote)
            }

            emit(ResultWrapper.Success(remote))
        } catch (e: Exception) {
            val cached = runCatching { localRepository.getAllMusics() }.getOrDefault(emptyList())
            if (cached.isNotEmpty()) emit(ResultWrapper.Success(cached))
            else emit(ResultWrapper.Error(e.message ?: "Erro ao carregar músicas."))
        }
    }.flowOn(Dispatchers.IO)

    override fun fetchMusicsByCollection(collectionId: String): Flow<ResultWrapper<List<Music>>> = flow {
        emit(ResultWrapper.Loading())
        try {
            val remote = collectionsApi.getMusicsForCollection(collectionId)
                .map { it.toDomain().copy(collectionId = collectionId) }

            runCatching { localRepository.insertMusics(remote) }

            emit(ResultWrapper.Success(remote))
        } catch (e: Exception) {
            val cached = runCatching { localRepository.getAllMusics() }
                .getOrDefault(emptyList())
                .filter { it.collectionId == collectionId }

            if (cached.isNotEmpty()) emit(ResultWrapper.Success(cached))
            else emit(ResultWrapper.Error(e.message ?: "Erro ao carregar músicas da coletânea."))
        }
    }.flowOn(Dispatchers.IO)

    override fun fetchMusics(collectionId: String?): Flow<ResultWrapper<List<Music>>> =
        if (collectionId.isNullOrBlank()) fetchAllMusics() else fetchMusicsByCollection(collectionId)

    override suspend fun getMusicById(id: String): ResultWrapper<Music?> {
        return try {
            val dto = musicApi.getById(id)
            ResultWrapper.Success(dto.toDomain())
        } catch (e: Exception) {
            val cached = runCatching { localRepository.getAllMusics() }.getOrNull()
                ?.firstOrNull { it.musId == id }

            if (cached != null) ResultWrapper.Success(cached)
            else ResultWrapper.Error(e.message ?: "Erro ao carregar música.")
        }
    }

    override fun saveMusic(music: Music): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        val title = music.musTitle?.trim()
        if (title.isNullOrBlank()) {
            emit(ResultWrapper.Error("O título é obrigatório."))
            return@flow
        }

        try {
            // CREATE (se quiseres continuar a suportar este cenário, musId tem de vir vazio/blank da UI)
            if (music.musId.isBlank()) {

                val created = musicApi.create(music.toDtoForCreate())

                val newId = created.musicId
                if (newId.isNullOrBlank()) {
                    emit(ResultWrapper.Error("A API não devolveu o ID da música criada."))
                    return@flow
                }

                // Se estiver a criar dentro de uma coleção, cria associação
                val colId = music.collectionId
                if (!colId.isNullOrBlank()) {
                    runCatching { collectionsApi.addMusicToCollection(colId!!, newId!!) }
                }

                // Guardar no Room com ID real + campos que a API pode não guardar
                val saved = created.toDomain().copy(
                    collectionId = colId,
                    musStyle = music.musStyle,
                    tabUrl = music.tabUrl
                )

                runCatching { localRepository.insertMusic(saved) }
                emit(ResultWrapper.Success(Unit))

            } else {
                // UPDATE
                val res = musicApi.update(music.musId, music.toDtoForUpdate())
                if (!res.isSuccessful) {
                    emit(ResultWrapper.Error("Erro ao atualizar música (${res.code()})."))
                    return@flow
                }

                runCatching { localRepository.insertMusic(music) }
                emit(ResultWrapper.Success(Unit))
            }
        } catch (_: Exception) {
            // OFFLINE: guarda em Room e devolve Error para o ViewModel mostrar o aviso
            val offline = if (music.musId.isBlank())
                music.copy(musId = UUID.randomUUID().toString())
            else
                music

            runCatching { localRepository.insertMusic(offline) }

            emit(ResultWrapper.Error("Sem ligação. Música guardada apenas localmente."))
        }
    }.flowOn(Dispatchers.IO)


    override fun removeMusicFromCollection(
        collectionId: String,
        musicId: String
    ): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        try {
            val res = collectionsApi.removeMusicFromCollection(collectionId, musicId)

            if (!res.isSuccessful) {
                // 👇 É AQUI que entra o que perguntaste
                val errorText = res.errorBody()?.string()
                emit(ResultWrapper.Error(errorText ?: "Erro (${res.code()})"))
                return@flow
            }

            // Só atualiza localmente quando a API confirmou sucesso
            runCatching {
                val all = localRepository.getAllMusics()
                val match = all.firstOrNull { it.musId == musicId }
                if (match != null) localRepository.insertMusic(match.copy(collectionId = null))
            }

            emit(ResultWrapper.Success(Unit))

        } catch (_: Exception) {
            // offline
            runCatching {
                val all = localRepository.getAllMusics()
                val match = all.firstOrNull { it.musId == musicId }
                if (match != null) localRepository.insertMusic(match.copy(collectionId = null))
            }

            emit(ResultWrapper.Error("Sem ligação. Remoção feita apenas localmente."))
        }
    }.flowOn(Dispatchers.IO)


    // -----------------------------
    // Mappers
    // -----------------------------
    private fun MusicDto.toDomain(): Music = Music(
        // ✅ Room: PK não pode ser null
        musId = this.musicId ?: UUID.randomUUID().toString(),
        musTitle = this.title,
        artist = this.artist,
        album = this.album,
        audioUrl = this.audioUrl,
        releaseDate = this.releaseDate
    )

    private fun Music.toDtoForCreate(): MusicDto = MusicDto(
        musicId = null,
        title = this.musTitle!!.trim(),
        artist = this.artist,
        album = this.album,
        audioUrl = this.audioUrl,
        releaseDate = this.releaseDate
    )

    private fun Music.toDtoForUpdate(): MusicDto = MusicDto(
        musicId = this.musId,
        title = this.musTitle!!.trim(),
        artist = this.artist,
        album = this.album,
        audioUrl = this.audioUrl,
        releaseDate = this.releaseDate
    )
}
