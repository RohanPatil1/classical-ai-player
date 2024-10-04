package com.rohan.classic_ai_player.player.normalizer

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.rohan.classic_ai_player.data.model.AudioStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class MusicNormalizer @Inject constructor(@ApplicationContext val context: Context) {

    suspend fun analyzeAudio(contentUri: Uri): AudioStats = withContext(Dispatchers.IO) {

        val filePath = getFilePathFromUri(context, contentUri)

        val command = "-i $filePath -af loudnorm=print_format=json -f null -"
        var stats = AudioStats(0f, 0f, 0f)

        FFmpegKit.execute(command).also { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                // Extract JSON data from FFmpeg output
                val jsonRegex = "\\{[\\s\\S]*\\}".toRegex()
                val jsonMatch = jsonRegex.find(session.output)
                val jsonString = jsonMatch?.value ?: "{}"
                val json = JSONObject(jsonString)

                stats = AudioStats(
                    integratedLoudness = json.optDouble("input_i", 0.0).toFloat(),
                    loudnessRange = json.optDouble("input_lra", 0.0).toFloat(),
                    truePeak = json.optDouble("input_tp", 0.0).toFloat()
                )
            } else {
                val log = session?.allLogs
                println("FFmpeg process failed with state ${session.state} and rc ${session.returnCode}")
                println("Error message: ${session.failStackTrace}")
                println("FFmpeg LOGS: $log")
            }
        }

        stats
    }

    fun calculateDynamicGain(
        stats: AudioStats,
        targetLoudness: Float = -14f,
        maxGain: Float = 15f,
    ): Float {
        val baseGain = targetLoudness - stats.integratedLoudness
        val dynamicFactor = (stats.loudnessRange / 20f).coerceAtMost(1f) // Normalize LRA
        return (baseGain * (1 - dynamicFactor)).coerceIn(-maxGain, maxGain)
    }


    fun getAudioGain(audioStats: AudioStats): Float {
        return calculateDynamicGain(audioStats)
    }

    fun getFilePathFromUri(context: Context, contentUri: Uri): String? {
        val fileDescriptor =
            context.contentResolver.openFileDescriptor(contentUri, "r") ?: return null
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)

        // Create a temporary file
        val tempFile = File(context.cacheDir, "temp_audio_file.mp3")
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return tempFile.absolutePath
    }
}