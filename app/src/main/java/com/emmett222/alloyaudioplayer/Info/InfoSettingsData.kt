package com.emmett222.alloyaudioplayer.Info

/**
 * Holds strings for settings information pop-ups.
 *
 * @author Emmett Grebe
 * @version 8-4-2026
 */
object InfoSettingsData {
    const val TITLE_TEXT_COLOR = "Text color:"
    const val TITLE_BG_COLOR = "Background color:"
    const val TITLE_ACC_COLOR = "Accent color:"
    const val TITLE_ANIM = "Animations:"
    const val TITLE_SORT = "Sort file names:"
    const val TITLE_SORT_BY = "Sort by:"
    const val TITLE_SHORT = "Shorten file names:"
    const val TITLE_REPEAT = "Always repeat playlist:"
    const val TITLE_SHUFF = "Always shuffle:"
    const val TITLE_VIS = "Visualizer:"

    const val BODY_TEXT_COLOR = "Color 1 changes the color of all text."
    const val BODY_BG_COLOR = "Color 2 changes the background color of screens."
    const val BODY_ACC_COLOR = "Color 3 changes the accent colors. Accent colors are icons and dividers. If there are no colors selected for the visualizers, color 3 is used."
    const val BODY_ANIM = "Animations are shown in the file select screen."
    const val BODY_SORT = "This option changes how files are sorted everywhere they are displayed together. Default uses your file systems normal placement of files."
    const val BODY_SORT_BY = "This option makes the sort go from start to finish, or finish to start."
    const val BODY_SHORT = "This option will shorten file names in all places files are shown. For example, a file named \"My favorite song (128KBIT_AAC).mp3\" will be shown as \"My favorite song\"."
    const val BODY_REPEAT = "This option makes the repeat playlist always start in the on position in the player when selecting a song. It can be turned off in the player, but will always turn itself back on when a new song is selected in the file select screen."
    const val BODY_SHUFF = "This option makes shuffle always start in the on position in the player when selecting a song. It can be turned off in the player, but will always turn itself back on when a new song is selected in the file select screen."
    const val BODY_VIS = "Visualizers move to the audio being played."

    const val BAT_NONE = "Visualizers move to the audio being played."
    const val BAT_SLIGHT = "This effects battery usage slightly."
    const val BAT_ANIM = "Animations slightly effect battery usage. Turn them off to save battery."
    const val BAT_VIS = "Visualizers effect battery a lot. Turn them off to save battery. Visualizers that have less moving parts than others use less battery."
}
