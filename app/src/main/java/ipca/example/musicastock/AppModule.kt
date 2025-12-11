package ipca.example.musicastock

import android.content.Context
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ipca.example.musicastock.data.local.AppDatabase
import ipca.example.musicastock.data.local.dao.CollectionDao
import ipca.example.musicastock.data.local.dao.MusicDao
import ipca.example.musicastock.data.repository.CollectionRepositoryImpl
import ipca.example.musicastock.data.repository.LoginRepository
import ipca.example.musicastock.data.repository.MusicRepository
import ipca.example.musicastock.domain.repository.ICollectionRepository
import ipca.example.musicastock.domain.repository.ILoginRepository
import ipca.example.musicastock.domain.repository.IMusicRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "local_music_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()

    @Provides
    @Singleton
    fun provideMusicDao(db: AppDatabase): MusicDao = db.musicDao()


    @Provides
    @Singleton
    fun provideCollectionRepository(
        db: FirebaseFirestore,
        auth: FirebaseAuth
    ): ICollectionRepository = CollectionRepositoryImpl(db, auth)

    @Provides
    @Singleton
    fun provideLoginRepository(
        auth: FirebaseAuth,
        db: FirebaseFirestore
    ): ILoginRepository = LoginRepository(auth, db)

    @Provides
    @Singleton
    fun provideMusicRepository(
        db: FirebaseFirestore,
        auth: FirebaseAuth
    ): IMusicRepository = MusicRepository(db, auth)

}

