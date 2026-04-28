package uz.angrykitten.shiftball

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.angrykitten.shiftball.ui.theme.ShiftBallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftBallTheme {
                VoidFallApp()
            }
        }
    }

    override fun onPause()  { super.onPause();  MusicManager.pause()  }
    override fun onResume() { super.onResume(); MusicManager.resume() }
    override fun onDestroy() { super.onDestroy(); MusicManager.stop() }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun VoidFallApp() {
    val navController     = rememberNavController()
    val gameViewModel     : GameViewModel     = viewModel()
    val settingsViewModel : SettingsViewModel = viewModel()

    // Collect current theme and provide it to the entire composition tree
    val themeEnum by settingsViewModel.theme.collectAsStateWithLifecycle()
    val currentTheme = themeByEnum(themeEnum)

    // Drive music from the current back-stack destination
    val context = androidx.compose.ui.platform.LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val soundOn by settingsViewModel.soundEffects.collectAsStateWithLifecycle()

    LaunchedEffect(currentRoute, soundOn) {
        MusicManager.setSoundEnabled(soundOn)
        val route = currentRoute ?: return@LaunchedEffect
        // Always (re-)play the right track so toggling sound back on restarts music
        when {
            route == "menu"              -> MusicManager.play(context, MusicManager.Track.MENU)
            route == "splash"            -> MusicManager.play(context, MusicManager.Track.MENU)
            route == "game"              -> MusicManager.play(context, MusicManager.Track.GAME)
            route == "settings"          -> Unit  // keep whatever is already playing
            route.startsWith("gameover") -> MusicManager.play(context, MusicManager.Track.LOST)
        }
    }

    CompositionLocalProvider(LocalVoidFallTheme provides currentTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = currentTheme.background
        ) {
            NavHost(
                navController    = navController,
                startDestination = "splash"
            ) {
                // Splash screen — reads theme from CompositionLocal automatically
                composable("splash") {
                    SplashScreen(onDone = {
                        navController.navigate("menu") {
                            popUpTo("splash") { inclusive = true }
                        }
                    })
                }

                composable("menu") {
                    MenuScreen(
                        onPlay = {
                            gameViewModel.syncSettings(
                                settingsViewModel.ballColorIdx.value,
                                settingsViewModel.difficulty.value,
                                currentTheme
                            )
                            navController.navigate("game")
                        },
                        onSettings        = { navController.navigate("settings") },
                        settingsViewModel = settingsViewModel
                    )
                }

                composable("game") {
                    GameScreen(
                        viewModel  = gameViewModel,
                        onGameOver = { score, gems ->
                            navController.navigate("gameover/$score/$gems") {
                                popUpTo("menu")
                            }
                        },
                        onMenu = {
                            gameViewModel.resetGame()
                            navController.navigate("menu") {
                                popUpTo("menu") { inclusive = true }
                            }
                        }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack    = { navController.popBackStack() }
                    )
                }

                composable(
                    route     = "gameover/{score}/{gems}",
                    arguments = listOf(
                        navArgument("score") { type = NavType.IntType },
                        navArgument("gems")  { type = NavType.IntType }
                    )
                ) { backStack ->
                    val score = backStack.arguments?.getInt("score") ?: 0
                    val gems  = backStack.arguments?.getInt("gems")  ?: 0
                    val best  = settingsViewModel.bestScore.value
                    GameOverScreen(
                        score       = score,
                        gems        = gems,
                        bestScore   = best,
                        isNewBest   = score > 0 && score >= best,
                        onPlayAgain = {
                            // Reset game FIRST, then navigate fresh — prevents showing GameOver twice
                            gameViewModel.resetGame()
                            gameViewModel.syncSettings(
                                settingsViewModel.ballColorIdx.value,
                                settingsViewModel.difficulty.value,
                                currentTheme
                            )
                            navController.navigate("game") {
                                popUpTo("menu")
                            }
                        },
                        onMenu = {
                            gameViewModel.resetGame()
                            navController.navigate("menu") {
                                popUpTo("menu") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}