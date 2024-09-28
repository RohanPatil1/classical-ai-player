package com.rohan.classic_ai_player.data.source

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.WorkerThread
import com.rohan.classic_ai_player.data.model.Music
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MusicContentResolver @Inject
constructor(@ApplicationContext val context: Context) {
    private var mCursor: Cursor? = null

    private val projection: Array<String> = arrayOf(
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.ARTIST,
        MediaStore.Audio.AudioColumns.DATA,
        MediaStore.Audio.AudioColumns.DURATION,
        MediaStore.Audio.AudioColumns.TITLE,
    )

    private var selectionClause: String? =
        "${MediaStore.Audio.AudioColumns.IS_MUSIC} = ? AND ${MediaStore.Audio.Media.MIME_TYPE} NOT IN (?, ?, ?)"
    private var selectionArg = arrayOf("1", "audio/amr", "audio/3gpp", "audio/aac")

    private val sortOrder = "${MediaStore.Audio.AudioColumns.DISPLAY_NAME} ASC"


    @WorkerThread
    fun fetchMusicList(): List<Music> {
        return getCursorData()
    }


    private fun getCursorData(): MutableList<Music> {
        val audioList = mutableListOf<Music>()

        mCursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selectionClause,
            selectionArg,
            sortOrder
        )

        mCursor?.use { cursor ->
            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
            val artistColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
            val dataColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
            val titleColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)

            cursor.apply {
                if (count == 0) {
                    Log.e("Cursor", "getCursorData: Cursor is Empty")
                } else {
                    while (cursor.moveToNext()) {
                        val displayName = getString(displayNameColumn)
                        val id = getLong(idColumn)
                        val artist = getString(artistColumn)
                        val data = getString(dataColumn)
                        val duration = getInt(durationColumn)
                        //val title = getString(titleColumn)
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        audioList += Music(
                            musicId = id,
                            albumName = data,
                            songName = displayName,
                            contentUri = uri,
                            duration = duration,
                            artistName = artist,
                        )
                    }

                }
            }


        }

        return audioList
    }

//    @WorkerThread
//    fun getAlbumArt(context: Context, uri: Uri): Bitmap?{
//        val mmr = MediaMetadataRetriever()
//        mmr.setDataSource(context, uri)
//        val bitmap: Bitmap? = try{
//            val data = mmr.embeddedPicture
//            if (data != null){
//                BitmapFactory.decodeByteArray(data, 0, data.size)
//            } else{
//                null
//            }
//        } catch (exp: Exception){
//            null
//        } finally {
//            mmr.release()
//        }
//        return bitmap
//    }

}