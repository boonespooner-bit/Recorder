package com.recorder.app.audio

import com.recorder.app.data.model.TimedChord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ChordAnalyzer @Inject constructor(
    private val chromagramExtractor: ChromagramExtractor,
    private val templateManager: ChordTemplateManager
) {

    companion object {
        // 12-element binary chord templates indexed as [C, C#, D, D#, E, F, F#, G, G#, A, A#, B]
        private val BUILTIN_TEMPLATES: Map<String, FloatArray> = mapOf(
            "G"  to floatArrayOf(0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
            "C"  to floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f),
            "D"  to floatArrayOf(0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f),
            "A"  to floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f),
            "Am" to floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f),
            "Em" to floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
            "E"  to floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 1f),
            "F"  to floatArrayOf(1f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 0f)
        )
    }

    suspend fun analyzeFile(filePath: String): List<TimedChord> = withContext(Dispatchers.IO) {
        val hopSizeMs = 250L
        val windowSizeMs = 500L

        val chromagram = chromagramExtractor.extractChromagram(filePath, windowSizeMs, hopSizeMs)
        if (chromagram.isEmpty()) return@withContext emptyList()

        val templates: Map<String, FloatArray> = if (templateManager.hasAllTemplates()) {
            templateManager.getAllTemplates()
        } else {
            BUILTIN_TEMPLATES
        }

        // Assign best chord for each frame
        val frameChords = chromagram.mapIndexed { frameIndex, chroma ->
            var bestChord = ""
            var bestSim = Float.NEGATIVE_INFINITY
            for ((chord, template) in templates) {
                val sim = cosineSimilarity(chroma, template)
                if (sim > bestSim) {
                    bestSim = sim
                    bestChord = chord
                }
            }
            val startMs = frameIndex * hopSizeMs
            val endMs = startMs + windowSizeMs
            Triple(bestChord, startMs, endMs)
        }

        // Merge consecutive identical chords
        val merged = mutableListOf<TimedChord>()
        for ((chord, startMs, endMs) in frameChords) {
            val last = merged.lastOrNull()
            if (last != null && last.chord == chord) {
                merged[merged.lastIndex] = last.copy(endTimeMs = endMs)
            } else {
                merged.add(TimedChord(chord = chord, startTimeMs = startMs, endTimeMs = endMs))
            }
        }

        merged
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom < 1e-6f) 0f else dot / denom
    }
}
