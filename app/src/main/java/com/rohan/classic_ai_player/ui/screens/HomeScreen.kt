package com.rohan.classic_ai_player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.rohan.classic_ai_player.ui.compose_widgets.AudioCardItem
import com.rohan.classic_ai_player.ui.view_model.MusicViewModel
import com.rohan.classic_ai_player.ui.view_model.UIState
import com.rohan.classic_ai_player.utils.PlayerUiEvents

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel = hiltViewModel<MusicViewModel>(),
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }


    val tabs = listOf("Songs", "Playlists")
    val appCurrentPlayList by viewModel.appCurrentPlayList.collectAsState()

    val uiState by viewModel.uiState.observeAsState(UIState.Loading)

    val sheetState = rememberBottomSheetScaffoldState()


    val currentMediaItemIndex = viewModel.mExoPlayer.currentMediaItemIndex
    val exoPlayer = viewModel.mExoPlayer
    val selectedIndex = remember(viewModel.mExoPlayer) { mutableIntStateOf(0) }

    when (uiState) {
        is UIState.Success -> {
            val selectedTrack = viewModel.currSelectedMusic.collectAsState()

            val mediaItemList = remember(true) { mutableListOf<MediaItem>() }
            LaunchedEffect(key1 = appCurrentPlayList.size) {
                appCurrentPlayList.forEach { music ->
                    mediaItemList.add(MediaItem.fromUri(music.contentUri))
                }
                println("LAUNCHED EFFECT: List<Music> to List<MediaItem>")
            }


            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                sheetContent = {
                    PlayerBottomSheet(
                        audio = selectedTrack.value,
                        previous = { viewModel.onPlayerUiChanged(PlayerUiEvents.Backward) },
                        next = { viewModel.onPlayerUiChanged(PlayerUiEvents.SeekToNext) },
                        playPause = { viewModel.onPlayerUiChanged(PlayerUiEvents.PlayPause) },
                        onSeekChange = {
                            viewModel.onPlayerUiChanged(
                                PlayerUiEvents.UpdateProgress(
                                    it
                                )
                            )
                        },
                        onIndexChange = {
                            selectedIndex.intValue = it
                            viewModel.onPlayerUiChanged(PlayerUiEvents.SelectedAudioChange(it))
                        },
                        totalDuration = selectedTrack.value.duration.toLong(),
                        isPlaying = viewModel.mExoPlayer.isPlaying,
                        sheetScaffoldState = sheetState,
                        viewModel = viewModel
                    )
                },
                scaffoldState = sheetState,
                sheetPeekHeight = 100.dp,
                sheetDragHandle = { }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                text = { Text(title) },
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index }
                            )
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> SongsScreen(viewModel, mediaItemList) {
                            selectedIndex.intValue = it
                            viewModel.onPlayerUiChanged(PlayerUiEvents.SelectedAudioChange(it))
                        }

                        1 -> PlaylistScreen(viewModel)
                    }
                }
            }
        }

        else -> {}
    }


//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Music App") },
//            )
//        },
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//        ) {
//            TabRow(selectedTabIndex = selectedTabIndex) {
//                tabs.forEachIndexed { index, title ->
//                    Tab(
//                        text = { Text(title) },
//                        selected = selectedTabIndex == index,
//                        onClick = { selectedTabIndex = index }
//                    )
//                }
//            }
//
//            when (selectedTabIndex) {
//                0 -> SongsScreen(viewModel)
//                1 -> PlaylistScreen(viewModel)
//            }
//        }
//    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MusicViewModel = hiltViewModel<MusicViewModel>(),
    mediaItemList: MutableList<MediaItem>,
    selectedIndex: (Int) -> Unit,
) {
    val appCurrentPlayList by viewModel.appCurrentPlayList.collectAsState()
    val uiState by viewModel.uiState.observeAsState(UIState.Loading)


    when (uiState) {
        is UIState.Loading -> CircularProgressIndicator()
        is UIState.Success -> {

            LazyColumn {
                itemsIndexed(appCurrentPlayList) { index, music ->
                    AudioCardItem(
                        music = music,
                        viewModel = viewModel,
                        mediaItemList = mediaItemList,
                        selectedIndex = index,
                        selectedTrack = {
                            selectedIndex(index)
                        }
                    )

                }
            }

        }

        is UIState.Error -> {
            val errorMessage = (uiState as UIState.Error).message
            Text(text = errorMessage)
        }
    }
}

//@Composable
//fun SongItem(music: Music, isPlaying: Boolean, onClick: () -> Unit) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() }
//            .padding(16.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(music.songName, modifier = Modifier.weight(1f))
//        Icon(
//            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
//            contentDescription = null
//        )
//    }
//}


@Composable
fun PlaylistScreen(viewModel: MusicViewModel) {
    val playlists by viewModel.playlists.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize(
            )
            .background(color = Color.Blue)
    ) {
        Text(text = "PLAYLIST SCREEN")
    }
//
//    LazyColumn {
//        items(playlists) { playlist ->
//            PlaylistItem(
//                playlist = playlist,
//                onPlaylistClick = { viewModel.setPlaylist(playlist.songs) }
//            )
//        }
//    }
}