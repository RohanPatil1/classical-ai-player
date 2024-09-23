package com.rohan.classic_ai_player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.ui.view_model.MusicViewModel
import kotlin.math.floor


@Composable
fun SongsScreen(navController: NavController, viewModel: MusicViewModel = hiltViewModel()) {

    val musicList by viewModel.allMusicList.collectAsState()
    val operationState by viewModel.screenState.collectAsState()

    when (operationState) {
        is MusicViewModel.UIState.Loading -> CircularProgressIndicator()
        is MusicViewModel.UIState.Error -> Text(
            text = "Error",
            color = androidx.compose.ui.graphics.Color.White
        )

        else -> {
            LazyColumn {
                items(musicList) { music ->
                    MusicListTile(music) {
                        viewModel.getMusicById(music.id)
                        navController.navigate("playingScreen")
                    }
                }
            }
        }
    }
}

@Composable
fun MusicListTile(
    music: Music,
    onItemClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable {
                onItemClick()
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = music.songName ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    overflow = TextOverflow.Clip,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = music.artistName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )

            }
            Text(
                text = timeStampToDuration(music.duration.toLong())
            )
            Spacer(modifier = Modifier.size(8.dp))
        }

    }

}


fun timeStampToDuration(position: Long): String {
    val totalSecond = floor(position / 1E3).toInt()
    val minutes = totalSecond / 60
    val remainingSeconds = totalSecond - (minutes * 60)
    return if (position < 0) "--:--"
    else "%d:%02d".format(minutes, remainingSeconds)
}