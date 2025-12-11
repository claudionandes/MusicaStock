package ipca.example.musicastock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ipca.example.musicastock.domain.models.Collection
import ipca.example.musicastock.data.local.dao.CollectionDao
import ipca.example.musicastock.domain.models.Music
import ipca.example.musicastock.data.local.dao.MusicDao

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

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "database-musicstock"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}