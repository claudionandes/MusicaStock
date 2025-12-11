package ipca.example.musicastock.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "collections")
data class Collection(


    @PrimaryKey(autoGenerate = true)
    var localId: Int = 0,
    var id: String? = null,
    var title: String? = null,
    var style: String? = null,

    @Ignore
    var owners: List<String> = emptyList()
) {

    constructor(
        localId: Int,
        id: String?,
        title: String?,
        style: String?
    ) : this(
        localId = localId,
        id = id,
        title = title,
        style = style,
        owners = emptyList()
    )
}
