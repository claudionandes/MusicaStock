package ipca.example.musicastock.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.snapshotFlow
import ipca.example.musicastock.domain.models.Music
import ipca.example.musicastock.domain.repository.IMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MusicRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : IMusicRepository {

    override fun fetchAllMusics(): Flow<ResultWrapper<List<Music>>> =
        fetchMusics(collectionId = null)

    override fun fetchMusicsByCollection(collectionId: String): Flow<ResultWrapper<List<Music>>> =
        fetchMusics(collectionId)

    override fun fetchMusics(collectionId: String?): Flow<ResultWrapper<List<Music>>> = flow {
        emit(ResultWrapper.Loading())

        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Utilizador não autenticado")

        var query = db.collection("musics")
            .whereEqualTo("ownerId", uid)

        if (!collectionId.isNullOrBlank()) {
            query = query.whereEqualTo("collectionId", collectionId)
        }

        query.snapshotFlow().collect { snap ->
            val list = snap.documents.mapNotNull { doc ->
                doc.toObject(Music::class.java)?.apply { musId = doc.id }
            }

            emit(ResultWrapper.Success(list))
        }

    }.flowOn(Dispatchers.IO)

    override suspend fun getMusicById(id: String): ResultWrapper<Music?> {
        return try {
            val doc = db.collection("musics")
                .document(id)
                .get()
                .await()

            val music = doc.toObject(Music::class.java)?.apply { musId = doc.id }
            ResultWrapper.Success(music)
        } catch (e: Exception) {
            ResultWrapper.Error(e.message ?: "Erro ao carregar música.")
        }
    }

    override fun saveMusic(music: Music): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Utilizador não autenticado")

        val musicWithOwner = music.copy(ownerId = music.ownerId ?: uid)

        if (musicWithOwner.musId == null) {
            db.collection("musics")
                .add(musicWithOwner)
                .await()
        } else {
            db.collection("musics")
                .document(musicWithOwner.musId!!)
                .set(musicWithOwner)
                .await()
        }

        emit(ResultWrapper.Success(Unit))

    }.flowOn(Dispatchers.IO)

    override fun deleteMusic(id: String): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        db.collection("musics")
            .document(id)
            .delete()
            .await()

        emit(ResultWrapper.Success(Unit))

    }.flowOn(Dispatchers.IO)


}
