package ipca.example.musicastock.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "musics")
data class Music(
    @PrimaryKey
    val musId: String = "",

    val musTitle: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val releaseDate: String? = null,
    val audioUrl: String? = null,

    // extras locais
    val musStyle: String? = null,
    val tabUrl: String? = null,

    // pode ser null (música fora de coletânea)
    val collectionId: String? = null
)
