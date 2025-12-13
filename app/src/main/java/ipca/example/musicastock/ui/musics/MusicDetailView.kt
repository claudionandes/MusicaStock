package ipca.example.musicastock.ui.musics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import ipca.example.musicastock.R
import ipca.example.musicastock.domain.models.Music
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicDetailView(
    navController: NavHostController,
    collectionId: String,
    musicId: String? = null,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var musStyle by remember { mutableStateOf("") }
    var audioUrl by remember { mutableStateOf("") }
    var tabUrl by remember { mutableStateOf("") }

    val safeCollectionId: String? = collectionId
        .trim()
        .takeIf { it.isNotBlank() && it.lowercase() != "null" && it.lowercase() != "none" }

    LaunchedEffect(musicId) {
        if (musicId != null) viewModel.loadMusicById(musicId)
    }

    LaunchedEffect(uiState.selectedMusic) {
        uiState.selectedMusic?.let { music ->
            title = music.musTitle.orEmpty()
            artist = music.artist.orEmpty()
            album = music.album.orEmpty()
            releaseDate = music.releaseDate.orEmpty()
            musStyle = music.musStyle.orEmpty()
            audioUrl = music.audioUrl.orEmpty()
            tabUrl = music.tabUrl.orEmpty()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    fun isValidIsoDate(input: String): Boolean {
        val s = input.trim()
        val regex = Regex("""^\d{4}-\d{2}-\d{2}$""")
        return s.isBlank() || regex.matches(s)
    }

    fun onSaveClick() {
        val titleTrim = title.trim()
        val artistTrim = artist.trim()
        val albumTrim = album.trim()
        val releaseDateTrim = releaseDate.trim()
        val musStyleTrim = musStyle.trim()

        if (titleTrim.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("O título é obrigatório!") }
            return
        }

        if (!isValidIsoDate(releaseDateTrim)) {
            scope.launch { snackbarHostState.showSnackbar("Data inválida. Usa yyyy-MM-dd (ex: 1997-05-20).") }
            return
        }

        // ✅ musId agora é sempre String (não-nulo)
        val finalId = musicId ?: uiState.selectedMusic?.musId ?: UUID.randomUUID().toString()

        val music = Music(
            musId = musicId ?: java.util.UUID.randomUUID().toString(),
            musTitle = titleTrim,
            artist = artistTrim.ifBlank { null },
            album = albumTrim.ifBlank { null },
            releaseDate = releaseDateTrim.ifBlank { null },
            musStyle = musStyleTrim.ifBlank { null },
            audioUrl = audioUrl.trim().ifBlank { null },
            tabUrl = tabUrl.trim().ifBlank { null },
            collectionId = safeCollectionId,
            //isNew = (musicId == null)
        )


        viewModel.saveMusic(music) { navController.popBackStack() }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.img_3),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_51),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )

                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = if (musicId == null) "Nova Música" else "Editar Música",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFFAF512E),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { onSaveClick() },
                        containerColor = Color(0xFFAF512E),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = if (musicId == null) "Guardar Música" else "Atualizar Música"
                        )
                    }
                }
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Álbum") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = releaseDate,
                    onValueChange = { releaseDate = it },
                    label = { Text("Data de Lançamento (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = musStyle,
                    onValueChange = { musStyle = it },
                    label = { Text("Estilo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = audioUrl,
                    onValueChange = { audioUrl = it },
                    label = { Text("URL do Áudio (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tabUrl,
                    onValueChange = { tabUrl = it },
                    label = { Text("URL da Tabelatura (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}
