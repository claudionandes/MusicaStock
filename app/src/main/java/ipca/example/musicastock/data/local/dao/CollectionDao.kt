package ipca.example.musicastock.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ipca.example.musicastock.domain.models.Collection

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections")
    suspend fun getAll(): List<Collection>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(collection: Collection)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(collections: List<Collection>)

    @Delete
    suspend fun delete(collection: Collection)

    @Query("DELETE FROM collections")

    suspend fun deleteAll()

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Collection?

}