package com.rohan.classic_ai_player.player.normalizer

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.rohan.classic_ai_player.data.model.AudioStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class MusicNormalizer @Inject constructor() {

    suspend fun analyzeAudio(filePath: String): AudioStats = withContext(Dispatchers.IO) {
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
                println("FFmpeg process failed with state ${session.state} and rc ${session.returnCode}")
                println("Error message: ${session.failStackTrace}")
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
}