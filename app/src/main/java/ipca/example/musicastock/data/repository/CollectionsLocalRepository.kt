package ipca.example.musicastock.data.repository

import ipca.example.musicastock.data.local.dao.CollectionDao
import ipca.example.musicastock.domain.models.Collection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionsLocalRepository @Inject constructor(
    private val dao: CollectionDao
) {
    suspend fun getAllCollections(): List<Collection> = dao.getAll()

    suspend fun insertCollections(collections: List<Collection>) = dao.insertAll(collections)

    suspend fun insertCollection(collection: Collection) = dao.insert(collection)

    suspend fun deleteCollection(collection: Collection) = dao.delete(collection)

    suspend fun clearAll() = dao.deleteAll()
}
