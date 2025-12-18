
package ipca.example.musicastock.data.remote.dto

data class EnvironmentDto(
    val environmentId: String,
    val name: String,
    val description: String?,
    val city: String?,
    val linkedCollectionId: String?
)

data class CreateEnvironmentRequestDto(
    val name: String,
    val description: String?,
    val city: String,
    val linkedCollectionId: String?
)
