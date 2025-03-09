package com.example.kidstutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kidstutor.data.database.TutorDatabase
import com.example.kidstutor.data.model.TutorSession
import com.example.kidstutor.data.repository.TutorRepository
import com.example.kidstutor.ui.screens.HomeScreen
import com.example.kidstutor.ui.screens.SessionScreen
import com.example.kidstutor.ui.theme.KidsTutorTheme
import com.example.kidstutor.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = TutorDatabase.getDatabase(applicationContext)
        val repository = TutorRepository(database.tutorSessionDao(), applicationContext)

        setContent {
            KidsTutorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModel.Factory(repository)
                    )

                    var currentSession by remember { mutableStateOf<TutorSession?>(null) }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onSessionClick = { session ->
                                    currentSession = session
                                    navController.navigate("session/${session.id}")
                                }
                            )
                        }

                        composable(
                            route = "session/{sessionId}",
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                            currentSession?.let { session ->
                                if (session.id == sessionId) {
                                    SessionScreen(
                                        session = session,
                                        viewModel = viewModel,
                                        onBackClick = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}