@file:OptIn(ExperimentalFoundationApi::class)

package com.rohan.classic_ai_player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.rohan.classic_ai_player.data.model.Playlist
import com.rohan.classic_ai_player.ui.compose_widgets.PlayerBottomSheet
import com.rohan.classic_ai_player.ui.compose_widgets.PlaylistSelectionDialog
import com.rohan.classic_ai_player.ui.view_model.MusicViewModel
import com.rohan.classic_ai_player.utils.PlayerUiEvents

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel = hiltViewModel<MusicViewModel>(),
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Songs", "Playlists")
    val isLoading by viewModel.isLoading.collectAsState()
    val sheetState = rememberBottomSheetScaffoldState()
    var currMusicIndex by remember { mutableIntStateOf(0) }

    if (!isLoading) {
        BottomSheetScaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Classic App bar") }
                )
            },
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            sheetContent = {
                PlayerBottomSheet(
                    previous = {
                        currMusicIndex -= 1
                        viewModel.onPlayerUiChanged(
                            PlayerUiEvents.SelectedAudioChange(
                                currMusicIndex
                            )
                        )
                    },
                    next = {
                        currMusicIndex += 1
                        viewModel.onPlayerUiChanged(
                            PlayerUiEvents.SelectedAudioChange(
                                currMusicIndex
                            )
                        )
                    },
                    playPause = { viewModel.onPlayerUiChanged(PlayerUiEvents.PlayPause) },
                    onSeekChange = {
                        viewModel.onPlayerUiChanged(
                            PlayerUiEvents.UpdateProgress(
                                it
                            )
                        )
                    },
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
                            onClick = {
                                selectedTabIndex = index
                                if (index == 0) {
                                    // Go to Songs Screen
                                    viewModel.resetPlaylistSelection()
                                }
                            }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> MusicListScreen(viewModel) {
                        currMusicIndex = it
                        viewModel.onPlayerUiChanged(PlayerUiEvents.SelectedAudioChange(it))
                    }

                    1 -> PlaylistScreen(viewModel) {
                        currMusicIndex = it
                        viewModel.onPlayerUiChanged(PlayerUiEvents.SelectedAudioChange(it))
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center // Aligns the CircularProgressIndicator to the center
        ) {
            CircularProgressIndicator()
        }
    }
}

@UnstableApi
@Composable
fun MusicListScreen(viewModel: MusicViewModel, selectedIndex: (Int) -> Unit) {
    val musicList by viewModel.appCurrentPlayList.collectAsState()
    val selectedMusicIds by viewModel.selectedMusicIds.collectAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Column {
            if (selectedMusicIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {

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
    }

    if (showPlaylistDialog) {
        PlaylistSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showPlaylistDialog = false }
        )
    }
}


@UnstableApi
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlaylistScreen(viewModel: MusicViewModel, selectedIndex: (Int) -> Unit) {
    val playlists by viewModel.playlists.collectAsState()
    val currSelectedPlaylistMusic by viewModel.currSelectedPlaylistMusic.collectAsState()
    var playlistName by remember { mutableStateOf("") }
    Column {

        if (currSelectedPlaylistMusic.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.resetPlaylistSelection()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(playlistName)
            }

            // show song list for the current selected playlist
            LazyColumn {
                itemsIndexed(currSelectedPlaylistMusic) { index, music ->
                    ListItem(
                        headlineContent = { Text(music.songName) },
                        supportingContent = { Text(music.artistName) },
                        modifier = Modifier
                            .clickable {
                                selectedIndex(index)
                            }
                            .padding(16.dp),

                        )
                }
            }
        } else {
            // show list of playlists
            LazyColumn {
                items(playlists) { playlist ->
                    PlaylistTile(
                        playlist = playlist,
                        onClick = {
                            // on playlist play
                            playlistName = playlist.playlistName
                            viewModel.playPlaylist(playlist)
                        }
                    )
                }
            }
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