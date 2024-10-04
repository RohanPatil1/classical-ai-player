package com.rohan.classic_ai_player.ui.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.rohan.classic_ai_player.ui.screens.HomeScreen
import com.rohan.classic_ai_player.ui.theme.ClassicAIPlayerTheme
import com.rohan.classic_ai_player.ui.view_model.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassicAIPlayerTheme {
                val context = LocalContext.current

                // Track permission request state
                var permissionRequestState by remember { mutableStateOf(PermissionRequestState.NotRequested) }

                val permissionState = rememberPermissionState(
                    permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                )

                // Automatically request permission when the app starts
                LaunchedEffect(Unit) {
                    permissionState.launchPermissionRequest()
                    permissionRequestState = PermissionRequestState.Requested
                }

                // Handle permission state changes
                LaunchedEffect(permissionState.status) {
                    when (permissionState.status) {
                        is PermissionStatus.Granted -> {
                            permissionRequestState = PermissionRequestState.Granted
                            musicViewModel.loadMusicIfNeeded()
                        }

                        is PermissionStatus.Denied -> {
                            permissionRequestState = PermissionRequestState.Denied
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (permissionRequestState) {
                        PermissionRequestState.NotRequested, PermissionRequestState.Requested -> {
                            // Show loading or blank screen while waiting for permission decision
                            Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }

                        PermissionRequestState.Granted -> {
                            HomeScreen(viewModel = musicViewModel)
                        }

                        PermissionRequestState.Denied -> {
                            PermissionDeniedUI(
                                shouldShowRationale = permissionState.status.shouldShowRationale,
                                onRequestPermission = {
                                    permissionState.launchPermissionRequest()
                                    permissionRequestState = PermissionRequestState.Requested
                                },
                                onOpenSettings = {
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data =
                                                Uri.fromParts("package", context.packageName, null)
                                        }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedUI(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (shouldShowRationale) {
                "Storage permission is required to access your music files."
            } else {
                "Storage permission is necessary for playing your music files"
            },
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = if (shouldShowRationale) onRequestPermission else onOpenSettings
        ) {
            Text(
                text = if (shouldShowRationale) "Request Permission" else "Open App Settings"
            )
        }
    }
}

enum class PermissionRequestState {
    NotRequested, Requested, Granted, Denied
}