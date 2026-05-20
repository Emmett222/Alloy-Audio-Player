package com.emmett222.alloyaudioplayer.Player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/**
 * Visualizer for the player. Made by Gemini. Changed slightly by Emmett Grebe.
 *
 * @version 5-20-2025
 */
class VisualizerGraphic @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wavePath = Path()
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f // Slightly thicker line to match your image style
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#00FF00")

        // Hardware glow
        setShadowLayer(14f, 0f, 0f, Color.parseColor("#008a00"))
    }

    // This stretches the wave cycles horizontally.
    private val desiredPoints = 8
    private var targetAmplitudes = FloatArray(desiredPoints)
    private var smoothedAmplitudes = FloatArray(desiredPoints)

    // Lower numbers (like 0.1) make the wave glide even smoother
    private val lerpFactor = 0.12f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * Call this directly from your Activity's onWaveFormDataCapture callback.
     */
    fun updateWaveform(waveform: ByteArray) {
        // Calculate the stride needed to step through the raw 1024 buffer down to 64 buckets
        val skipStep = waveform.size / desiredPoints
        if (skipStep < 1) return

        for (i in 0 until desiredPoints) {
            // Pick an evenly spaced index out of the raw audio wave array
            val rawIndex = i * skipStep
            if (rawIndex >= waveform.size) break

            // 1. Convert unsigned byte (-128 to 127) into a normalized float (-1f to 1f)
            val rawValue = (waveform[rawIndex].toInt() and 0xFF - 128) / 128f

            // 2. Edge Attenuation: Pins the absolute start and end cleanly to the horizon
            val windowMultiplier = sin((i.toFloat() / (desiredPoints - 1)) * PI).toFloat()

            targetAmplitudes[i] = rawValue * windowMultiplier
        }

        // Force the main UI thread to re-run onDraw() immediately
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pointsCount = targetAmplitudes.size
        if (pointsCount < 2) return

        // THE FIX: Shift the horizon baseline to 1/3rd up from the bottom (2/3rds down from the top)
        val centerY = height * (3f / 5f)

        // SAFETY FIX: Calculate stretch space based on the remaining room below the horizon line
        // This leaves a 15% safety buffer so the wave never hits the physical bottom edge
        val maxVerticalStretch = (height - centerY) * 0.85f

        val stepX = width.toFloat() / (pointsCount - 1)

        wavePath.reset()
        wavePath.moveTo(0f, centerY)

        // Run the interpolation and draw the broad Bezier curves across the screen
        for (i in 0 until pointsCount) {
            // Smoothly ease current position toward the target value to eliminate flickering
            smoothedAmplitudes[i] = smoothedAmplitudes[i] +
                    (targetAmplitudes[i] - smoothedAmplitudes[i]) * lerpFactor

            if (i == 0) continue

            // Coordinates for our current target node
            val currentX = i * stepX
            val currentY = centerY - (smoothedAmplitudes[i] * maxVerticalStretch)

            // Coordinates of the previous point we left behind
            val previousX = (i - 1) * stepX
            val previousY = centerY - (smoothedAmplitudes[i - 1] * maxVerticalStretch)

            // Cubic Bezier: Control points placed halfway horizontally between steps
            val controlX1 = previousX + (stepX / 2f)
            val controlY1 = previousY

            val controlX2 = previousX + (stepX / 2f)
            val controlY2 = currentY

            wavePath.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
        }

        // Draw the assembled path onto the view canvas
        canvas.drawPath(wavePath, wavePaint)

        // Keep the rendering engine processing smoothly until the lines fully rest
        invalidate()
    }
}