package ipca.example.musicastock.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.snapshotFlow
import ipca.example.musicastock.domain.models.Collection
import ipca.example.musicastock.domain.repository.ICollectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ICollectionRepository {

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun getCurrentUserEmail(): String? = auth.currentUser?.email

    override fun fetchCollections(): Flow<ResultWrapper<List<Collection>>> = flow {
        emit(ResultWrapper.Loading())

        val uid = getCurrentUserId()
        if (uid == null) {
            emit(ResultWrapper.Error("Utilizador não autenticado."))
            return@flow
        }

        db.collection("collections")
            .whereArrayContains("owners", uid)
            .snapshotFlow()
            .collect { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Collection::class.java)?.apply { id = doc.id }
                }

                emit(ResultWrapper.Success(list))
            }

    }.flowOn(Dispatchers.IO)

    override fun addCollection(collection: Collection): Flow<ResultWrapper<String>> = flow {
        emit(ResultWrapper.Loading())

        try {
            val documentRef = db.collection("collections")
                .add(collection)
                .await()

            emit(ResultWrapper.Success(documentRef.id))

        } catch (e: Exception) {
            emit(ResultWrapper.Error(e.message ?: "Erro ao criar coleção"))
        }

    }.flowOn(Dispatchers.IO)

    override fun deleteCollection(collectionId: String): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        try {
            db.collection("collections")
                .document(collectionId)
                .delete()
                .await()

            emit(ResultWrapper.Success(Unit))
        } catch (e: Exception) {
            emit(ResultWrapper.Error(e.message ?: "Erro ao apagar coleção"))
        }

    }.flowOn(Dispatchers.IO)

    override fun updateCollection(
        collectionId: String,
        title: String,
        style: String
    ): Flow<ResultWrapper<Unit>> = flow {
        emit(ResultWrapper.Loading())

        try {
            db.collection("collections")
                .document(collectionId)
                .update("title", title, "style", style)
                .await()

            emit(ResultWrapper.Success(Unit))
        } catch (e: Exception) {
            emit(ResultWrapper.Error(e.message ?: "Erro ao atualizar coleção"))
        }

    }.flowOn(Dispatchers.IO)

}
