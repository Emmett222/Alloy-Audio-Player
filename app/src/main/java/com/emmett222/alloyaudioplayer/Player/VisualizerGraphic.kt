package com.emmett222.alloyaudioplayer.Player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Visualizer for the player. Made by Gemini. Changed slightly by Emmett Grebe.
 * Decoupled vertical layouts for wave and bar modes.
 *
 * @version 5-20-2026
 */
class VisualizerGraphic @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val TYPE_WAVE = 0
        const val TYPE_BARS = 1
    }

    private var currentType = TYPE_WAVE

    private val wavePath = Path()
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#00FF00")
    }

    private var currentGlowRadius = 14f
    private var currentLerpFactor = 0.12f

    private var targetAmplitudes = FloatArray(16)
    private var smoothedAmplitudes = FloatArray(16)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updatePaintConfiguration()
    }

    private fun updatePaintConfiguration() {
        if (currentType == TYPE_BARS) {
            wavePaint.strokeWidth = 5f
            currentGlowRadius = 8f
            wavePaint.setShadowLayer(currentGlowRadius, 0f, 0f, Color.parseColor("#008a00"))
        } else {
            wavePaint.strokeWidth = 6f
            currentGlowRadius = 14f
            wavePaint.setShadowLayer(currentGlowRadius, 0f, 0f, Color.parseColor("#008a00"))
        }
    }

    fun change(type: Int) {
        if (type == currentType) return

        currentType = type
        updatePaintConfiguration()

        val newPointsCount = if (currentType == TYPE_BARS) 32 else 16
        currentLerpFactor = if (currentType == TYPE_BARS) 0.05f else 0.12f

        targetAmplitudes = FloatArray(newPointsCount)
        smoothedAmplitudes = FloatArray(newPointsCount)

        invalidate()
    }

    fun updateWaveform(waveform: ByteArray) {
        val desiredPoints = targetAmplitudes.size
        val skipStep = waveform.size / desiredPoints
        if (skipStep < 1) return

        for (i in 0 until desiredPoints) {
            val rawIndex = i * skipStep
            if (rawIndex >= waveform.size) break

            val rawValue = (waveform[rawIndex].toInt() and 0xFF - 128) / 128f
            val windowMultiplier = sin((i.toFloat() / (desiredPoints - 1)) * PI).toFloat()

            targetAmplitudes[i] = if (currentType == TYPE_BARS) {
                abs(rawValue) * windowMultiplier
            } else {
                rawValue * windowMultiplier
            }
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pointsCount = targetAmplitudes.size
        if (pointsCount < 2) return

        val stepX = width.toFloat() / (pointsCount - 1)

        // Run common frame easing calculations
        for (i in 0 until pointsCount) {
            smoothedAmplitudes[i] = smoothedAmplitudes[i] +
                    (targetAmplitudes[i] - smoothedAmplitudes[i]) * currentLerpFactor
        }

        // Branch drawing routines with layout math customized per mode
        if (currentType == TYPE_WAVE) {
            // Wave stays anchored at 3/5ths down the canvas
            val centerY = height * (3f / 5f)
            val maxVerticalStretch = (height - centerY) * 0.85f

            drawWigglyWave(canvas, centerY, maxVerticalStretch, stepX, pointsCount)
        } else {
            // Bars are centered exactly 1/2 down the canvas
            val centerY = height * (1f / 2f)
            // Stretches evenly into the top and bottom halves with a safety cushion
            val maxVerticalStretch = centerY * 0.85f

            drawVerticalBars(canvas, centerY, maxVerticalStretch, stepX, pointsCount)
        }

        invalidate()
    }

    private fun drawWigglyWave(canvas: Canvas, centerY: Float, maxVerticalStretch: Float, stepX: Float, pointsCount: Int) {
        wavePaint.style = Paint.Style.STROKE
        wavePath.reset()
        wavePath.moveTo(0f, centerY)

        for (i in 1 until pointsCount) {
            val currentX = i * stepX
            val currentY = centerY - (smoothedAmplitudes[i] * maxVerticalStretch)

            val previousX = (i - 1) * stepX
            val previousY = centerY - (smoothedAmplitudes[i - 1] * maxVerticalStretch)

            val controlX1 = previousX + (stepX / 2f)
            val controlY1 = previousY

            val controlX2 = previousX + (stepX / 2f)
            val controlY2 = currentY

            wavePath.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
        }
        canvas.drawPath(wavePath, wavePaint)
    }

    private fun drawVerticalBars(canvas: Canvas, centerY: Float, maxVerticalStretch: Float, stepX: Float, pointsCount: Int) {
        wavePaint.style = Paint.Style.FILL_AND_STROKE

        val minHeightPercentOfStretch = 0.01f
        val maxStretchCompressionRatio = 0.55f

        for (i in 0 until pointsCount) {
            val x = i * stepX
            val compressedMagnitude = minHeightPercentOfStretch + (smoothedAmplitudes[i] * maxStretchCompressionRatio)
            val barHalfHeight = compressedMagnitude * maxVerticalStretch

            val topY = centerY - barHalfHeight
            val bottomY = centerY + barHalfHeight

            canvas.drawLine(x, topY, x, bottomY, wavePaint)
        }
    }
}