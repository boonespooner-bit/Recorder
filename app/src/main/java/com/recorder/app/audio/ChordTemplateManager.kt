package com.recorder.app.audio

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChordTemplateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chromagramExtractor: ChromagramExtractor,
    private val gson: Gson
) {

    val requiredChords: List<String> = listOf("G", "C", "D", "A", "Am", "Em", "E", "F")

    private val templatesFile: File = File(context.filesDir, "chord_templates.json")

    private val templates: MutableMap<String, FloatArray> = mutableMapOf()

    init {
        loadTemplates()
    }

    suspend fun saveTemplate(chord: String, audioFile: File) {
        withContext(Dispatchers.IO) {
            val chromagram = chromagramExtractor.extractChromagram(audioFile.absolutePath)
            if (chromagram.isEmpty()) return@withContext

            val averaged = FloatArray(12)
            for (frame in chromagram) {
                for (i in 0 until 12) averaged[i] += frame[i]
            }
            for (i in 0 until 12) averaged[i] /= chromagram.size.toFloat()

            templates[chord] = averaged
            persistTemplates()
        }
    }

    fun getTemplate(chord: String): FloatArray? = templates[chord]

    fun getAllTemplates(): Map<String, FloatArray> = templates.toMap()

    fun hasAllTemplates(): Boolean = requiredChords.all { templates.containsKey(it) }

    fun getTrainedChords(): List<String> = requiredChords.filter { templates.containsKey(it) }

    private fun persistTemplates() {
        val serializable: Map<String, List<Float>> = templates.mapValues { it.value.toList() }
        templatesFile.writeText(gson.toJson(serializable))
    }

    private fun loadTemplates() {
        if (!templatesFile.exists()) return
        try {
            val json = templatesFile.readText()
            val type = object : TypeToken<Map<String, List<Float>>>() {}.type
            val loaded: Map<String, List<Float>> = gson.fromJson(json, type) ?: return
            for ((chord, values) in loaded) {
                templates[chord] = values.toFloatArray()
            }
        } catch (_: Exception) {
        }
    }
}
