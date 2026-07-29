package com.emmett222.alloyaudioplayer.Util

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.children

/**
 * A utilities class for commonly used functions that deal with colors.
 *
 * @author Emmett Grebe
 * @version 7-29-2026
 */
object ColorUtil {
    /**
     * Brightens a color based on a specified amount.
     *
     * @param color A drawable color, must be either ColorDrawable or GradientDrawable.
     * @return A brightened color.
     */
    fun brightenColor(color: Drawable, amount : Int) : Int {
        var newR = 0
        var newG = 0
        var newB = 0
        if (color is ColorDrawable) {
            newR = (color.color.red + amount).coerceIn(0, 255)
            newG = (color.color.green + amount).coerceIn(0, 255)
            newB = (color.color.blue + amount).coerceIn(0, 255)
        }
        if (color is GradientDrawable) {
            val baseColor = color.colors?.get(0) ?: Color.BLACK
            newR = (baseColor.red + amount).coerceIn(0, 255)
            newG = (baseColor.green + amount).coerceIn(0, 255)
            newB = (baseColor.blue + amount).coerceIn(0, 255)
        }
        return Color.rgb(newR, newG, newB)
    }

    /**
     * Darkens a color based on a specified amount.
     *
     * @param color A drawable color, must be either ColorDrawable or GradientDrawable.
     * @return A darkened color.
     */
    fun darkenColor(color: Drawable, amount : Int) : Int {
        var newR = 0
        var newG = 0
        var newB = 0
        if (color is ColorDrawable) {
            newR = (color.color.red - amount).coerceIn(0, 255)
            newG = (color.color.green - amount).coerceIn(0, 255)
            newB = (color.color.blue - amount).coerceIn(0, 255)
        }
        if (color is GradientDrawable) {
            val baseColor = color.colors?.get(0) ?: Color.BLACK
            newR = (baseColor.red - amount).coerceIn(0, 255)
            newG = (baseColor.green - amount).coerceIn(0, 255)
            newB = (baseColor.blue - amount).coerceIn(0, 255)
        }
        return Color.rgb(newR, newG, newB)
    }

    /**
     * Gets the most visible text color from a color. For dark colors, returns white. For bright
     * colors, returns black.
     *
     * @param color A drawable color, must be either ColorDrawable or GradientDrawable.
     * @return White for dark colors, Black for bright colors.
     */
    fun textColorFromColor(color: Drawable) : Int {
        var luminance = 0.0
        if (color is ColorDrawable) {
            luminance = (color.color.red * 0.299) +
                    (color.color.green * 0.587) +
                    (color.color.blue * 0.114)
        }
        if (color is GradientDrawable) {
            val baseColor = color.colors?.get(0) ?: Color.BLACK
            luminance = ColorUtils.calculateLuminance(baseColor)
        }

        return if (luminance > 0.4) Color.BLACK else Color.WHITE
    }

    /**
     * Updates all text colors in a container. Recursively looks through a view and it's children
     * for TextViews to change the color.
     *
     * @param view The View to search through
     * @param newColor The color to change all the text color to.
     */
    fun updateAllTextColors(view: View, newColor: Int) {
        if (view is TextView) {
            view.setTextColor(newColor)
        }
        else if (view is ViewGroup) {
            for (child in view.children) {
                updateAllTextColors(child, newColor)
            }
        }
    }

    /**
     * Updates all accent colors in a container. Recursively looks through a view and it's children
     * for Images to change the color.
     *
     * @param view The View to search through
     * @param newColor The color to change all the accent colors to.
     */
    fun updateAllAccentColors(view: View, newColor: Int) {
        if (view is ImageView) {
            if (view.drawable != null) {
                view.setColorFilter(newColor)
            } else {
                view.background = newColor.toDrawable()
            }
        }
        else if (view is ViewGroup) {
            for (child in view.children) {
                updateAllAccentColors(child, newColor)
            }
        }
    }
}