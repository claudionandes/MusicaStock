package ipca.example.musicastock.domain.models

import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "musics")
data class Music(

    @PrimaryKey(autoGenerate = true)
    var localId: Int = 0,

    var musId: String? = null,
    var musTitle: String? = null,
    var artist: String? = null,
    var album: String? = null,
    var releaseDate: String? = null,
    var musStyle: String? = null,
    var audioUrl: String? = null,
    var tabUrl: String? = null,
    var collectionId: String? = null,
    var ownerId: String? = null
)
