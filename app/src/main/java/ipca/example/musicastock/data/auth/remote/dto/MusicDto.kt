package ipca.example.musicastock.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MusicDto(
    @SerializedName(value = "musicId", alternate = ["MusicId"])
    val musicId: String? = null,

    @SerializedName(value = "title", alternate = ["Title"])
    val title: String,

    @SerializedName(value = "artist", alternate = ["Artist"])
    val artist: String? = null,

    @SerializedName(value = "album", alternate = ["Album"])
    val album: String? = null,

    @SerializedName(value = "audioUrl", alternate = ["AudioUrl"])
    val audioUrl: String? = null,

    // API: "1997-05-20"
    @SerializedName(value = "releaseDate", alternate = ["ReleaseDate"])
    val releaseDate: String? = null
)
