//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Pause
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.SkipNext
//import androidx.compose.material.icons.filled.SkipPrevious
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.LinearProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.rohan.classic_ai_player.player.service.MusicPlayerHandler
//import com.rohan.classic_ai_player.ui.view_model.MusicViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PlayingScreen(
//    viewModel: MusicViewModel = hiltViewModel(),
//) {
//    val playerState by viewModel.playerState.collectAsState()
//    val selectedMusic by viewModel.selectedMusic.collectAsState()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Now Playing") },
//                navigationIcon = {
//                    IconButton(onClick = { /* Handle navigation */ }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Music Info
//            selectedMusic?.let { music ->
//                Text(
//                    text = music.songName ?: "",
//                    style = MaterialTheme.typography.headlineMedium,
//                    textAlign = TextAlign.Center,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Text(
//                    text = music.artistName ?: "",
//                    style = MaterialTheme.typography.bodyLarge,
//                    textAlign = TextAlign.Center,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            // Progress Bar
//            when (val state = playerState) {
//                is MusicPlayerHandler.PlayerState.Playing -> {
//                    LinearProgressIndicator(
//                        progress = state.currentPosition.toFloat() / state.duration.toFloat(),
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(text = formatDuration(state.currentPosition))
//                        Text(text = formatDuration(state.duration))
//                    }
//                }
//
//                is MusicPlayerHandler.PlayerState.Paused -> {
//                    LinearProgressIndicator(
//                        progress = state.currentPosition.toFloat() / state.duration.toFloat(),
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(text = formatDuration(state.currentPosition))
//                        Text(text = formatDuration(state.duration))
//                    }
//                }
//
//                else -> {
//                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
//                }
//            }
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            // Playback Controls
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                IconButton(onClick = { viewModel.skipToPrevious() }) {
//                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
//                }
//                IconButton(onClick = { viewModel.playPause() }) {
//                    Icon(
//                        if (playerState is MusicPlayerHandler.PlayerState.Playing)
//                            Icons.Default.Pause else Icons.Default.PlayArrow,
//                        contentDescription = "Play/Pause"
//                    )
//                }
//                IconButton(onClick = { viewModel.skipToNext() }) {
//                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun formatDuration(durationMs: Long): String {
//    val totalSeconds = durationMs / 1000
//    val minutes = totalSeconds / 60
//    val seconds = totalSeconds % 60
//    return String.format("%d:%02d", minutes, seconds)
//}