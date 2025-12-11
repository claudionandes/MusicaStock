package ipca.example.musicastock.data.repository

import ipca.example.musicastock.data.local.dao.MusicDao
import ipca.example.musicastock.domain.models.Music
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicsLocalRepository @Inject constructor(
    private val dao: MusicDao
) {

    suspend fun getAllMusics(): List<Music> =
        dao.getAll()

    suspend fun insertMusics(musics: List<Music>) =
        dao.insertAll(musics)

    suspend fun insertMusic(music: Music) =
        dao.insert(music)

    suspend fun deleteMusic(music: Music) =
        dao.delete(music)

    suspend fun clearAll() =
        dao.deleteAll()
}
