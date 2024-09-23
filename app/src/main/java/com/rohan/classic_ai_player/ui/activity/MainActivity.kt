package com.rohan.classic_ai_player.ui.activity

import PlayingScreen
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.rohan.classic_ai_player.player.service.MusicSessionService
import com.rohan.classic_ai_player.ui.screens.SongsScreen
import com.rohan.classic_ai_player.ui.theme.ClassicAIPlayerTheme
import com.rohan.classic_ai_player.ui.view_model.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()
    private var isServiceRunning = false

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassicAIPlayerTheme {

                val permissionState = rememberPermissionState(
                    permission = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                            Manifest.permission.READ_MEDIA_IMAGES
                        }

                        else -> {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                    }
                )

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(key1 = lifecycleOwner) {

                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            permissionState.launchPermissionRequest()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    MyApp()

//                    MusicListingScreen(
//                        progress = musicViewModel.progress,
//                        onProgress = { musicViewModel.onPlayerUiChanged(PlayerUiEvents.SeekTo(it)) },
//                        isMusicPlaying = musicViewModel.isPlaying,
//                        musicList = musicViewModel.musicList,
//                        currentPlayingMusic = musicViewModel.currSelectedMusic,
//                        onStart = {
//                            musicViewModel.onPlayerUiChanged(PlayerUiEvents.PlayPause)
//                        },
//                        onItemClick = {
//                            musicViewModel.onPlayerUiChanged(PlayerUiEvents.SelectedAudioChange(it))
//                            startService()
//                        },
//                        onNext = {
//                            musicViewModel.onPlayerUiChanged(PlayerUiEvents.SeekToNext)
//                        }
//
//                    )

                }

            }
        }
    }

    private fun startService() {
        if (!isServiceRunning) {
            val intent = Intent(this, MusicSessionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            isServiceRunning = true
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "songListScreen") {

        composable("songListScreen") { SongsScreen(navController) }
        composable("playingScreen") { PlayingScreen() }

    }
}

