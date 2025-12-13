package ipca.example.musicastock.data.repository

import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.remote.api.CollectionsApi
import ipca.example.musicastock.data.remote.dto.MusicCollectionDto
import ipca.example.musicastock.domain.models.Collection
import ipca.example.musicastock.domain.repository.ICollectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val api: CollectionsApi,
    private val local: CollectionsLocalRepository
) : ICollectionRepository {

    // A API ainda não mostra auth; por agora não há “current user” no Android.
    override fun getCurrentUserId(): String? = null
    override fun getCurrentUserEmail(): String? = null

    override fun fetchCollections(): Flow<ResultWrapper<List<Collection>>> = flow {
        emit(ResultWrapper.Loading())

        try {
            val remote = api.getAll().map { it.toDomain() }

            runCatching {
                local.clearAll()
                local.insertCollections(remote)
            }

            emit(ResultWrapper.Success(remote))
        } catch (e: Exception) {
            val cached = runCatching { local.getAllCollections() }.getOrDefault(emptyList())
            if (cached.isNotEmpty()) {
                emit(ResultWrapper.Success(cached))
            } else {
                emit(ResultWrapper.Error(e.message ?: "Erro ao carregar coleções."))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun addCollection(collection: Collection): Flow<ResultWrapper<String>> = flow {
        emit(ResultWrapper.Loading())

        val title = collection.title?.trim()
        if (title.isNullOrBlank()) {
            emit(ResultWrapper.Error("O título é obrigatório."))
            return@flow
        }

        try {
            val created = api.create(
                MusicCollectionDto(
                    title = title,
                    style = collection.style,
                    ownerId = null // ainda sem auth na API
                )
            )

            val saved = created.toDomain()
            runCatching { local.insertCollection(saved) }

            emit(ResultWrapper.Success(saved.id))
        } catch (_: Exception) {
            // OFFLINE: cria ID local para poderes navegar para o detalhe
            val localId = UUID.randomUUID().toString()
            val offline = collection.copy(id = localId)

            runCatching { local.insertCollection(offline) }

            // devolve Success com id (para navegação), mas a UI vai receber aviso via Error?
            // aqui preferimos manter o padrão do teu ViewModel:
            // - ele navega no Success
            // - e no Error ele também navega (no teu código atual navega no Error)
            // Para ficar 100% consistente com o teu ViewModel atual, devolvemos Error.
            // MAS: no teu ViewModel, no Error ele também cria id local e navega.
            // Ou seja, podemos devolver Error e manter o padrão.
            emit(ResultWrapper.Error("Sem ligação. Coletânea guardada apenas localmente."))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteCollection(collectionId: String): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        val apiOk = runCatching {
            val res = api.delete(collectionId)
            res.isSuccessful
        }.getOrDefault(false)

        // remove do Room sempre
        runCatching {
            val all = local.getAllCollections()
            val match = all.firstOrNull { it.id == collectionId }
            if (match != null) local.deleteCollection(match)
        }

        if (apiOk) emit(ResultWrapper.Success(Unit))
        else emit(ResultWrapper.Error("Sem ligação. Coletânea apagada apenas localmente."))
    }.flowOn(Dispatchers.IO)

    override fun updateCollection(
        collectionId: String,
        title: String,
        style: String
    ): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        val titleTrim = title.trim()
        if (titleTrim.isBlank()) {
            emit(ResultWrapper.Error("O título é obrigatório."))
            return@flow
        }

        try {
            // Buscar atual para manter campos que a API possa exigir
            val current = api.getById(collectionId)

            val body = current.copy(
                title = titleTrim,
                style = style
            )

            val res = api.update(collectionId, body)
            if (!res.isSuccessful) {
                emit(ResultWrapper.Error("Erro ao atualizar coleção (${res.code()})."))
                return@flow
            }

            // Atualiza cache local
            runCatching {
                val all = local.getAllCollections()
                val match = all.firstOrNull { it.id == collectionId }
                if (match != null) local.insertCollection(match.copy(title = titleTrim, style = style))
            }

            emit(ResultWrapper.Success(Unit))
        } catch (_: Exception) {
            // OFFLINE: atualiza localmente e avisa
            runCatching {
                val all = local.getAllCollections()
                val match = all.firstOrNull { it.id == collectionId }
                if (match != null) local.insertCollection(match.copy(title = titleTrim, style = style))
            }

            emit(ResultWrapper.Error("Sem ligação. Coletânea atualizada apenas localmente."))
        }
    }.flowOn(Dispatchers.IO)

    // (Opcional mas recomendado) — evita crash caso collectionId venha nulo no JSON
    private fun MusicCollectionDto.toDomain(): Collection =
        Collection(
            id = this.collectionId ?: UUID.randomUUID().toString(),
            title = this.title,
            style = this.style
        )
}
