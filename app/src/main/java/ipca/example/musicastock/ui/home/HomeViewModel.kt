package ipca.example.musicastock.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.data.remote.api.EnvironmentsApi
import ipca.example.musicastock.data.remote.dto.EnvironmentStatusDto
import ipca.example.musicastock.domain.models.Collection
import ipca.example.musicastock.domain.repository.ICollectionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

enum class RecommendationMode {
    NONE, WEATHER, SENSORS
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    val environmentName: String = "",
    val city: String = "",
    val externalWeatherDescription: String = "",
    val externalTempRange: String = "",
    val externalPrecipitation: String = "",

    val internalTemperature: String? = null,
    val internalHumidity: String? = null,
    val internalLight: String? = null,

    val selectedMode: RecommendationMode = RecommendationMode.NONE,
    val weatherBasedCollections: List<Collection> = emptyList(),
    val sensorBasedCollections: List<Collection> = emptyList()
)

/**
 * Eventos one-shot da Home (para navegação, etc.).
 */
sealed class HomeEvent {
    object NavigateToAllCollections : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val environmentsApi: EnvironmentsApi,
    private val collectionRepository: ICollectionRepository
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    /**
     * Carrega o estado inicial da Home:
     * - estado do ambiente (meteo + sensores)
     * - lista de coletâneas disponíveis
     */
    fun load(environmentId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            try {
                // 1) Obter estado do ambiente (IPMA + sensores)
                val status: EnvironmentStatusDto =
                    environmentsApi.getEnvironmentStatus(environmentId)

                // 2) Obter coleções (podes trocar para get by owner, se quiseres)
                val collectionsResult = collectionRepository
                    .fetchCollections()
                    .first { it !is ResultWrapper.Loading }


                // Forçar tipo não-nulo para evitar problemas de inferência
                val collections: List<Collection> = when (collectionsResult) {
                    is ResultWrapper.Success -> collectionsResult.data ?: emptyList()
                    is ResultWrapper.Error -> emptyList()
                    is ResultWrapper.Loading -> emptyList()
                }

                // 3) Calcular sugestões
                val weatherBased = suggestByWeather(status, collections)
                val sensorBased = suggestBySensors(status, collections)

                uiState = uiState.copy(
                    isLoading = false,
                    error = null,

                    environmentName = status.environment.name,
                    city = status.environment.city.orEmpty(),

                    externalWeatherDescription =
                        status.externalWeather?.weatherDescription.orEmpty(),

                    externalTempRange = status.externalWeather?.let { weather ->
                        val min = weather.minTemperatureC
                        val max = weather.maxTemperatureC
                        when {
                            min != null && max != null -> "${min.roundToInt()}ºC - ${max.roundToInt()}ºC"
                            min != null -> "${min.roundToInt()}ºC"
                            max != null -> "${max.roundToInt()}ºC"
                            else -> ""
                        }
                    } ?: "",

                    externalPrecipitation = status.externalWeather
                        ?.precipitationProbability
                        ?.let { "${it.roundToInt()}%" }
                        ?: "",

                    internalTemperature = status.lastReading?.temperature?.let { "$it ºC" },
                    internalHumidity = status.lastReading?.humidity?.let { "$it %" },
                    internalLight = status.lastReading?.light?.let { "$it" },

                    weatherBasedCollections = weatherBased,
                    sensorBasedCollections = sensorBased
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Erro ao carregar o estado do ambiente. Verifique a ligação e tente novamente."
                )
            }
        }
    }

    fun selectWeatherMode() {
        uiState = uiState.copy(selectedMode = RecommendationMode.WEATHER)
    }

    fun selectSensorsMode() {
        uiState = uiState.copy(selectedMode = RecommendationMode.SENSORS)
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    /**
     * Chamado quando o utilizador carrega no botão
     * "Ver todas as coletâneas" na Home.
     * A composable observa [events] e faz a navegação.
     */
    fun onViewAllCollectionsClicked() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToAllCollections)
        }
    }

    // --------- Lógica de sugestões ---------


    private enum class Mood { CALM, FOCUS, PARTY, TRAINING, SUMMER, OUTDOOR }

    private fun moodsForCollection(c: Collection): Set<Mood> {
        val style = c.style.orEmpty().trim().lowercase()
        val title = c.title.orEmpty().trim().lowercase()

        val moods = mutableSetOf<Mood>()

        // 1) Mapeamento principal por STYLE (baseado nas tuas coleções reais)
        when {
            style == "acoustic" -> moods += setOf(Mood.CALM, Mood.OUTDOOR)
            style.contains("lo-fi") || style.contains("lofi") -> moods += setOf(Mood.FOCUS, Mood.CALM)
            style == "soundtrack" -> moods += setOf(Mood.FOCUS, Mood.CALM)
            style == "classical" -> moods += setOf(Mood.FOCUS, Mood.CALM)

            style.contains("rock") -> moods += setOf(Mood.PARTY, Mood.TRAINING)
            style == "reggae" -> moods += setOf(Mood.SUMMER, Mood.OUTDOOR, Mood.CALM)
            style == "pop" -> moods += setOf(Mood.PARTY, Mood.SUMMER)
            style == "latino" -> moods += setOf(Mood.PARTY, Mood.SUMMER)
            style == "pimba" -> moods += setOf(Mood.PARTY)

            style == "electronic" -> moods += setOf(Mood.TRAINING, Mood.PARTY)
            style == "hip-hop" || style == "hip hop" -> moods += setOf(Mood.TRAINING, Mood.PARTY)
            style == "metal" -> moods += setOf(Mood.TRAINING, Mood.PARTY)
        }

        // 2) Bónus por palavras no TÍTULO (mais robusto)
        if (title.contains("study") || title.contains("focus") || title.contains("coding")) moods += Mood.FOCUS
        if (title.contains("chill") || title.contains("relax") || title.contains("sunset")) moods += Mood.CALM
        if (title.contains("running") || title.contains("lift") || title.contains("treino")) moods += Mood.TRAINING
        if (title.contains("summer") || title.contains("sunny") || title.contains("heat")) moods += Mood.SUMMER
        if (title.contains("festa") || title.contains("aldeia")) moods += Mood.PARTY

        return moods
    }

    private fun moodsForEnvironmentName(name: String): Set<Mood> {
        val n = name.lowercase()
        val moods = mutableSetOf<Mood>()

        if (n.contains("crossfit") || n.contains("ginásio") || n.contains("ginasio") || n.contains("pista") || n.contains("atlet")) {
            moods += Mood.TRAINING
        }
        if (n.contains("biblioteca") || n.contains("office") || n.contains("escrit")) {
            moods += Mood.FOCUS
        }
        if (n.contains("praia") || n.contains("parque") || n.contains("passeio") || n.contains("miradouro")) {
            moods += setOf(Mood.OUTDOOR, Mood.SUMMER)
        }
        if (n.contains("churrasco") || n.contains("jantar") || n.contains("bar")) {
            moods += Mood.PARTY
        }

        return moods
    }

    private fun pickRecommendations(
        collections: List<Collection>,
        linkedCollectionId: String?,
        preferred: List<Mood>,
        envMoods: Set<Mood>,
        modeSalt: String,                 // "WEATHER" ou "SENSORS"
        limit: Int = 5,
        linkedBoost: Int = 10,            // boost pequeno (não “força”)
        forceLinkedFirst: Boolean = false // se quiser forçar, põe true
    ): List<Collection> {

        val linked = linkedCollectionId?.let { id ->
            collections.firstOrNull { it.id == id }
        }

        val scored = collections.map { c ->
            val moods = moodsForCollection(c)
            var score = 0

            // Preferências do modo (meteorologia/sensores) -> dá mais peso
            preferred.forEachIndexed { idx, m ->
                if (moods.contains(m)) {
                    score += (18 - idx * 3).coerceAtLeast(3)
                }
            }

            // Peso do tipo de ambiente (ginásio, praia, escritório, etc.) -> menos peso
            envMoods.forEach { m ->
                if (moods.contains(m)) score += 3
            }

            // Boost leve para a linked (não cola sempre no topo)
            if (linked != null && c.id == linked.id) score += linkedBoost

            // Desempate determinístico diferente por modo
            val tieBreak = kotlin.math.abs((modeSalt + c.id).hashCode())

            Triple(c, score, tieBreak)
        }

        val sorted = scored
            .sortedWith(
                compareByDescending<Triple<Collection, Int, Int>> { it.second }
                    .thenBy { it.third }
            )
            .map { it.first }

        return if (forceLinkedFirst && linked != null) {
            listOf(linked) + sorted.filter { it.id != linked.id }.take((limit - 1).coerceAtLeast(0))
        } else {
            sorted.take(limit)
        }
    }


    private fun suggestByWeather(
        status: EnvironmentStatusDto,
        collections: List<Collection>
    ): List<Collection> {
        val envName = status.environment.name
        val envMoods = moodsForEnvironmentName(envName)

        // Coleção principal associada ao ambiente (vem do backend)
        val linkedId = status.environment.linkedCollectionId

        val max = status.externalWeather?.maxTemperatureC
        val precip = status.externalWeather?.precipitationProbability
        val desc = status.externalWeather?.weatherDescription?.lowercase().orEmpty()

        val rainy = (precip != null && precip >= 60) ||
                desc.contains("chuva") || desc.contains("rain") || desc.contains("aguace")

        val preferred = when {
            rainy -> listOf(Mood.FOCUS, Mood.CALM)
            max != null && max <= 14.0 -> listOf(Mood.CALM, Mood.FOCUS)
            max != null && max >= 26.0 -> listOf(Mood.SUMMER, Mood.PARTY, Mood.OUTDOOR, Mood.TRAINING)
            else -> listOf(Mood.OUTDOOR, Mood.CALM, Mood.FOCUS, Mood.PARTY)
        }

        return pickRecommendations(
            collections = collections,
            linkedCollectionId = linkedId,
            preferred = preferred,
            envMoods = envMoods,
            modeSalt = "WEATHER",
            limit = 5,
            forceLinkedFirst = false
        )


    }

    private fun suggestBySensors(
        status: EnvironmentStatusDto,
        collections: List<Collection>
    ): List<Collection> {
        val envName = status.environment.name
        val envMoods = moodsForEnvironmentName(envName)
        val linkedId = status.environment.linkedCollectionId

        val r = status.lastReading
        val temp = r?.temperature
        val hum = r?.humidity
        val light = r?.light

        val dark = light != null && light < 200
        val bright = light != null && light > 600
        val warm = temp != null && temp >= 25
        val comfortable = temp != null && temp in 19.0..24.0
        val veryHumid = hum != null && hum >= 70

        val preferred = when {
            dark -> listOf(Mood.CALM, Mood.FOCUS)
            comfortable && light != null && light in 200.0..600.0 -> listOf(Mood.FOCUS, Mood.CALM)
            warm || bright || veryHumid -> listOf(Mood.TRAINING, Mood.PARTY, Mood.SUMMER)
            else -> listOf(Mood.CALM, Mood.FOCUS, Mood.PARTY)
        }

        return pickRecommendations(
            collections = collections,
            linkedCollectionId = linkedId,
            preferred = preferred,
            envMoods = envMoods,
            modeSalt = "SENSORS",
            limit = 5,
            forceLinkedFirst = false
        )

    }

}
