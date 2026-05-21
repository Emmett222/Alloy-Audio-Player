package com.emmett222.alloyaudioplayer.Player.Graphic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Base graphic for the green screen on the player. This is needed to switch between graphics.
 * This is a very long class, so I have sorted it very neatly.
 *
 * @author Emmett Grebe
 * @version 5-21-2026
 */
class BaseGraphic @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    /**
     * vvvv -------------- IMMUTABLE VARIABLES -------------- vvvvv
     */
    companion object {
        const val NOTHING = 0 // Blank graphic.
        const val MENU = 1 // Menu graphic.
        const val VIS_MENU = 2 // Visualizer menu graphic.
        const val VIS_TYPE_WAVE = 3 // Wave line visualizer graphic.
        const val VIS_TYPE_BARS = 4 // Bars line visualizer graphic.
        const val VIS_TYPE_CIRCLE_WAVE = 5 // Wave circle visualizer graphic.
        const val VIS_TYPE_CIRCLE_BARS = 6 // Bars circle visualizer graphic.
        const val QUEUE = 7 // Playlist queue.
        const val FILES = 8 // Files menu.
        const val SETTINGS = 9 // Settings.
    }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#00FF00")
    }

    private val wavePath = Path()



    /**
     * vvvv -------------- MUTABLE VARIABLES -------------- vvvvv
     */
    public var currentType: Int = 0
    private var currentGlowRadius = 14f
    private var currentLerpFactor = 0.12f
    private var targetAmplitudes = FloatArray(16)
    private var smoothedAmplitudes = FloatArray(16)
    private var dynamicVolumeMultiplier = 1.0f
    private var targetVolumeMultiplier = 1.0f





    /**
     * vvvvv -------------- PUBLIC FUNCTIONS -------------- vvvvv
     */
    /**
     * Change the current screen.
     *
     * @param screenType The type of screen to use. Use this class's companion constants to set the
     * screen.
     */
    public fun changeScreen(screenType: Int) {
        if (currentType == screenType) return
        currentType = screenType

        updatePaintConfiguration()

        // Points on the graphic.
        var newPointsCount = 0
        when (currentType) {
            VIS_TYPE_BARS -> newPointsCount = 32
            VIS_TYPE_CIRCLE_BARS -> newPointsCount = 80
            VIS_TYPE_CIRCLE_WAVE -> newPointsCount = 96
            else -> newPointsCount = 16
        }

        currentLerpFactor = if (currentType == VIS_TYPE_BARS) 0.05f else 0.12f

        targetAmplitudes = FloatArray(newPointsCount)
        smoothedAmplitudes = FloatArray(newPointsCount)

        invalidate()
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updatePaintConfiguration()
    }

    /**
     * Updates the waveform based on new data.
     * Made by Gemini. Refactored to fit into this class by Emmett.
     *
     * @param waveform ByteArray to base the waveform off of.
     */
    public fun updateWaveform(waveform: ByteArray) {
        val desiredPoints = targetAmplitudes.size
        val skipStep = waveform.size / desiredPoints
        if (skipStep < 1) return

        // Compute baseline root energy metrics
        var totalChunkEnergy = 0f
        for (b in waveform) {
            totalChunkEnergy += abs((b.toInt() and 0xFF - 128) / 128f)
        }
        val averageEnergy = if (waveform.isNotEmpty()) totalChunkEnergy / waveform.size else 0f

        // Define clean threshold limits to bypass system AGC
        val noiseFloor = 0.06f       // Any energy below this is treated as quiet air
        val maxExpectedEnergy = 0.26f // Real-world peak energy threshold for standard music mixes

        // Map the raw energy value into a highly reactive, clamped 0.0 to 1.0 range index
        val normalizedEnergy = ((averageEnergy - noiseFloor) / (maxExpectedEnergy - noiseFloor)).coerceIn(0f, 1f)

        // Cubic Curve scaling creates wide contrast gaps between low-level noise and true beat hits
        val powerEnergy = normalizedEnergy * normalizedEnergy * normalizedEnergy

        // Quiet parts drop to a baseline size of 0.1, while beat drops scale up to 2.6
        targetVolumeMultiplier = 0.1f + (powerEnergy * 2.5f)

        for (i in 0 until desiredPoints) {
            val rawIndex = i * skipStep
            if (rawIndex >= waveform.size) break

            val rawValue = (waveform[rawIndex].toInt() and 0xFF - 128) / 128f
            val isLinearMode = currentType == VIS_TYPE_WAVE || currentType == VIS_TYPE_BARS
            val windowMultiplier = if (isLinearMode) sin((i.toFloat() / (desiredPoints - 1)) * PI).toFloat() else 1.0f

            // Inject the energy normalization straight into the individual node positions
            // This suppresses jittery micro-wiggles during quiet acoustic movements
            val attenuationFactor = 0.15f + (normalizedEnergy * 0.85f)

            targetAmplitudes[i] = if (currentType == VIS_TYPE_BARS || currentType == VIS_TYPE_CIRCLE_BARS) {
                abs(rawValue) * windowMultiplier * attenuationFactor
            } else {
                rawValue * windowMultiplier * attenuationFactor
            }
        }

        postInvalidateOnAnimation()
    }



    /**
     * vvvvv -------------- PRIVATE FUNCTIONS -------------- vvvvv
     */
    private fun updatePaintConfiguration() {
        if (currentType == VIS_TYPE_BARS) {
            wavePaint.strokeWidth = 5f
            currentGlowRadius = 8f
            wavePaint.setShadowLayer(currentGlowRadius, 0f, 0f, Color.parseColor("#008a00"))
        } else {
            wavePaint.strokeWidth = 6f
            currentGlowRadius = 14f
            wavePaint.setShadowLayer(currentGlowRadius, 0f, 0f, Color.parseColor("#008a00"))
        }
    }



    /**
     * vvvvv -------------- DRAWING FUNCTIONS -------------- vvvvv
     */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pointsCount = targetAmplitudes.size
        if (pointsCount < 2) return
        var isAnimating = false

        // Enhanced Easing Equation: Snaps out instantly on beats (0.35f), recedes gracefully (0.10f)
        val volumeLerp = if (targetVolumeMultiplier > dynamicVolumeMultiplier) 0.35f else 0.10f
        dynamicVolumeMultiplier += (targetVolumeMultiplier - dynamicVolumeMultiplier) * volumeLerp

        // Common frame easing calculations
        for (i in 0 until pointsCount) {
            val delta = targetAmplitudes[i] - smoothedAmplitudes[i]
            smoothedAmplitudes[i] += delta * currentLerpFactor

            // If any value hasn't settled yet, flag that we need another frame pass
            if (abs(delta) > 0.001f) {
                isAnimating = true
            }
        }

        // Always animate if the volume envelope tracker is still moving
        if (abs(targetVolumeMultiplier - dynamicVolumeMultiplier) > 0.01f) {
            isAnimating = true
        }

        // When is the Kotlin equivalent of a Switch statement.
        when (currentType) {
            NOTHING -> {}
            MENU -> {drawMenu(canvas)}
            VIS_MENU -> {}
            VIS_TYPE_WAVE -> {drawVisWaveLine(canvas)}
            VIS_TYPE_BARS -> {drawVisBarsLine(canvas)}
            VIS_TYPE_CIRCLE_WAVE -> {drawVisCircleWave(canvas)}
            VIS_TYPE_CIRCLE_BARS -> {drawVisCircleBars(canvas)}
            QUEUE -> {}
            FILES -> {}
            SETTINGS -> {}
        }

        if (isAnimating) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawMenu(canvas: Canvas) {
    }

    /**
     * Draws the wave line visualizer. Looks like a wave.
     * Math for drawing this made by Gemini. Refactored to fit into this class by Emmett.
     *
     * @param canvas The canvas to be drawn on. Usually passed from onDraw()'s canvas.
     */
    private fun drawVisWaveLine(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        // Wave stays anchored at 3/5ths down the canvas
        val centerY = height * (4f / 5f)
        val maxVerticalStretch = (height - centerY) * 0.70f * dynamicVolumeMultiplier
        val stepX = width.toFloat() / (pointsCount - 1)

        // Run common frame easing calculations
        for (i in 0 until pointsCount) {
            smoothedAmplitudes[i] = smoothedAmplitudes[i] +
                    (targetAmplitudes[i] - smoothedAmplitudes[i]) * currentLerpFactor
        }

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

    /**
     * Draws the bars line visualizer. Looks like a wave.
     * Math for drawing this made by Gemini. Refactored to fit into this class by Emmett.
     *
     * @param canvas The canvas to be drawn on. Usually passed from onDraw()'s canvas.
     */
    private fun drawVisBarsLine(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        val stepX = width.toFloat() / (pointsCount - 1)
        // Bars are centered exactly 1/2 down the canvas
        val centerY = height * (1f / 2f)

        // Scale overall height by the dynamic volume multiplier
        val maxVerticalStretch = centerY * 0.70f * dynamicVolumeMultiplier

        // Run common frame easing calculations
        for (i in 0 until pointsCount) {
            smoothedAmplitudes[i] = smoothedAmplitudes[i] +
                    (targetAmplitudes[i] - smoothedAmplitudes[i]) * currentLerpFactor
        }

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

    /**
     * Draws a continuous, looping wiggly line around a central ring.
     * Math for drawing this made by Gemini. Refactored to fit into this class by Emmett.
     *
     * @param canvas The canvas to be drawn on. Usually passed from onDraw()'s canvas.
     */
    private fun drawVisCircleWave(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size

        wavePaint.style = Paint.Style.STROKE
        wavePaint.strokeWidth = 6f
        wavePaint.setShadowLayer(currentGlowRadius, 0f, 0f, Color.parseColor("#008a00"))

        wavePath.reset()

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = min(width, height) * 0.25f

        val maxStretch = baseRadius * 0.25f * dynamicVolumeMultiplier

        // Loop to pointsCount inclusive so the final line segment joins back to the start seamlessly
        for (i in 0..pointsCount) {
            val index = i % pointsCount
            val angle = (i * 2 * PI / pointsCount).toFloat()

            // Modulate the radius using the signed amplitude value (allows inward and outward wiggles)
            val currentRadius = baseRadius + (smoothedAmplitudes[index] * maxStretch)

            val x = cx + currentRadius * cos(angle)
            val y = cy + currentRadius * sin(angle)

            if (i == 0) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }

        canvas.drawPath(wavePath, wavePaint)
    }

    /**
     * Draws the radial circle bars visualizer projecting outwards from a central ring.
     * Math for drawing this made by Gemini. Refactored to fit into this class by Emmett.
     *
     * @param canvas The canvas to be drawn on. Usually passed from onDraw()'s canvas.
     */
    private fun drawVisCircleBars(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        wavePaint.strokeWidth = 12f
        wavePaint.style = Paint.Style.STROKE
        wavePaint.setShadowLayer(currentGlowRadius, 0f, 0f, Color.parseColor("#00FF00"))

        // Find center points and calculate a safe bounding layout footprint
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = min(width, height) * 0.25f
        // Scale outward bar length limits by the dynamic volume multiplier
        val maxBarLength = baseRadius * 0.50f * dynamicVolumeMultiplier

        // Draw the inner solid reference ring layer
        canvas.drawCircle(cx, cy, baseRadius, wavePaint)

        // Subtle range compression modifiers specific to circle rendering
        val minHeightRatio = 0.02f
        val maxCompressionRatio = 0.80f

        for (i in 0 until pointsCount) {
            // Distribute angles evenly across 360 degrees
            val angle = (i * 2 * PI / pointsCount).toFloat()

            val cosAngle = cos(angle)
            val sinAngle = sin(angle)

            val compressedMagnitude = minHeightRatio + (smoothedAmplitudes[i] * maxCompressionRatio)
            val currentBarLength = compressedMagnitude * maxBarLength

            // Start point sits directly on the ring's edge
            val startX = cx + baseRadius * cosAngle
            val startY = cy + baseRadius * sinAngle

            // End point shoots directly outward along the radial line vector
            val endX = cx + (baseRadius + currentBarLength) * cosAngle
            val endY = cy + (baseRadius + currentBarLength) * sinAngle

            canvas.drawLine(startX, startY, endX, endY, wavePaint)
        }
    }
}