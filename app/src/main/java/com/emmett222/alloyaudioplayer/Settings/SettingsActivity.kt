package com.emmett222.alloyaudioplayer.Settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.emmett222.alloyaudioplayer.Player.Graphic.BaseGraphic
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.VisualizerMenuAdapter
import com.emmett222.alloyaudioplayer.R
import org.w3c.dom.Text

/**
 * Screen for changing settings.
 *
 * @author Emmett Grebe
 * @version 7-28-2026
 */
class SettingsActivity : AppCompatActivity() {
    var filesOpen: Boolean = false
    var playerOpen: Boolean = false

    private lateinit var animValue: TextView
    private lateinit var shortenValue: TextView
    private lateinit var repeatValue: TextView
    private lateinit var shuffleValue: TextView
    private lateinit var visualizerValue: TextView

    private val optLeft = "Left"
    private val optRight = "Right"
    private val optNone = "None"
    private val optYes = "Yes"
    private val optNo = "No"

    /**
     * Runs on creation.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This makes Android's navigation bar become transparent.
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        // This replaces the default back button functionality.
//        val onBackPressedCallback = object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                // Add an 'are you sure?' pop up here.
//            }
//        }
//        // This adds the custom call back to the dispatcher. The dispatcher is responsible for
//        // handling the back click.
//        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        animValue = findViewById(R.id.settingAnimValue)
        shortenValue = findViewById(R.id.settingShortenValue)
        repeatValue = findViewById(R.id.settingRValue)
        shuffleValue = findViewById(R.id.settingASValue)
        visualizerValue = findViewById(R.id.settingVisualizerValue)

        setupValues()
        setupDropdowns()
        setupButtons()
    }

    /**
     * Changes the values to what the user set them to previously. If user has not changed a
     * setting, then it will show nothing.
     */
    private fun setupValues() {
        when (SettingsChange.getAnimType(this)) {
            0 -> animValue.text = optLeft
            1 -> animValue.text = optRight
            2 -> animValue.text = optNone
        }

        if (SettingsChange.getShortMode(this)) shortenValue.text = optYes
        else optNo

        if (SettingsChange.getRepeatMode(this)) repeatValue.text = optYes
        else optNo

        if (SettingsChange.getShufMode(this)) shuffleValue.text = optYes
        else optNo

        visualizerValue.text = VisualizerMenuAdapter.items[SettingsChange.getVisType(this)]
    }

    /**
     * Sets up the dropdowns. Makes them drop down to show settings inside.
     */
    private fun setupDropdowns() {
        val fileTitle: LinearLayout = findViewById(R.id.fileListTitleContainer)
        val playerTitle: LinearLayout = findViewById(R.id.playerTitleContainer)

        val fileArrow: ImageView = findViewById(R.id.fileDropdown)
        val playerArrow: ImageView = findViewById(R.id.playerDropdown)

        val fileContainer: LinearLayout = findViewById(R.id.filesSettingsContainer)
        val playerContainer: LinearLayout = findViewById(R.id.playerSettingsContainer)

        fileTitle.setOnClickListener {
            if (filesOpen) {
                fileArrow.rotation = 0F
                fileContainer.visibility = View.GONE
                filesOpen = false
            } else {
                fileArrow.rotation = 180F
                fileContainer.visibility = View.VISIBLE
                filesOpen = true
            }
        }

        playerTitle.setOnClickListener {
            if (playerOpen) {
                playerArrow.rotation = 0F
                playerContainer.visibility = View.GONE
                playerOpen = false
            } else {
                playerArrow.rotation = 180F
                playerContainer.visibility = View.VISIBLE
                playerOpen = true
            }
        }
    }

    /**
     * Sets up the buttons. When pressing a button, it will change the value text to itself, then
     * change the setting.
     */
    private fun setupButtons() {
        // Buttons:
        val animLeft: TextView = findViewById(R.id.settingAnimLeft)
        val animRight: TextView = findViewById(R.id.settingAnimRight)
        val animNone: TextView = findViewById(R.id.settingAnimNone)
        val shortenYes: TextView = findViewById(R.id.settingShortYes)
        val shortenNo: TextView = findViewById(R.id.settingShortNo)
        val repeatYes: TextView = findViewById(R.id.settingRYes)
        val repeatNo: TextView = findViewById(R.id.settingRNo)
        val shuffleYes: TextView = findViewById(R.id.settingShufYes)
        val shuffleNo: TextView = findViewById(R.id.settingShufNo)
        val visNone: TextView = findViewById(R.id.settingVNone)
        val visWaves: TextView = findViewById(R.id.settingVLW)
        val visMirWaves: TextView = findViewById(R.id.settingVMLW)
        val visMidBars: TextView = findViewById(R.id.settingVBM)
        val visBotBars: TextView = findViewById(R.id.settingVBB)
        val visCirWaves: TextView = findViewById(R.id.settingVCW)
        val visCirBars: TextView = findViewById(R.id.settingVCB)
        val visGrowCir: TextView = findViewById(R.id.settingVGC)
        val visSmile: TextView = findViewById(R.id.settingVSF)

        // Put it all in a hashmap:
        val allButtons: HashMap<TextView, Array<TextView>> = HashMap()
        allButtons[animValue] = arrayOf(animLeft, animRight, animNone)
        allButtons[shortenValue] = arrayOf(shortenYes, shortenNo)
        allButtons[repeatValue] = arrayOf(repeatYes, repeatNo)
        allButtons[shuffleValue] = arrayOf(shuffleYes, shuffleNo)
        allButtons[visualizerValue] = arrayOf(
            visNone,
            visWaves,
            visMirWaves,
            visMidBars,
            visBotBars,
            visCirWaves,
            visCirBars,
            visGrowCir,
            visSmile
        )

        // Doing this so I don't have to repeat this for every button.
        for (key in allButtons.keys) {
            for (button in allButtons[key]!!) { // This will never be null.
                button.setOnClickListener {
                    changeSetting(key, button.text as String)
                    button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        }
    }

    /**
     * Changes a setting. Changes the text and the setting itself.
     *
     * @param settingText The text of the setting's option.
     * @param newValue The value to change the setting to.
     */
    private fun changeSetting(settingText: TextView, newValue: String) {
        settingText.text = newValue
        when (settingText) {
            animValue -> {
                when (newValue) {
                    "Left" -> SettingsChange.saveAnimType(this, 0)
                    "Right" -> SettingsChange.saveAnimType(this, 1)
                    "None" -> SettingsChange.saveAnimType(this, 2)
                }
            }

            shortenValue -> {
                if (newValue == "Yes") SettingsChange.saveShortMode(this, true)
                else SettingsChange.saveShortMode(this, false)
            }

            repeatValue -> {
                if (newValue == "Yes") SettingsChange.saveRepeatMode(this, true)
                else SettingsChange.saveRepeatMode(this, false)
            }

            shuffleValue -> {
                if (newValue == "Yes") SettingsChange.saveShufMode(this, true)
                else SettingsChange.saveShufMode(this, false)
            }

            visualizerValue -> {
                when (newValue) {
                    VisualizerMenuAdapter.NOVIS -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_NONE
                    )

                    VisualizerMenuAdapter.LINEWAVE -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_WAVE
                    )

                    VisualizerMenuAdapter.MIRLINEWAVE -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_MIRROR_WAVE
                    )

                    VisualizerMenuAdapter.LINEBARS -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_BARS
                    )

                    VisualizerMenuAdapter.BOTLINEBARS -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_BOTTOM_BARS
                    )

                    VisualizerMenuAdapter.CIRCLEWAVE -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_CIRCLE_WAVE
                    )

                    VisualizerMenuAdapter.CIRCLEBAR -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_CIRCLE_BARS
                    )

                    VisualizerMenuAdapter.CIRCLEGROW -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_CIRCLE_GROW
                    )

                    VisualizerMenuAdapter.TALKINGSMILEY -> SettingsChange.saveVisType(
                        this,
                        BaseGraphic.VIS_TYPE_SMILEY
                    )
                }
            }
        }
    }
}