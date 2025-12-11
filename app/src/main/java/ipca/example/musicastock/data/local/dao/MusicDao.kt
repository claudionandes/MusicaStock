package ipca.example.musicastock.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ipca.example.musicastock.domain.models.Music

@Dao
interface MusicDao {

    @Query("SELECT * FROM musics")
    suspend fun getAll(): List<Music>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(music: Music)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(musics: List<Music>)

    @Delete
    suspend fun delete(music: Music)

    @Query("DELETE FROM musics")
    suspend fun deleteAll()
}