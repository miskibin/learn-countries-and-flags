package com.miskibin.poznajswiat

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miskibin.poznajswiat.data.AppData
import com.miskibin.poznajswiat.data.Progress
import com.miskibin.poznajswiat.data.ProgressRepo
import com.miskibin.poznajswiat.data.QuizMode
import com.miskibin.poznajswiat.ui.history.TimelineScreen
import com.miskibin.poznajswiat.ui.home.HomeScreen
import com.miskibin.poznajswiat.ui.learn.CountryDetailScreen
import com.miskibin.poznajswiat.ui.learn.LearnScreen
import com.miskibin.poznajswiat.ui.quiz.QuizScreen
import com.miskibin.poznajswiat.ui.quiz.QuizViewModel
import com.miskibin.poznajswiat.ui.theme.PoznajSwiatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoznajSwiatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNav()
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val dataState = produceState<AppData?>(initialValue = null) {
        value = AppData.get(context)
    }
    val progress by ProgressRepo.get(context).progress
        .collectAsState(initial = Progress())

    val data = dataState.value
    if (data == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var continent by rememberSaveable { mutableStateOf<String?>(null) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                data = data,
                progress = progress,
                continent = continent,
                onContinentChange = { continent = it },
                onStartSession = { session ->
                    navController.navigate("quiz/$session?c=${Uri.encode(continent ?: "")}")
                },
                onOpenLearn = {
                    navController.navigate("learn?c=${Uri.encode(continent ?: "")}")
                },
                onOpenTimeline = { navController.navigate("timeline") },
                modifier = Modifier.safeDrawingPadding(),
            )
        }
        composable(
            route = "quiz/{session}?c={c}",
            arguments = listOf(
                navArgument("session") { type = NavType.StringType },
                navArgument("c") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val session = entry.arguments?.getString("session") ?: QuizMode.FLAGS.id
            val cont = entry.arguments?.getString("c")?.takeIf { it.isNotEmpty() }
            val app = context.applicationContext as Application
            val vm: QuizViewModel = viewModel(
                key = "quiz-$session-$cont",
                factory = QuizViewModel.Factory(app, session, cont),
            )
            QuizScreen(viewModel = vm, onExit = { navController.popBackStack() })
        }
        composable("timeline") {
            TimelineScreen(
                data = data,
                onOpenCountry = { cca2 -> navController.navigate("country/$cca2") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "learn?c={c}",
            arguments = listOf(
                navArgument("c") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val cont = entry.arguments?.getString("c")?.takeIf { it.isNotEmpty() }
            LearnScreen(
                data = data,
                progress = progress,
                initialContinent = cont,
                onOpenCountry = { cca2 -> navController.navigate("country/$cca2") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "country/{cca2}",
            arguments = listOf(navArgument("cca2") { type = NavType.StringType }),
        ) { entry ->
            CountryDetailScreen(
                data = data,
                progress = progress,
                cca2 = entry.arguments?.getString("cca2") ?: "",
                onOpenCountry = { cca2 -> navController.navigate("country/$cca2") },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
