package ipca.example.musicastock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ipca.example.musicastock.data.local.dao.CollectionDao
import ipca.example.musicastock.data.local.dao.MusicDao
import ipca.example.musicastock.domain.models.Collection
import ipca.example.musicastock.domain.models.Music

@Database(
    entities = [
        Music::class,
        Collection::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun musicDao(): MusicDao
    abstract fun collectionDao(): CollectionDao
}
