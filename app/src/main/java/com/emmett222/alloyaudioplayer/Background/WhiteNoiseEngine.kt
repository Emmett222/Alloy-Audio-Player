package com.emmett222.alloyaudioplayer.Background

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Binder
import android.os.IBinder
import java.util.Random
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * White noise background engine for Alloy Audio Player.
 * Mostly made by Gemini.
 *
 * @author Emmett Grebe
 * @version 5-27-2026
 */
class WhiteNoiseEngine : Service() {

    private val binder = LocalBinder()
    private var audioTrack: AudioTrack? = null

    @Volatile
    var isPlayingNoise = false
    private val sampleRate = 44100

    // Original variables
    private var filterWarmth = 0
    private var textureStrength = 0
    private var waveValue = 0

    // New customization variables
    private var spatialWidth = 0
    private var bassCut = 0
    private var windGusting = 0

    // DSP State Trackers (Doubled for Stereo Phase Independence)
    private var sampleCounter = 0
    private var lastOutputLeft = 0
    private var lastOutputRight = 0
    private var lastBassLeft = 0
    private var lastBassRight = 0

    // Wind Random Walk State Trackers
    private var windTarget = 1.0
    private var windEnvelope = 1.0

    private val random = Random()

    override fun onBind(intent: Intent?): IBinder {
        initAudioEngine()
        return binder
    }

    /**
     * Reconfigured to CHANNEL_OUT_STEREO to support spatial tracking.
     */
    fun initAudioEngine() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /**
     * Makes audio pierce less.
     */
    fun updateWarmth(value: Int) {
        filterWarmth = value
    }

    /**
     * Like rain.
     */
    fun updateTexture(value: Int) {
        textureStrength = value
    }

    /**
     * Like waves hitting the shore.
     */
    fun updateModulation(value: Int) {
        waveValue = value.coerceIn(0, 100)
    }

    /**
     * Makes it move around in 3d.
     */
    fun updateSpatialWidth(value: Int) {
        spatialWidth = value.coerceIn(0, 100)
    }

    /**
     * Filters rumble.
     */
    fun updateBassCut(value: Int) {
        bassCut = value.coerceIn(0, 100)
    }

    /**
     * Choppiness. Like a fast boat cutting through the ocean.
     */
    fun updateWindGusting(value: Int) {
        windGusting = value.coerceIn(0, 100)
    }

    fun playNoise() {
        if (isPlayingNoise) return
        isPlayingNoise = true

        if (audioTrack == null) {
            initAudioEngine()
        }

        audioTrack?.play()

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val tempBuffer = ShortArray(bufferSize)

        thread(start = true) {
            while (isPlayingNoise) {
                // Thread-safe local cache declarations
                val currentWarmth = filterWarmth.coerceIn(0, 99)
                val currentTexture = textureStrength.coerceIn(0, 100)
                val currentWave = waveValue
                val currentSpatial = spatialWidth.coerceIn(0, 100)
                val currentBassCut = bassCut.coerceIn(0, 100)
                val currentWind = windGusting.coerceIn(0, 100)

                // Stereo step increment processing
                for (i in 0 until tempBuffer.size step 2) {
                    if (i + 1 >= tempBuffer.size) break

                    // Spatialized Core Sources
                    val centerNoise = random.nextInt(7000 * 2) - 7000
                    val leftSideNoise = random.nextInt(7000 * 2) - 7000
                    val rightSideNoise = random.nextInt(7000 * 2) - 7000

                    val monoBlend = (100 - currentSpatial) / 100.0
                    val stereoBlend = currentSpatial / 100.0

                    var rawLeft = (centerNoise * monoBlend) + (leftSideNoise * stereoBlend)
                    var rawRight = (centerNoise * monoBlend) + (rightSideNoise * stereoBlend)

                    // Independent Transient Micro-Pops (Rain/Crackle)
                    if (currentTexture > 0) {
                        if (random.nextInt(20000) < currentTexture * 4) {
                            rawLeft += if (random.nextBoolean()) 22000 else -22000
                        }
                        if (random.nextInt(20000) < currentTexture * 4) {
                            rawRight += if (random.nextBoolean()) 22000 else -22000
                        }
                    }

                    // Bass Cut Subtraction Engine (High-Pass Filter Mapping)
                    if (currentBassCut > 0) {
                        // Isolate ultra-low rumble values via low coefficient tracking
                        val bassLowPassLeft = ((96 * lastBassLeft) + (4 * rawLeft)) / 100
                        lastBassLeft = bassLowPassLeft.toInt()
                        rawLeft -= (bassLowPassLeft * (currentBassCut / 100.0))

                        val bassLowPassRight = ((96 * lastBassRight) + (4 * rawRight)) / 100
                        lastBassRight = bassLowPassRight.toInt()
                        rawRight -= (bassLowPassRight * (currentBassCut / 100.0))
                    }

                    // Warmth Filtration (Leaky Integrator Low-Pass)
                    val filteredLeft = ((currentWarmth * lastOutputLeft) + ((100 - currentWarmth) * rawLeft)) / 100
                    lastOutputLeft = filteredLeft.toInt()

                    val filteredRight = ((currentWarmth * lastOutputRight) + ((100 - currentWarmth) * rawRight)) / 100
                    lastOutputRight = filteredRight.toInt()

                    // Multi-Envelope Generation (Waves + Wind Chaos)
                    sampleCounter++

                    var waveEnvelope = 1.0
                    if (currentWave > 0) {
                        val durationSeconds = 15.0 - ((currentWave - 1) / 99.0 * 13.8)
                        val angle = (sampleCounter * PI * 2) / (sampleRate * durationSeconds)
                        waveEnvelope = 0.925 + (sin(angle) * 0.075)
                    }

                    var windEnvelopeMod = 1.0
                    if (currentWind > 0) {
                        // Recalculate target direction roughly 20 times a second to prevent CPU overhead
                        if (sampleCounter % 2205 == 0) {
                            windTarget = 1.0 - (random.nextDouble() * (currentWind / 100.0) * 0.45)
                        }
                        // Inertial smooth tracker drift
                        windEnvelope = (0.998 * windEnvelope) + (0.002 * windTarget)
                        windEnvelopeMod = windEnvelope
                    }

                    val combinedEnvelope = waveEnvelope * windEnvelopeMod

                    // Output interleaved audio frames directly to stereo index channels
                    tempBuffer[i] = (filteredLeft * combinedEnvelope).toInt().toShort()
                    tempBuffer[i + 1] = (filteredRight * combinedEnvelope).toInt().toShort()
                }

                audioTrack?.write(tempBuffer, 0, tempBuffer.size)
            }
        }
    }

    fun deleteEngineState() {
        sampleCounter = 0
        lastOutputLeft = 0
        lastOutputRight = 0
        lastBassLeft = 0
        lastBassRight = 0
        windTarget = 1.0
        windEnvelope = 1.0
    }

    fun pauseNoise() {
        isPlayingNoise = false
        audioTrack?.apply {
            try {
                stop()
                flush()
            } catch (e: IllegalStateException) {}
        }
        deleteEngineState()
    }

    override fun onDestroy() {
        super.onDestroy()
        pauseNoise()
        audioTrack?.release()
        audioTrack = null
    }

    inner class LocalBinder : Binder() {
        fun getEngine(): WhiteNoiseEngine = this@WhiteNoiseEngine
    }
}