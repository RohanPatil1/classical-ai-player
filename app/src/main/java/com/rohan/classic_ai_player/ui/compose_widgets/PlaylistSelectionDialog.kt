package com.rohan.classic_ai_player.ui.compose_widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rohan.classic_ai_player.ui.view_model.MusicViewModel


@Composable
fun PlaylistSelectionDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    var newPlaylistName by remember { mutableStateOf("") }
    val playlists by viewModel.playlists.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select or Create Playlist") },
        text = {
            Column {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("New Playlist Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createNewPlaylist(newPlaylistName)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Create and Add")
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn {
                    items(playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.playlistName) },
                            modifier = Modifier.clickable {
                                viewModel.addSelectedMusicToPlaylist(playlist.playlistId)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}