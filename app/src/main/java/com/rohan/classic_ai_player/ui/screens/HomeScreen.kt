package com.rohan.classic_ai_player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.rohan.classic_ai_player.data.model.Playlist
import com.rohan.classic_ai_player.ui.compose_widgets.AudioCardItem
import com.rohan.classic_ai_player.ui.compose_widgets.PlaylistSelectionDialog
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
                        0 -> MusicListScreen(viewModel) {
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(viewModel: MusicViewModel, selectedIndex: (Int) -> Unit) {
    val musicList by viewModel.appCurrentPlayList.collectAsState()
    val selectedMusicIds by viewModel.selectedMusicIds.collectAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music List") },
                actions = {
                    if (selectedMusicIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                        IconButton(onClick = { showPlaylistDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to playlist"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            itemsIndexed(musicList) { index, music ->
                val isSelected = selectedMusicIds.contains(music.musicId)
                ListItem(
                    headlineContent = { Text(music.songName) },
                    supportingContent = { Text(music.artistName) },
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                selectedIndex(index)
                            },
                            onLongClick = { viewModel.toggleMusicSelection(music.musicId) }
                        )
                        .padding(16.dp),
                    leadingContent = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
            }
        }
    }

    if (showPlaylistDialog) {
        PlaylistSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showPlaylistDialog = false }
        )
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

    LazyColumn {
        items(playlists) { playlist ->
            PlaylistTile(
                playlist = playlist,
                onClick = {
                    // on playlist play
                }
            )
        }
    }
}

@Composable
fun PlaylistTile(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(playlist.playlistName, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null
        )
    }


}