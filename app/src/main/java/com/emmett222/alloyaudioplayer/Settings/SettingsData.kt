package com.emmett222.alloyaudioplayer.Settings

import androidx.annotation.DrawableRes
import com.emmett222.alloyaudioplayer.R

/**
 * Stores the constant variables for settings.
 *
 * @author Emmett Grebe
 * @version 8-16-2026
 */
enum class AnimationType(val id: Int, val label: String) {
    LEFT(0, "Left"),
    RIGHT(1, "Right"),
    NONE(2, "None")
}

enum class SortType(val id: Int, val label: String) {
    DEFAULT(0, "Default"),
    ALPHABETICAL(1, "Alphabetical"),
    AUTHOR(2, "Author"),
    LENGTH(3, "Length"),
    RANDOM(4, "Random")
}

enum class SortDirection(val id: Int, val label: String) {
    ASCENDING(0, "Ascending"),
    DESCENDING(1, "Descending")
}

enum class VisualizerType(val id: Int, val label: String, val icon: Int) {
    NOVIS(0, "No Visualizer", R.drawable.menu_vis_novis),
    LINEWAVE(0, "Waves", R.drawable.menu_vis_linewave),
    MIRLINEWAVE(0, "Mirrored Waves", R.drawable.menu_vis_mirlinewave),
    LINEBARS(0, "Middle Bars", R.drawable.menu_vis_linebars),
    BOTLINEBARS(0, "Bottom Bars", R.drawable.menu_vis_botlinebars),
    CIRCLEWAVE(0, "Circle Waves", R.drawable.menu_vis_circlewave),
    CIRCLEBAR(0, "Circle Bars", R.drawable.menu_vis_circlebar),
    CIRCLEGROW(0, "Growing Circle", R.drawable.menu_vis_circlegrow),
    TALKINGSMILEY(0, "Smiley face", R.drawable.menu_vis_talkingsmiley)
}