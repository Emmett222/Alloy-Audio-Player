package com.emmett222.alloyaudioplayer.Background

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Binder
import android.os.IBinder
import androidx.media3.session.DefaultMediaNotificationProvider
import com.emmett222.alloyaudioplayer.R
import java.util.Random
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * White noise background engine for Alloy Audio Player. Playing white noise is run on a background
 * thread to not lock everything else up.
 * Math for white noise made by Gemini.
 *
 * @author Emmett Grebe
 * @version 5-27-2026
 */
class WhiteNoiseEngine : Service() {
    // Volatile so it can be visible to other threads.
    // This boolean is like the kill switch for other threads to tell it to stop.
    private val binder = LocalBinder()
    private var audioTrack: AudioTrack? = null

    @Volatile
    public var isPlayingNoise = false
    private val sampleRate = 44100

    private var filterWarmth = 0
    private var sampleHoldFactor = 1
    private var waveDuration = 0

    private var sampleCounter = 0
    private var lastOutputSample = 0
    private var heldSample = 0
    private var holdCounter = 0
    private val random = Random()

    /**
     * Runs when it is first made.
     */
    override fun onBind(p0: Intent?): IBinder? {
        initAudioEngine()
        return binder
    }

    /**
     * Initiates the audio engine that plays the white noise. Sets these things:
     * - Usage: USAGE_MEDIA (So it is treated as media.)
     * - Content Type: CONTENT_TYPE_MUSIC (So it is treated as music.)
     * - Encoding: ENCODING_PCM_16BIT (16 bit per sample.)
     * - Sample rate: 44100
     * - Channel Mask: CHANNEL_OUT_MONO
     * - Transfer mode: MODE_STREAM (Stream so it is constantly new noise.)
     */
    public fun initAudioEngine() {
        // v This is needed to tell Android what hardware and how much memory it needs.
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder() // Create the AudioTrack using the builder.
            .setAudioAttributes( // Set the attributes to:
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA) // Usage media so it is treated as media.
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC) // Same as ^
                    .build()
            )
            .setAudioFormat( // Set the audio format to:
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT) // 16 bit per sample.
                    .setSampleRate(sampleRate) // Sample rate at top of class.
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO) // Mono audio.
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM) // Stream so it is constantly new noise.
            .build()
    }

    fun updateWarmth(value: Int) {
        filterWarmth = value
    }

    fun updateTexture(value: Int) {
        sampleHoldFactor = value
    }

    fun updateModulation(seconds: Int) {
        waveDuration = seconds
    }

    /**
     * Plays the white noise if it is currently not playing.
     * Math made by Gemini.
     */
    fun playNoise() {
        if (isPlayingNoise) return
        isPlayingNoise = true

        if (audioTrack == null) {
            initAudioEngine()
        }

        audioTrack?.play()

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val tempBuffer = ShortArray(bufferSize)

        thread(start = true) {
            while (isPlayingNoise) {
                val currentWarmth = filterWarmth.coerceIn(0, 98)
                val currentHold = sampleHoldFactor.coerceIn(1, 30)
                val currentWaveDuration = waveDuration

                for (i in tempBuffer.indices) {

                    if (holdCounter >= currentHold) {
                        heldSample = random.nextInt(30000 * 2) - 30000
                        holdCounter = 0
                    }
                    holdCounter++

                    val filtered = ((currentWarmth * lastOutputSample) + ((100 - currentWarmth) * heldSample)) / 100
                    lastOutputSample = filtered

                    var envelope = 1.0
                    if (currentWaveDuration > 0) {
                        sampleCounter++
                        val angle = (sampleCounter * PI * 2) / (sampleRate * currentWaveDuration)
                        envelope = (sin(angle) + 1.0) / 2.0
                    }

                    tempBuffer[i] = (filtered * envelope).toInt().toShort()
                }

                audioTrack?.write(tempBuffer, 0, tempBuffer.size)
            }
        }
    }
    /**
     * Stops the white noise if it is currently playing.
     */
    fun pauseNoise() {
        isPlayingNoise = false
        audioTrack?.apply {
            try {
                stop()
                flush()
            } catch (e: IllegalStateException) {
                // Track was uninitialized or stopped early
            }
        }
        sampleCounter = 0
        lastOutputSample = 0
        heldSample = 0
        holdCounter = 0
    }

    /**
     * Needed to get the binder for the onBind stuff.
     */
    inner class LocalBinder : Binder() {
        fun getEngine(): WhiteNoiseEngine = this@WhiteNoiseEngine
    }
}