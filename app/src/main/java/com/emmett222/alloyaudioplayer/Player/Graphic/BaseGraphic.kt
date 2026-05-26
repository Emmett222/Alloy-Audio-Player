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
 * Fully optimized with headroom limits and an elastic, tamed wavy talking smiley face.
 *
 * @author Emmett Grebe
 * @version 5-26-2026
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
        const val NOTHING = 0
        const val MENU = 1
        const val VIS_MENU = 2
        const val VIS_TYPE_WAVE = 3
        const val VIS_TYPE_BARS = 4
        const val VIS_TYPE_CIRCLE_WAVE = 5
        const val VIS_TYPE_CIRCLE_BARS = 6
        const val VIS_TYPE_BOTTOM_BARS = 7
        const val VIS_TYPE_CIRCLE_GROW = 8
        const val VIS_TYPE_MIRROR_WAVE = 9
        const val VIS_TYPE_SMILEY = 10
        const val QUEUE = 11
        const val FILES = 12
        const val SETTINGS = 13
    }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#00FF00")
    }

    private val wavePath = Path()
    private val mirrorPath = Path()

    /**
     * vvvv -------------- MUTABLE VARIABLES -------------- vvvvv
     */
    var currentType: Int = 0
    private var currentGlowRadius = 14f
    private var currentLerpFactor = 0.12f
    private var targetAmplitudes = FloatArray(16)
    private var smoothedAmplitudes = FloatArray(16)
    private var dynamicVolumeMultiplier = 1.0f
    private var targetVolumeMultiplier = 1.0f

    /**
     * vvvvv -------------- PUBLIC FUNCTIONS -------------- vvvvv
     */
    @Synchronized
    fun changeScreen(screenType: Int) {
        if (currentType == screenType) return
        currentType = screenType

        updatePaintConfiguration()

        var newPointsCount = 0
        when (currentType) {
            VIS_TYPE_BARS, VIS_TYPE_BOTTOM_BARS -> newPointsCount = 32
            VIS_TYPE_CIRCLE_BARS, VIS_TYPE_CIRCLE_WAVE -> newPointsCount = 96
            VIS_TYPE_CIRCLE_GROW -> newPointsCount = 48
            VIS_TYPE_SMILEY -> newPointsCount = 64
            else -> newPointsCount = 16
        }

        currentLerpFactor = if (currentType == VIS_TYPE_BARS || currentType == VIS_TYPE_BOTTOM_BARS) 0.05f else 0.12f

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
     */
    @Synchronized
    fun updateWaveform(waveform: ByteArray) {
        val desiredPoints = targetAmplitudes.size
        val skipStep = waveform.size / desiredPoints
        if (skipStep < 1) return

        var totalChunkEnergy = 0f
        for (b in waveform) {
            totalChunkEnergy += abs((b.toInt() and 0xFF - 128) / 128f)
        }
        val averageEnergy = if (waveform.isNotEmpty()) totalChunkEnergy / waveform.size else 0f

        val noiseFloor = 0.06f
        val maxExpectedEnergy = 0.26f

        val normalizedEnergy = ((averageEnergy - noiseFloor) / (maxExpectedEnergy - noiseFloor)).coerceIn(0f, 1f)
        val powerEnergy = normalizedEnergy * normalizedEnergy * normalizedEnergy

        targetVolumeMultiplier = 0.1f + (powerEnergy * 2.5f)

        for (i in 0 until desiredPoints) {
            val rawIndex = i * skipStep
            if (rawIndex >= waveform.size) break

            val rawValue = (waveform[rawIndex].toInt() and 0xFF - 128) / 128f

            val isLinearMode = currentType == VIS_TYPE_WAVE || currentType == VIS_TYPE_BARS || currentType == VIS_TYPE_BOTTOM_BARS || currentType == VIS_TYPE_MIRROR_WAVE
            val windowMultiplier = if (isLinearMode) sin((i.toFloat() / (desiredPoints - 1)) * PI).toFloat() else 1.0f
            val attenuationFactor = 0.15f + (normalizedEnergy * 0.85f)

            val wantsAbsolute = currentType == VIS_TYPE_BARS || currentType == VIS_TYPE_CIRCLE_BARS || currentType == VIS_TYPE_BOTTOM_BARS || currentType == VIS_TYPE_CIRCLE_GROW
            targetAmplitudes[i] = if (wantsAbsolute) {
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
        val isBarLayout = currentType == VIS_TYPE_BARS || currentType == VIS_TYPE_BOTTOM_BARS || currentType == VIS_TYPE_CIRCLE_BARS
        if (isBarLayout) {
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
    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pointsCount = targetAmplitudes.size
        if (pointsCount < 2) return
        var isAnimating = false

        val volumeLerp = if (targetVolumeMultiplier > dynamicVolumeMultiplier) 0.35f else 0.10f
        dynamicVolumeMultiplier += (targetVolumeMultiplier - dynamicVolumeMultiplier) * volumeLerp

        for (i in 0 until pointsCount) {
            val delta = targetAmplitudes[i] - smoothedAmplitudes[i]
            smoothedAmplitudes[i] += delta * currentLerpFactor

            if (abs(delta) > 0.001f) {
                isAnimating = true
            }
        }

        if (abs(targetVolumeMultiplier - dynamicVolumeMultiplier) > 0.01f) {
            isAnimating = true
        }

        when (currentType) {
            MENU -> { drawMenu(canvas) }
            VIS_TYPE_WAVE -> { drawVisWaveLine(canvas) }
            VIS_TYPE_BARS -> { drawVisBarsLine(canvas) }
            VIS_TYPE_CIRCLE_WAVE -> { drawVisCircleWave(canvas) }
            VIS_TYPE_CIRCLE_BARS -> { drawVisCircleBars(canvas) }
            VIS_TYPE_BOTTOM_BARS -> { drawVisBottomBars(canvas) }
            VIS_TYPE_CIRCLE_GROW -> { drawVisConcentricRings(canvas) }
            VIS_TYPE_MIRROR_WAVE -> { drawVisMirrorWave(canvas) }
            VIS_TYPE_SMILEY -> { drawVisSmiley(canvas) }
            NOTHING, VIS_MENU, QUEUE, FILES, SETTINGS -> {}
        }

        if (isAnimating) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawMenu(canvas: Canvas) {}

    private fun drawVisWaveLine(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        val centerY = height * (4f / 5f)
        val stepX = width.toFloat() / (pointsCount - 1)
        val maxVerticalStretch = centerY * 0.35f * dynamicVolumeMultiplier

        wavePaint.style = Paint.Style.STROKE
        wavePath.reset()
        wavePath.moveTo(0f, centerY)

        for (i in 1 until pointsCount) {
            val currentX = i * stepX
            val currentY = centerY - (smoothedAmplitudes[i] * maxVerticalStretch)
            val previousX = (i - 1) * stepX
            val previousY = centerY - (smoothedAmplitudes[i - 1] * maxVerticalStretch)

            wavePath.cubicTo(previousX + (stepX / 2f), previousY, previousX + (stepX / 2f), currentY, currentX, currentY)
        }
        canvas.drawPath(wavePath, wavePaint)
    }

    private fun drawVisBarsLine(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        val stepX = width.toFloat() / (pointsCount - 1)
        val centerY = height * (1f / 2f)
        val maxVerticalStretch = centerY * 0.35f * dynamicVolumeMultiplier

        wavePaint.style = Paint.Style.FILL_AND_STROKE
        val minHeightPercentOfStretch = 0.01f
        val maxStretchCompressionRatio = 0.55f

        for (i in 0 until pointsCount) {
            val x = i * stepX
            val compressedMagnitude = minHeightPercentOfStretch + (smoothedAmplitudes[i] * maxStretchCompressionRatio)
            val barHalfHeight = compressedMagnitude * maxVerticalStretch

            canvas.drawLine(x, centerY - barHalfHeight, x, centerY + barHalfHeight, wavePaint)
        }
    }

    private fun drawVisBottomBars(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        val stepX = width.toFloat() / (pointsCount - 1)
        val bottomY = height.toFloat()
        val maxVerticalStretch = height * 0.35f * dynamicVolumeMultiplier

        wavePaint.style = Paint.Style.FILL_AND_STROKE
        val minHeightPercent = 0.02f
        val maxCompression = 0.75f

        for (i in 0 until pointsCount) {
            val x = i * stepX
            val compressedMagnitude = minHeightPercent + (smoothedAmplitudes[i] * maxCompression)
            val topY = bottomY - (compressedMagnitude * maxVerticalStretch)

            canvas.drawLine(x, bottomY, x, topY, wavePaint)
        }
    }

    private fun drawVisConcentricRings(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        wavePaint.style = Paint.Style.STROKE
        wavePaint.strokeWidth = 5f

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = min(width, height) * 0.40f

        val bandSize = pointsCount / 3
        var bassSum = 0f
        var midSum = 0f
        var trebleSum = 0f

        for (i in 0 until bandSize) {
            bassSum += smoothedAmplitudes[i]
            midSum += smoothedAmplitudes[i + bandSize]
            trebleSum += smoothedAmplitudes[i + (bandSize * 2)]
        }

        val avgBass = (bassSum / bandSize).coerceIn(0f, 1f)
        val avgMid = (midSum / bandSize).coerceIn(0f, 1f)
        val avgTreble = (trebleSum / bandSize).coerceIn(0f, 1f)

        val innerRadius = maxRadius * 0.30f + (avgBass * maxRadius * 0.25f * dynamicVolumeMultiplier)
        canvas.drawCircle(cx, cy, innerRadius, wavePaint)

        val midRadius = maxRadius * 0.60f + (avgMid * maxRadius * 0.20f * dynamicVolumeMultiplier)
        canvas.drawCircle(cx, cy, midRadius, wavePaint)

        val outerRadius = maxRadius * 0.85f + (avgTreble * maxRadius * 0.15f * dynamicVolumeMultiplier)
        canvas.drawCircle(cx, cy, outerRadius, wavePaint)
    }

    private fun drawVisMirrorWave(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        val centerY = height * (1f / 2f)
        val stepX = width.toFloat() / (pointsCount - 1)
        val maxVerticalStretch = centerY * 0.35f * dynamicVolumeMultiplier

        wavePaint.style = Paint.Style.STROKE
        wavePath.reset()
        mirrorPath.reset()

        wavePath.moveTo(0f, centerY)
        mirrorPath.moveTo(0f, centerY)

        for (i in 1 until pointsCount) {
            val currentX = i * stepX
            val offset = smoothedAmplitudes[i] * maxVerticalStretch
            val currentTopY = centerY - offset
            val currentBottomY = centerY + offset

            val previousX = (i - 1) * stepX
            val prevOffset = smoothedAmplitudes[i - 1] * maxVerticalStretch
            val previousTopY = centerY - prevOffset
            val previousBottomY = centerY + prevOffset

            val cpX1 = previousX + (stepX / 2f)
            wavePath.cubicTo(cpX1, previousTopY, cpX1, currentTopY, currentX, currentTopY)
            mirrorPath.cubicTo(cpX1, previousBottomY, cpX1, currentBottomY, currentX, currentBottomY)
        }

        canvas.drawPath(wavePath, wavePaint)
        canvas.drawPath(mirrorPath, wavePaint)
    }

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

        for (i in 0..pointsCount) {
            val index = i % pointsCount
            val angle = (i * 2 * PI / pointsCount).toFloat()
            val currentRadius = baseRadius + (smoothedAmplitudes[index] * maxStretch)

            val x = cx + currentRadius * cos(angle)
            val y = cy + currentRadius * sin(angle)

            if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
        }
        canvas.drawPath(wavePath, wavePaint)
    }

    private fun drawVisCircleBars(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        wavePaint.strokeWidth = 12f
        wavePaint.style = Paint.Style.STROKE
        wavePaint.setShadowLayer(currentGlowRadius, 0f, 0f, Color.parseColor("#00FF00"))

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = min(width, height) * 0.25f
        val maxBarLength = baseRadius * 0.50f * dynamicVolumeMultiplier

        canvas.drawCircle(cx, cy, baseRadius, wavePaint)
        val minHeightRatio = 0.02f
        val maxCompressionRatio = 0.80f

        for (i in 0 until pointsCount) {
            val angle = (i * 2 * PI / pointsCount).toFloat()
            val cosAngle = cos(angle)
            val sinAngle = sin(angle)

            val compressedMagnitude = minHeightRatio + (smoothedAmplitudes[i] * maxCompressionRatio)
            val currentBarLength = compressedMagnitude * maxBarLength

            canvas.drawLine(
                cx + baseRadius * cosAngle,
                cy + baseRadius * sinAngle,
                cx + (baseRadius + currentBarLength) * cosAngle,
                cy + (baseRadius + currentBarLength) * sinAngle,
                wavePaint
            )
        }
    }

    /**
     * Draws a line-art smiley face whose open mouth morphs and wiggles dynamically with audio.
     */
    private fun drawVisSmiley(canvas: Canvas) {
        val pointsCount = targetAmplitudes.size
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = min(width, height) * 0.35f

        wavePaint.style = Paint.Style.STROKE
        wavePaint.strokeWidth = 6f
        wavePaint.setShadowLayer(currentGlowRadius, 0f, 0f, Color.parseColor("#008a00"))

        // 1. Draw outer head profile
        canvas.drawCircle(cx, cy, baseRadius, wavePaint)

        // 2. Draw eye line rings
        val eyeOffsetX = baseRadius * 0.35f
        val eyeOffsetY = baseRadius * 0.20f
        val eyeRadius = baseRadius * 0.08f
        canvas.drawCircle(cx - eyeOffsetX, cy - eyeOffsetY, eyeRadius, wavePaint)
        canvas.drawCircle(cx + eyeOffsetX, cy - eyeOffsetY, eyeRadius, wavePaint)

        // 3. Draw an elastic, closed-loop wavy mouth layout with scaled bounds
        wavePath.reset()

        val rx = baseRadius * 0.50f
        val ry = baseRadius * 0.08f

        // Loop completely around 360 degrees inclusive to seamlessly close the loop
        for (i in 0..pointsCount) {
            val index = i % pointsCount
            val angle = (i * 2 * PI / pointsCount).toFloat()

            val cosAngle = cos(angle)
            val sinAngle = sin(angle)

            // Parabolic mapping forces the oval base structure to warp into a smiling arc expression
            val smileBend = (1f - cosAngle * cosAngle) * (baseRadius * 0.16f)

            // THE FIX: Tamed macro expansion down to 0.6f and micro wiggles to 0.07f to keep mouth slim
            val dynamicRy = (ry * (0.1f + dynamicVolumeMultiplier * 0.6f)) + (smoothedAmplitudes[index] * baseRadius * 0.07f)

            val x = cx + rx * cosAngle
            val y = cy + (baseRadius * 0.20f) + smileBend + (dynamicRy * sinAngle)

            if (i == 0) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }
        canvas.drawPath(wavePath, wavePaint)
    }
}