package ipca.example.musicastock.ui.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.repository.CollectionsLocalRepository
import ipca.example.musicastock.domain.models.Collection
import ipca.example.musicastock.domain.repository.ICollectionRepository
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CollectionState(
    val collections: List<Collection> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false,
    // Mantido só para não partir UI existente (pode ser removido depois)
    val userEmail: String = "Jukebox API"
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: ICollectionRepository,
    private val localRepository: CollectionsLocalRepository
) : ViewModel() {

    var uiState by mutableStateOf(CollectionState())
        private set

    fun fetchCollections() {
        viewModelScope.launch {
            collectionRepository.fetchCollections().collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        val collections = result.data ?: emptyList()

                        uiState = uiState.copy(
                            collections = collections,
                            isLoading = false,
                            error = null
                        )

                        // Cache local (redundante se o repo já o fizer, mas não quebra)
                        runCatching {
                            localRepository.clearAll()
                            localRepository.insertCollections(collections)
                        }
                    }

                    is ResultWrapper.Error -> {
                        val cached = runCatching { localRepository.getAllCollections() }
                            .getOrDefault(emptyList())

                        uiState = if (cached.isNotEmpty()) {
                            uiState.copy(
                                collections = cached,
                                isLoading = false,
                                error = "Sem ligação. A mostrar dados offline."
                            )
                        } else {
                            uiState.copy(
                                isLoading = false,
                                error = result.message ?: "Erro ao carregar coleções."
                            )
                        }
                    }
                }
            }
        }
    }

    fun addCollection(title: String, style: String, onSuccess: (String) -> Unit) {
        val titleTrim = title.trim()
        val styleTrim = style.trim()

        if (titleTrim.isBlank()) {
            uiState = uiState.copy(error = "O título é obrigatório.")
            return
        }


        val baseCollection = Collection(
            title = titleTrim,
            style = styleTrim.ifBlank { null }
        )

        viewModelScope.launch {
            collectionRepository.addCollection(baseCollection).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        val newId = result.data
                        if (newId.isNullOrBlank()) {
                            uiState = uiState.copy(isLoading = false, error = "Erro: ID da coleção inválido.")
                            return@collect
                        }

                        val finalCollection = baseCollection.copy(id = newId)

                        runCatching { localRepository.insertCollection(finalCollection) }

                        uiState = uiState.copy(
                            collections = uiState.collections + finalCollection,
                            isLoading = false,
                            error = null
                        )

                        onSuccess(newId)
                    }

                    is ResultWrapper.Error -> {
                        // Offline-friendly: cria ID local e navega na mesma
                        val localId = UUID.randomUUID().toString()
                        val localCollection = baseCollection.copy(id = localId)

                        val saved = runCatching { localRepository.insertCollection(localCollection) }.isSuccess

                        uiState = uiState.copy(
                            collections = uiState.collections + localCollection,
                            isLoading = false,
                            error = if (saved) {
                                "Sem ligação. Coletânea guardada apenas localmente."
                            } else {
                                result.message ?: "Erro ao guardar coletânea."
                            }
                        )

                        onSuccess(localId)
                    }
                }
            }
        }
    }

    fun deleteCollection(id: String) {
        viewModelScope.launch {
            collectionRepository.deleteCollection(id).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        val updated = uiState.collections.filterNot { it.id == id }
                        uiState = uiState.copy(
                            collections = updated,
                            isLoading = false,
                            error = null
                        )
                    }

                    is ResultWrapper.Error -> {
                        // O repository pode já ter apagado localmente e devolver erro só para avisar "sem ligação".
                        val updated = uiState.collections.filterNot { it.id == id }
                        uiState = uiState.copy(
                            collections = updated,
                            isLoading = false,
                            error = result.message ?: "Sem ligação. Coletânea apagada apenas localmente."
                        )
                    }
                }
            }
        }
    }

    fun updateCollection(id: String, title: String, style: String) {
        val titleTrim = title.trim()
        val styleTrim = style.trim()

        if (titleTrim.isBlank()) {
            uiState = uiState.copy(error = "O título é obrigatório.")
            return
        }

        viewModelScope.launch {
            collectionRepository.updateCollection(id, titleTrim, styleTrim).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        val updatedList = uiState.collections.map { c ->
                            if (c.id == id) c.copy(title = titleTrim, style = styleTrim.ifBlank { null })
                            else c
                        }

                        uiState = uiState.copy(
                            collections = updatedList,
                            isLoading = false,
                            error = null
                        )
                    }

                    is ResultWrapper.Error -> {
                        // O repository pode ter atualizado localmente e devolver erro só para avisar "sem ligação".
                        val updatedList = uiState.collections.map { c ->
                            if (c.id == id) c.copy(title = titleTrim, style = styleTrim.ifBlank { null })
                            else c
                        }

                        uiState = uiState.copy(
                            collections = updatedList,
                            isLoading = false,
                            error = result.message ?: "Sem ligação. Coletânea atualizada apenas localmente."
                        )
                    }
                }
            }
        }
    }
}
