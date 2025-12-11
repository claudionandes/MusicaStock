package ipca.example.musicastock.ui.musics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.domain.models.Music
import ipca.example.musicastock.domain.repository.IMusicRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import ipca.example.musicastock.data.repository.MusicsLocalRepository


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

    fun fetchAllMusics() {
        viewModelScope.launch {
            musicRepository.fetchAllMusics().collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(isLoading = true, error = null)
                    }

                    is ResultWrapper.Success -> {
                        val musics = result.data ?: emptyList()

                        uiState = uiState.copy(
                            isLoading = false,
                            musics = musics,
                            error = null
                        )

                        try {
                            localRepository.clearAll()
                            localRepository.insertMusics(musics)
                        } catch (_: Exception) {
                        }
                    }

                    is ResultWrapper.Error -> {
                        try {
                            val cached = localRepository.getAllMusics()

                            if (cached.isNotEmpty()) {
                                uiState = uiState.copy(
                                    isLoading = false,
                                    musics = cached,
                                    error = "Sem ligação. A mostrar dados offline."
                                )
                            } else {
                                uiState = uiState.copy(
                                    isLoading = false,
                                    error = result.message ?: "Erro ao carregar músicas."
                                )
                            }
                        } catch (e: Exception) {
                            uiState = uiState.copy(
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

                        uiState = uiState.copy(
                            isLoading = false,
                            musics = musics,
                            error = null
                        )

                        try {
                            localRepository.insertMusics(musics)
                        } catch (_: Exception) {
                        }
                    }

                    is ResultWrapper.Error -> {
                        try {
                            val cached = localRepository
                                .getAllMusics()
                                .filter { it.collectionId == collectionId }

                            if (cached.isNotEmpty()) {
                                uiState = uiState.copy(
                                    isLoading = false,
                                    musics = cached,
                                    error = "Sem ligação. A mostrar dados offline."
                                )
                            } else {
                                uiState = uiState.copy(
                                    isLoading = false,
                                    error = result.message
                                        ?: "Erro ao carregar músicas da coletânea."
                                )
                            }
                        } catch (e: Exception) {
                            uiState = uiState.copy(
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
            when (val result = musicRepository.getMusicById(musicId)) {
                is ResultWrapper.Loading ->
                    uiState = uiState.copy(isLoading = true, error = null)

                is ResultWrapper.Success ->
                    uiState = uiState.copy(
                        isLoading = false,
                        selectedMusic = result.data,
                        error = null
                    )

                is ResultWrapper.Error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        selectedMusic = null,
                        error = result.message
                    )
            }
        }
    }

    fun saveMusic(music: Music, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            musicRepository.saveMusic(music).collect { result ->
                when (result) {
                    is ResultWrapper.Loading -> {
                        uiState = uiState.copy(
                            isLoading = true,
                            error = null
                        )
                    }

                    is ResultWrapper.Success -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = null
                        )

                        try {
                            localRepository.insertMusic(music)
                        } catch (_: Exception) { }

                        onSuccess()
                    }

                    is ResultWrapper.Error -> {
                       try {
                            localRepository.insertMusic(music)

                            uiState = uiState.copy(
                                isLoading = false,
                                error = "Sem ligação. Música guardada apenas localmente."
                            )


                            onSuccess()

                        } catch (e: Exception) {
                            uiState = uiState.copy(
                                isLoading = false,
                                error = result.message ?: "Erro ao guardar música."
                            )
                        }
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
                        uiState = uiState.copy(isLoading = false, error = null)

                        val musicToDelete = uiState.musics.find { it.musId == musicId }
                        try {
                            if (musicToDelete != null) {
                                localRepository.deleteMusic(musicToDelete)
                            }
                        } catch (_: Exception) {
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

}