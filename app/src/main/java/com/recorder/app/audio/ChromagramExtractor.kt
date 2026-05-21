package com.recorder.app.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class ChromagramExtractor @Inject constructor() {

    companion object {
        private const val TARGET_SAMPLE_RATE = 11025
        private const val FFT_SIZE = 4096
        // C0 in Hz; all chroma bin computation is relative to this reference
        private const val C0_HZ = 16.35f
    }

    fun extractChromagram(
        filePath: String,
        windowSizeMs: Long = 500,
        hopSizeMs: Long = 250
    ): List<FloatArray> {
        val rawSamples = decodeAudioToPcm(filePath)
        if (rawSamples.first.isEmpty()) return emptyList()

        val (samples, sampleRate) = rawSamples

        val targetSamples = if (sampleRate != TARGET_SAMPLE_RATE) {
            resample(samples, sampleRate, TARGET_SAMPLE_RATE)
        } else {
            samples
        }

        val windowSizeSamples = (windowSizeMs * TARGET_SAMPLE_RATE / 1000).toInt()
        val hopSizeSamples = (hopSizeMs * TARGET_SAMPLE_RATE / 1000).toInt()

        val chromagram = mutableListOf<FloatArray>()
        var offset = 0

        while (offset + windowSizeSamples <= targetSamples.size) {
            val window = FloatArray(windowSizeSamples) { i ->
                // Hann window to reduce spectral leakage
                val hann = 0.5f * (1f - cos(2.0 * PI * i / (windowSizeSamples - 1)).toFloat())
                targetSamples[offset + i] * hann
            }

            val chroma = computeChroma(window)
            chromagram.add(chroma)
            offset += hopSizeSamples
        }

        return chromagram
    }

    private fun decodeAudioToPcm(filePath: String): Pair<FloatArray, Int> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(filePath)
        } catch (e: Exception) {
            extractor.release()
            return Pair(FloatArray(0), 0)
        }

        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }

        if (audioTrackIndex == -1 || format == null) {
            extractor.release()
            return Pair(FloatArray(0), 0)
        }

        extractor.selectTrack(audioTrackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmSamples = mutableListOf<Float>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex, 0, sampleSize,
                                extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

                    val shortCount = bufferInfo.size / 2
                    val shorts = ShortArray(shortCount)
                    outputBuffer.asShortBuffer().get(shorts)

                    // Mix down to mono by averaging channels; normalise to [-1, 1]
                    var i = 0
                    while (i + channelCount <= shorts.size) {
                        var mono = 0f
                        for (ch in 0 until channelCount) {
                            mono += shorts[i + ch].toFloat()
                        }
                        pcmSamples.add(mono / (channelCount * 32768f))
                        i += channelCount
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        return Pair(pcmSamples.toFloatArray(), sampleRate)
    }

    private fun resample(samples: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return samples
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outputLength = (samples.size / ratio).toInt()
        return FloatArray(outputLength) { i ->
            val srcIndex = (i * ratio).toInt().coerceIn(0, samples.size - 1)
            samples[srcIndex]
        }
    }

    private fun computeChroma(windowSamples: FloatArray): FloatArray {
        val re = FloatArray(FFT_SIZE)
        val im = FloatArray(FFT_SIZE)
        val copyLen = minOf(windowSamples.size, FFT_SIZE)
        for (i in 0 until copyLen) re[i] = windowSamples[i]

        fft(re, im)

        val chroma = FloatArray(12)
        val nyquistBin = FFT_SIZE / 2

        for (bin in 1 until nyquistBin) {
            val freq = bin.toFloat() * TARGET_SAMPLE_RATE / FFT_SIZE.toFloat()
            if (freq < C0_HZ) continue

            val magnitude = sqrt(re[bin] * re[bin] + im[bin] * im[bin])

            // Map frequency to chroma bin: chroma = round(12 * log2(freq / C0)) mod 12
            val chromaBin = (12.0 * log2(freq / C0_HZ)).roundToInt().mod(12)
            chroma[chromaBin] += magnitude
        }

        // L2 normalise so each chroma vector has unit length
        val norm = sqrt(chroma.fold(0f) { acc, v -> acc + v * v })
        if (norm > 1e-6f) {
            for (i in chroma.indices) chroma[i] /= norm
        }

        return chroma
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var tmp = re[i]; re[i] = re[j]; re[j] = tmp
                tmp = im[i]; im[i] = im[j]; im[j] = tmp
            }
        }

        // Cooley-Tukey iterative radix-2 butterfly
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wRe = cos(angle).toFloat()
            val wIm = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f
                var curIm = 0f
                for (jj in 0 until halfLen) {
                    val uRe = re[i + jj]
                    val uIm = im[i + jj]
                    val vRe = re[i + jj + halfLen] * curRe - im[i + jj + halfLen] * curIm
                    val vIm = re[i + jj + halfLen] * curIm + im[i + jj + halfLen] * curRe
                    re[i + jj] = uRe + vRe
                    im[i + jj] = uIm + vIm
                    re[i + jj + halfLen] = uRe - vRe
                    im[i + jj + halfLen] = uIm - vIm
                    val nextRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nextRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
