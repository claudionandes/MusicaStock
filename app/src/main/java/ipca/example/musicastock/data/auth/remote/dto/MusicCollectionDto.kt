package ipca.example.musicastock.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MusicCollectionDto(
    @SerializedName(value = "collectionId", alternate = ["CollectionId", "id", "Id"])
    val collectionId: String? = null,

    @SerializedName(value = "title", alternate = ["Title"])
    val title: String? = null,

    @SerializedName(value = "style", alternate = ["Style"])
    val style: String? = null,

    // A API pede ownerId no exemplo do POST.
    @SerializedName(value = "ownerId", alternate = ["OwnerId"])
    val ownerId: String? = null
)
