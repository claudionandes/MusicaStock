package ipca.example.musicastock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint
import ipca.example.musicastock.ui.collection.*
import ipca.example.musicastock.ui.login.LoginView
import ipca.example.musicastock.ui.musics.AllMusicsView
import ipca.example.musicastock.ui.musics.MusicDetailView
import ipca.example.musicastock.ui.theme.MusicastockTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            val navController = rememberNavController()
            val auth = Firebase.auth

            MusicastockTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier.padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {

                        NavHost(
                            navController = navController,
                            startDestination = "login"
                        ) {

                            composable("login") {
                                LoginView(
                                    onLoginSuccess = {
                                        navController.navigate("collections") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("collections") {
                                CollectionView(navController)
                            }

                            composable(
                                route = "collectionDetail/{collectionId}",
                                arguments = listOf(
                                    navArgument("collectionId") { type = NavType.StringType }
                                )
                            ) { entry ->
                                val id = entry.arguments?.getString("collectionId")
                                    ?: return@composable

                                CollectionDetailView(
                                    navController = navController,
                                    collectionId = id
                                )
                            }

                            composable("collectionCreate") {
                                CollectionCreateView(navController)
                            }

                            composable(
                                route = "collectionEdit/{collectionId}",
                                arguments = listOf(
                                    navArgument("collectionId") { type = NavType.StringType }
                                )
                            ) { entry ->
                                val id = entry.arguments?.getString("collectionId")
                                    ?: return@composable

                                CollectionEditView(
                                    navController = navController,
                                    collectionId = id
                                )
                            }

                            composable("allMusics") {
                                AllMusicsView(navController = navController)
                            }


                            composable(
                                route = "musicDetail/{collectionId}",
                                arguments = listOf(
                                    navArgument("collectionId") { type = NavType.StringType }
                                )
                            ) { entry ->
                                val colId = entry.arguments?.getString("collectionId")
                                    ?: return@composable

                                MusicDetailView(
                                    navController = navController,
                                    collectionId = colId,
                                    musicId = null
                                )
                            }

                            composable(
                                route = "musicDetail/{collectionId}/{musicId}",
                                arguments = listOf(
                                    navArgument("collectionId") { type = NavType.StringType },
                                    navArgument("musicId") { type = NavType.StringType }
                                )
                            ) { entry ->
                                val colId = entry.arguments?.getString("collectionId")
                                    ?: return@composable

                                val musId = entry.arguments?.getString("musicId")

                                MusicDetailView(
                                    navController = navController,
                                    collectionId = colId,
                                    musicId = musId
                                )
                            }
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (auth.currentUser != null) {
                    navController.navigate("collections") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        }
    }
}
