package com.rohan.classic_ai_player.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.rohan.classic_ai_player.R
import com.rohan.classic_ai_player.data.model.Music
import com.rohan.classic_ai_player.ui.compose_widgets.ClassicAppBar
import com.rohan.classic_ai_player.ui.view_model.MusicViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


@ExperimentalMaterial3Api
@UnstableApi
@Composable
fun PlayerBottomSheet(
    audio: Music,
    previous: () -> Unit,
    next: () -> Unit,
    playPause: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onIndexChange: (Int) -> Unit,
    totalDuration: Long,
    isPlaying: Boolean,
    sheetScaffoldState: BottomSheetScaffoldState,
    viewModel: MusicViewModel,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerTopContent(
                audio = audio,
                sheetScaffoldState = sheetScaffoldState
            )

            PlayerCenterControls(
                totalDuration = totalDuration,
                onChange = onSeekChange,
                viewModel = viewModel,
            )
            PlayerBottomControls(
                previous = {
                    previous()
                    onIndexChange(viewModel.mExoPlayer.currentMediaItemIndex)
                },
                next = {
                    next()
                    onIndexChange(viewModel.mExoPlayer.currentMediaItemIndex)
                },
                playPause = playPause,
                totalDuration = totalDuration,
                viewModel = viewModel
            )
        }
        AnimatedVisibility(
            visible = sheetScaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded,
            enter = slideInVertically(animationSpec = tween(500), initialOffsetY = { -it }),
            exit = slideOutVertically(animationSpec = tween(500), targetOffsetY = { -it })
        ) {
            PlayerBottomBar(
                audio = audio,
                playPause = playPause,
                next = next,
                sheetScaffoldState = sheetScaffoldState,
                viewModel = viewModel,
                totalDuration = totalDuration
            )
        }
    }
}


@ExperimentalMaterial3Api
@Composable
fun PlayerTopContent(
    audio: Music,
    sheetScaffoldState: BottomSheetScaffoldState,
) {

    val mAudio = remember(audio) { mutableStateOf(audio) }
    val painterState = remember(audio) { mutableStateOf(PainterState.LOADING) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        ClassicAppBar(
            modifier = Modifier.padding(top = 20.dp),
            sheetState = sheetScaffoldState
        )
        when (painterState.value) {
            PainterState.LOADING, PainterState.SUCCESS -> {
                AsyncImage(
                    model = mAudio.value.albumArt,
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f),
                    contentScale = ContentScale.FillBounds,
                    onError = { painterState.value = PainterState.ERROR },
                    onLoading = { painterState.value = PainterState.LOADING }
                )
            }

            PainterState.ERROR -> {
                Icon(
                    painter = painterResource(id = R.drawable.music),
                    contentDescription = "Music",
                    modifier = Modifier
                        .scale(0.50f)
                        .padding(top = 22.dp, bottom = 22.dp)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 25.dp, bottom = 45.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = mAudio.value.songName,
                style = TextStyle(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mAudio.value.artistName,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun PlayerCenterControls(
    totalDuration: Long,
    onChange: (Float) -> Unit,
    viewModel: MusicViewModel,
) {

    val progress = viewModel.progress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(15.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = 100f,
                onValueChange = { },
                valueRange = 0f..100f,
                enabled = false,
                colors = SliderDefaults.colors(
                    disabledActiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledThumbColor = Color.Transparent
                )
            )
            Slider(
                value = progress.value,
                onValueChange = {
                    onChange(it)
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.onBackground,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = Color.Transparent
                ),
                thumb = { }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ((progress.value / 100f) * totalDuration).toLong().formatMinSec(),
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = totalDuration.formatMinSec(),
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@UnstableApi
@Composable
fun PlayerBottomControls(
    previous: () -> Unit,
    next: () -> Unit,
    playPause: () -> Unit,
    totalDuration: Long,
    viewModel: MusicViewModel,
) {

    val isPlaying = viewModel.isPlaying.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = previous) {
            Icon(
                painter = painterResource(id = R.drawable.play_previous),
                contentDescription = "Previous"
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Surface(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .clickable {
//                    if (progress.value.toLong() == totalDuration) {
//                        viewModel.onPlayerUiChanged(PlayerUiEvents.SeekTo(0f))
//                    } else {
//                        playPause()
//                    }
                    playPause()
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (isPlaying.value) R.drawable.pause else R.drawable.play),
                    contentDescription = "Play/Pause"
                )
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        IconButton(onClick = next) {
            Icon(
                painter = painterResource(id = R.drawable.play_next),
                contentDescription = "Next"
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun PlayerBottomBar(
    audio: Music,
    playPause: () -> Unit,
    next: () -> Unit,
    sheetScaffoldState: BottomSheetScaffoldState,
    viewModel: MusicViewModel,
    totalDuration: Long,
) {
    val scope = rememberCoroutineScope()
    val mAudio = remember(audio) { mutableStateOf(audio) }
    val painterState = remember(audio) { mutableStateOf(PainterState.LOADING) }
    val isPlaying = viewModel.isPlaying.collectAsState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(bottom = 20.dp)
            .clickable { scope.launch { sheetScaffoldState.bottomSheetState.expand() } },
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
//            Box(
//                modifier = Modifier.fillMaxHeight(),
//                contentAlignment = Alignment.Center
//            ) {
//                when (painterState.value) {
//                    PainterState.SUCCESS, PainterState.LOADING -> {
//                        AsyncImage(
//                            modifier = Modifier
//                                .size(50.dp)
//                                .clip(RoundedCornerShape(10.dp)),
//                            model = mAudio.value.albumArt,
//                            contentDescription = audio.songName,
//                            contentScale = ContentScale.FillBounds,
//                            onError = { painterState.value = PainterState.ERROR },
//                            onLoading = { painterState.value = PainterState.LOADING }
//                        )
//                    }
//
//                    PainterState.ERROR -> {
//                        Icon(
//                            painter = painterResource(id = R.drawable.music),
//                            contentDescription = "Album Art",
//                            modifier = Modifier
//                                .scale(0.6f)
//                                .height(44.dp)
//                                .width(50.dp)
//                        )
//                    }
//                }
//            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 30.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(0.50f),
                    text = audio.songName,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier.fillMaxWidth(0.50f),
                    text = audio.artistName,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .clickable { playPause() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(id = if (isPlaying.value) R.drawable.pause else R.drawable.play),
                        contentDescription = "Play/Pause"
                    )
                }
            }

            IconButton(onClick = next) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.play_next),
                    contentDescription = "Next"
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
fun Long.formatMinSec(): String {
    return String.format(
        "%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(this),
        TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(
                this
            )
        )
    )
}

enum class PainterState {
    LOADING,
    ERROR,
    SUCCESS
}
//@ExperimentalMaterial3Api
//@UnstableApi
//@Preview(showBackground = true)
//@Composable
//fun PlayerPreview() {
//    val audio = Music(
//        musicId = 0L,
//        duration = 12000,
//        audioStats = null,
//        contentUri = Uri.EMPTY,
//        artistName = "",
//        albumName = "",
//        songName = "Dummy Name"
//    )
//
//    PlayerContent(
//        audio = audio,
//        previous = { },
//        next = { },
//        playPause = { },
//        duration = 90L,
//        onSeekChange = { },
//        onIndexChange = { },
//        totalDuration = 100L,
//        isPlaying = false,
//        sheetScaffoldState = rememberBottomSheetScaffoldState(),
//        viewModel = hiltViewModel()
//    )
//}