package com.rohan.classic_ai_player.data.source

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import com.rohan.classic_ai_player.data.model.Music
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
class MusicContentResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var cursor: Cursor? = null

    private val projection = arrayOf(
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns.TITLE,
        MediaStore.Audio.AudioColumns.DURATION,
        MediaStore.Audio.AudioColumns.ARTIST,
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.ALBUM_ID,
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns.MIME_TYPE // Added MIME_TYPE to projection
    )

    // Filter for music files that are mp3 and are marked as music
    private val selectionClaus =
        "${MediaStore.Audio.Media.IS_MUSIC} = ? AND ${MediaStore.Audio.AudioColumns.MIME_TYPE} = ?"
    private val selectionArg =
        arrayOf("1", "audio/mpeg") // "audio/mpeg" is the MIME type for mp3 files
    private val sortOrder = "${MediaStore.Audio.AudioColumns.DISPLAY_NAME} ASC"

    @WorkerThread
    fun fetchMusicList(): List<Music> {
        return getCursorData()
    }

    private fun getCursorData(): MutableList<Music> {

        val musicList = mutableListOf<Music>()

        cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selectionClaus,
            selectionArg,
            sortOrder
        )

        cursor?.use { mCursor ->
            val displayNameColumn =
                mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
            val idColumn = mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
            val idAlbumColumn =
                mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID)
            val titleColumn = mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
            val durationColumn =
                mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
            val artistColumn = mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)

            mCursor.apply {

                while (mCursor.moveToNext()) {
                    val displayName = getString(displayNameColumn)
                    val id = getLong(idColumn)
                    val idAlbum = getLong(idAlbumColumn)
                    val title = getString(titleColumn)
                    val duration = getInt(durationColumn)
                    val artistName = getString(artistColumn)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val albumArt = ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        idAlbum
                    )

                    musicList.add(
                        Music(
                            songName = displayName,
                            musicId = id,
                            contentUri = uri,
                            title = title,
                            artistName = artistName,
                            duration = duration,
                            albumArt = albumArt
                        )
                    )
                }
            }
        }
        return musicList
    }
}
