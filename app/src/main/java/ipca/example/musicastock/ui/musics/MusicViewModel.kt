package ipca.example.musicastock.ui.musics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.repository.MusicsLocalRepository
import ipca.example.musicastock.domain.models.Music
import ipca.example.musicastock.domain.repository.IMusicRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicState(
    val musics: List<Music> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedMusic: Music? = null
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val musicRepository: IMusicRepository,
    private val localRepository: MusicsLocalRepository
) : ViewModel() {

    var uiState by mutableStateOf(MusicState())
        private set

    fun fetchAllMusics() {
        viewModelScope.launch {
            musicRepository.fetchAllMusics().collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        val musics = result.data ?: emptyList()
                        uiState = uiState.copy(isLoading = false, musics = musics, error = null)

                        runCatching {
                            localRepository.clearAll()
                            localRepository.insertMusics(musics)
                        }
                    }

                    is ResultWrapper.Error -> {
                        val cached = runCatching { localRepository.getAllMusics() }
                            .getOrDefault(emptyList())

                        uiState = if (cached.isNotEmpty()) {
                            uiState.copy(
                                isLoading = false,
                                musics = cached,
                                error = "Sem ligação. A mostrar dados offline."
                            )
                        } else {
                            uiState.copy(
                                isLoading = false,
                                error = result.message ?: "Erro ao carregar músicas."
                            )
                        }
                    }
                }
            }
        }
    }

    fun fetchMusicsByCollection(collectionId: String) {
        viewModelScope.launch {
            musicRepository.fetchMusicsByCollection(collectionId).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        val musics = result.data ?: emptyList()
                        uiState = uiState.copy(isLoading = false, musics = musics, error = null)

                        // cache local (p/ offline)
                        runCatching { localRepository.insertMusics(musics) }
                    }

                    is ResultWrapper.Error -> {
                        val cached = runCatching { localRepository.getAllMusics() }
                            .getOrDefault(emptyList())
                            .filter { it.collectionId == collectionId }

                        uiState = if (cached.isNotEmpty()) {
                            uiState.copy(
                                isLoading = false,
                                musics = cached,
                                error = "Sem ligação. A mostrar dados offline."
                            )
                        } else {
                            uiState.copy(
                                isLoading = false,
                                error = result.message ?: "Erro ao carregar músicas da coletânea."
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMusicById(musicId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, selectedMusic = null)

            when (val result = musicRepository.getMusicById(musicId)) {
                is ResultWrapper.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        selectedMusic = result.data,
                        error = null
                    )
                }

                is ResultWrapper.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        selectedMusic = null,
                        error = result.message ?: "Erro ao carregar música."
                    )
                }

                is ResultWrapper.Loading -> {
                    uiState = uiState.copy(isLoading = true, error = null)
                }
            }
        }
    }

    fun saveMusic(music: Music, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            musicRepository.saveMusic(music).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        uiState = uiState.copy(isLoading = false, error = null)

                        // IMPORTANTE:
                        // Não inserir aqui "music" diretamente no Room, porque ao criar online
                        // o ID vem do servidor e o repository é que deve cachear a versão correta.
                        // Faz refresh da lista para refletir o ID correto.
                        val colId = music.collectionId
                        if (!colId.isNullOrBlank()) fetchMusicsByCollection(colId) else fetchAllMusics()

                        onSuccess()
                    }

                    is ResultWrapper.Error -> {
                        // offline-friendly: o repository pode já ter guardado (ou tenta-se aqui)
                        runCatching { localRepository.insertMusic(music) }

                        uiState = uiState.copy(
                            isLoading = false,
                            error = "Sem ligação. Música guardada apenas localmente."
                        )

                        val colId = music.collectionId
                        if (!colId.isNullOrBlank()) fetchMusicsByCollection(colId) else fetchAllMusics()

                        onSuccess()
                    }
                }
            }
        }
    }

    fun deleteMusic(musicId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            musicRepository.deleteMusic(musicId).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = null,
                            musics = uiState.musics.filterNot { it.musId == musicId }
                        )

                        runCatching {
                            val all = localRepository.getAllMusics()
                            val match = all.firstOrNull { it.musId == musicId }
                            if (match != null) localRepository.deleteMusic(match)
                        }

                        onSuccess()
                    }

                    is ResultWrapper.Error -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = result.message ?: "Erro ao apagar música."
                        )
                    }
                }
            }
        }
    }

    fun removeMusicFromCollection(collectionId: String, musicId: String) {
        viewModelScope.launch {
            musicRepository.removeMusicFromCollection(collectionId, musicId).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = null,
                            musics = uiState.musics.filterNot { it.musId == musicId }
                        )

                        // refresh para garantir consistência com Room+API
                        fetchMusicsByCollection(collectionId)
                    }

                    is ResultWrapper.Error -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = result.message ?: "Erro ao remover música da coletânea."
                        )
                    }
                }
            }
        }
    }
}
