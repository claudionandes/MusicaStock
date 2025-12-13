package ipca.example.musicastock.domain.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "collections")
data class Collection(

    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),

    var title: String? = null,
    var style: String? = null,

    @Ignore
    var owners: List<String> = emptyList()
)
