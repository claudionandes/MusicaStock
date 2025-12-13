package ipca.example.musicastock.domain.repository

import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.domain.models.Collection
import kotlinx.coroutines.flow.Flow

interface ICollectionRepository {

    fun fetchCollections(): Flow<ResultWrapper<List<Collection>>>

    fun addCollection(collection: Collection): Flow<ResultWrapper<String>>

    fun deleteCollection(collectionId: String): Flow<ResultWrapper<Unit>>

    fun updateCollection(
        collectionId: String,
        title: String,
        style: String
    ): Flow<ResultWrapper<Unit>>

    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
}
