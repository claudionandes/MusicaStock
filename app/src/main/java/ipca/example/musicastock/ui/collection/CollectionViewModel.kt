package ipca.example.musicastock.ui.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.identity.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.repository.CollectionsLocalRepository
import ipca.example.musicastock.domain.models.Collection
import ipca.example.musicastock.domain.repository.ICollectionRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionState(
    val collections: List<Collection> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false,
    val userEmail: String = "Utilizador desconhecido"
)
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: ICollectionRepository,
    private val localRepository: CollectionsLocalRepository
) : ViewModel() {

    var uiState by mutableStateOf(
        CollectionState(
            userEmail = collectionRepository.getCurrentUserEmail() ?: "Utilizador desconhecido"
        )
    )

    fun fetchCollections() {
        viewModelScope.launch {
            collectionRepository.fetchCollections().collect { result ->
                when (result) {

                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(
                            isLoading = true,
                            error = null
                        )
                    }

                    is ResultWrapper.Success -> {
                        val collections = result.data ?: emptyList()

                        uiState = uiState.copy(
                            collections = collections,
                            isLoading = false,
                            error = null
                        )

                        try {
                            localRepository.clearAll()
                            localRepository.insertCollections(collections)
                        } catch (_: Exception) {}
                    }

                    is ResultWrapper.Error -> {
                        try {
                            val cached = localRepository.getAllCollections()

                            if (cached.isNotEmpty()) {
                                uiState = uiState.copy(
                                    collections = cached,
                                    isLoading = false,
                                    error = "Sem ligação. A mostrar dados offline."
                                )
                            } else {
                                uiState = uiState.copy(
                                    isLoading = false,
                                    error = result.message ?: "Erro ao carregar coleções."
                                )
                            }
                        } catch (e: Exception) {
                            uiState = uiState.copy(
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
        val currentUserId = collectionRepository.getCurrentUserId()
        if (currentUserId == null) {
            uiState = uiState.copy(error = "Utilizador não autenticado.")
            return
        }

        val baseCollection = Collection(
            id = null,
            title = title,
            style = style,
            owners = listOf(currentUserId)
        )

        viewModelScope.launch {
            collectionRepository.addCollection(baseCollection).collect { result ->
                when (result) {

                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {

                        val newId = result.data ?: return@collect

                        val finalCollection = baseCollection.copy(id = newId)

                        try {
                            localRepository.insertCollection(finalCollection)
                        } catch (_: Exception) {}

                        uiState = uiState.copy(
                            collections = uiState.collections + finalCollection,
                            isLoading = false,
                            error = null
                        )

                        onSuccess(newId)
                    }

                    is ResultWrapper.Error -> {
                        try {

                            val localId = UUID.randomUUID().toString()

                            val localCollection = baseCollection.copy(id = localId)

                            localRepository.insertCollection(localCollection)

                            uiState = uiState.copy(
                                collections = uiState.collections + localCollection,
                                isLoading = false,
                                error = "Sem ligação. Coletânea guardada apenas localmente."
                            )


                            onSuccess(localId)

                        } catch (e: Exception) {
                            uiState = uiState.copy(
                                isLoading = false,
                                error = "Erro ao guardar coletânea: ${e.message}"
                            )
                        }
                    }
                }
            }
        }
    }


    fun deleteCollection(id: String) {
        viewModelScope.launch {
            collectionRepository.deleteCollection(id).collect { result ->
                when (result) {
                    is ResultWrapper.Error -> {
                        uiState = uiState.copy(error = result.message)
                    }
                    is ResultWrapper.Success -> {
                        val updated = uiState.collections.filterNot { it.id == id }
                        uiState = uiState.copy(collections = updated)
                    }
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }
                }
            }
        }
    }

    fun updateCollection(id: String, title: String, style: String) {
        viewModelScope.launch {
            collectionRepository.updateCollection(id, title, style).collect { result ->
                when (result) {

                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(
                            isLoading = true,
                            error = null
                        )
                    }

                    is ResultWrapper.Error -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }

                    is ResultWrapper.Success -> {
                        val updatedList = uiState.collections.map { collection ->
                            if (collection.id == id)
                                collection.copy(title = title, style = style)
                            else collection
                        }

                        uiState = uiState.copy(
                            collections = updatedList,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        }
    }
}
