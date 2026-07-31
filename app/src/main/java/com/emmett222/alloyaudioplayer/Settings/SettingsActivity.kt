package com.emmett222.alloyaudioplayer.Settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.emmett222.alloyaudioplayer.Player.Graphic.BaseGraphic
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.VisualizerMenuAdapter
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Util.ColorUtil
import org.w3c.dom.Text

/**
 * Screen for changing settings.
 *
 * @author Emmett Grebe
 * @version 7-31-2026
 */
class SettingsActivity : AppCompatActivity() {
    var generalOpen: Boolean = false
    var filesOpen: Boolean = false
    var playerOpen: Boolean = false

    private lateinit var colorValue1: TextView
    private lateinit var colorValue2: TextView
    private lateinit var colorValue3: TextView
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

        colorValue1 = findViewById(R.id.settingColorValue1)
        colorValue2 = findViewById(R.id.settingColorValue2)
        colorValue3 = findViewById(R.id.settingColorValue3)
        animValue = findViewById(R.id.settingAnimValue)
        shortenValue = findViewById(R.id.settingShortenValue)
        repeatValue = findViewById(R.id.settingRValue)
        shuffleValue = findViewById(R.id.settingASValue)
        visualizerValue = findViewById(R.id.settingVisualizerValue)

        setupValues()
        setupDropdowns()
        setupButtons()
        setupColorButtons()
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

        visualizerValue.text = VisualizerMenuAdapter.items[SettingsChange.getVisType(this) - 2]

        val color1 = SettingsChange.getColor1(this)
        val color2 = SettingsChange.getColor2(this)
        val color3 = SettingsChange.getColor3(this)

        colorValue1.text = "#" + color1.toHexString()
        colorValue2.text = "#" + color2.toHexString()
        colorValue3.text = "#" + color3.toHexString()

        ColorUtil.updateAllTextColors(findViewById<ScrollView>(R.id.Scrollcontainer), color1)

        findViewById<ScrollView>(R.id.Scrollcontainer).background = color2.toDrawable()

        ColorUtil.updateAllAccentColors(findViewById<ScrollView>(R.id.Scrollcontainer), color3)

    }

    /**
     * Sets up the dropdowns. Makes them drop down to show settings inside.
     */
    private fun setupDropdowns() {
        val generalTitle: LinearLayout = findViewById(R.id.generalTitleContainer)
        val fileTitle: LinearLayout = findViewById(R.id.fileListTitleContainer)
        val playerTitle: LinearLayout = findViewById(R.id.playerTitleContainer)

        val generalArrow: ImageView = findViewById(R.id.generalDropdown)
        val fileArrow: ImageView = findViewById(R.id.fileDropdown)
        val playerArrow: ImageView = findViewById(R.id.playerDropdown)

        val generalContainer: LinearLayout = findViewById(R.id.generalSettingsContainer)
        val fileContainer: LinearLayout = findViewById(R.id.filesSettingsContainer)
        val playerContainer: LinearLayout = findViewById(R.id.playerSettingsContainer)

        generalTitle.setOnClickListener {
            if (generalOpen) {
                generalArrow.rotation = 0F
                generalContainer.visibility = View.GONE
                generalOpen = false
            } else {
                generalArrow.rotation = 180F
                generalContainer.visibility = View.VISIBLE
                generalOpen = true
            }
        }

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

    /**
     * Sets up the color buttons.
     */
    private fun setupColorButtons() {
        val color1Button: TextView = findViewById(R.id.settingColor1)
        val color2Button: TextView = findViewById(R.id.settingColor2)
        val color3Button: TextView = findViewById(R.id.settingColor3)

        color1Button.setOnClickListener {
            showColorPickerDialog(1)
            color1Button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        color2Button.setOnClickListener {
            showColorPickerDialog(2)
            color2Button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        color3Button.setOnClickListener {
            showColorPickerDialog(3)
            color3Button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Shows a color picker dialog. Just asks user for input of a hex code.
     *
     * @param colorNumber 1 for text, 2 for background, 3 for accents.
     * @returns The color as an Int.
     */
    private fun showColorPickerDialog(colorNumber: Int) {
        // Make the pop-up builder.
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Custom Color")
        builder.setMessage("Enter a hex code (e.g., #00FF00 for green). Supports Alpha values in " +
                "the front (e.g., #7F00FF00 for half opacity green)")

        // User input.
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText("#") // Pre-fill the hashtag to help the user out

        // Some style.
        input.setPadding(50, 40, 50, 40)
        builder.setView(input)

        // Save button.
        builder.setPositiveButton("Save") { dialog, _ ->
            val hexString = input.text.toString().trim()

            try {
                // Throws an error if the hex is incorrect.
                val colorInt = Color.parseColor(hexString)
                when (colorNumber) {
                    1 -> {
                        SettingsChange.saveColor1(this, colorInt)
                        colorValue1.text = hexString
                        ColorUtil.updateAllTextColors(findViewById<ScrollView>(R.id.Scrollcontainer), colorInt)
                    }
                    2 -> {
                        SettingsChange.saveColor2(this, colorInt)
                        colorValue2.text = hexString
                        findViewById<ScrollView>(R.id.Scrollcontainer).background = colorInt.toDrawable()
                    }
                    3 -> {
                        SettingsChange.saveColor3(this, colorInt)
                        colorValue3.text = hexString
                        ColorUtil.updateAllAccentColors(findViewById<ScrollView>(R.id.Scrollcontainer), colorInt)
                    }
                }
                Toast.makeText(this, "Color saved!", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "Invalid Hex Code!", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancel Button
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}