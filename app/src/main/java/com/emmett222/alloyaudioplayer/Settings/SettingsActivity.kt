package com.emmett222.alloyaudioplayer.Settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.emmett222.alloyaudioplayer.Info.InfoSettingsData
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Util.ColorUtil

/**
 * Screen for changing settings.
 *
 * @author Emmett Grebe
 * @version 8-21-2026
 */
class SettingsActivity : AppCompatActivity() {
    var generalOpen: Boolean = false
    var filesOpen: Boolean = false
    var playerOpen: Boolean = false
    private var color1: Int = 0
    private var color2: Int = 0
    private var color3: Int = 0
    private lateinit var colorValue1: TextView
    private lateinit var colorValue2: TextView
    private lateinit var colorValue3: TextView
    private lateinit var animValue: TextView
    private lateinit var dFolderValue: TextView
    private lateinit var pgnteValue: TextView
    private lateinit var shortenValue: TextView
    private lateinit var sortByValue: TextView
    private lateinit var sortDirValue: TextView
    private lateinit var repeatValue: TextView
    private lateinit var shuffleValue: TextView
    private lateinit var disconnectValue: TextView
    private lateinit var visualizerValue: TextView

    private lateinit var color1Info: ImageButton
    private lateinit var color2Info: ImageButton
    private lateinit var color3Info: ImageButton
    private lateinit var animInfo: ImageButton
    private lateinit var dFolderInfo: ImageButton
    private lateinit var pgnteInfo: ImageButton
    private lateinit var shortInfo: ImageButton
    private lateinit var sortInfo: ImageButton
    private lateinit var sortByInfo: ImageButton
    private lateinit var repeatInfo: ImageButton
    private lateinit var shuffInfo: ImageButton
    private lateinit var disconnectInfo: ImageButton
    private lateinit var visInfo: ImageButton

    private val optYes = "Yes"
    private val optNo = "No"
    private val malformedData = "Unknown"
    val folderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                var folderReturn = result.data?.data?.path ?: ""
                if (folderReturn.startsWith("/tree/primary:")) {
                    val cleanFolder = folderReturn.replace("/tree/primary:", "/storage/emulated/0/")
                    SettingsChange.saveDefaultFolder(this, cleanFolder)
                    dFolderValue.text = cleanFolder
                } else {
                    SettingsChange.saveDefaultFolder(this, result.data?.data?.path ?: "")
                    dFolderValue.text = result.data?.data?.path ?: "root"
                }
            }
        }

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
        dFolderValue = findViewById(R.id.settingDefaultFolderValue)
        pgnteValue = findViewById(R.id.settingPgnteValue)
        shortenValue = findViewById(R.id.settingShortenValue)
        repeatValue = findViewById(R.id.settingRValue)
        shuffleValue = findViewById(R.id.settingASValue)
        disconnectValue = findViewById(R.id.settingDisValue)
        sortByValue = findViewById(R.id.settingSortValue)
        sortDirValue = findViewById(R.id.settingSortDirValue)
        visualizerValue = findViewById(R.id.settingVisualizerValue)

        color1Info = findViewById(R.id.color1Info)
        color2Info = findViewById(R.id.color2Info)
        color3Info = findViewById(R.id.color3Info)
        animInfo = findViewById(R.id.animInfo)
        dFolderInfo = findViewById(R.id.defaultFolderInfo)
        pgnteInfo = findViewById(R.id.pgnteInfo)
        shortInfo = findViewById(R.id.shortInfo)
        sortInfo = findViewById(R.id.sortInfo)
        sortByInfo = findViewById(R.id.sortDirInfo)
        repeatInfo = findViewById(R.id.repeatInfo)
        shuffInfo = findViewById(R.id.shuffInfo)
        disconnectInfo = findViewById(R.id.disInfo)
        visInfo = findViewById(R.id.visInfo)

        setupValues()
        setupDropdowns()
        setupButtons()
        setupColorButtons()
        setupInfo()
        setupDefaultFolderButton()
        setupPgnteEnterButton()
    }

    /**
     * Changes the values to what the user set them to previously. If user has not changed a
     * setting, then it will show nothing.
     */
    private fun setupValues() {
        animValue.text =
            AnimationType.entries.find { it.id == SettingsChange.getAnimType(this) }?.label
                ?: malformedData

        dFolderValue.text = SettingsChange.getDefaultFolder(this)

        if (SettingsChange.getShortMode(this)) shortenValue.text = optYes
        else shortenValue.text = optNo

        if (SettingsChange.getRepeatMode(this)) repeatValue.text = optYes
        else repeatValue.text = optNo

        if (SettingsChange.getShufMode(this)) shuffleValue.text = optYes
        else shuffleValue.text = optNo

        if (SettingsChange.getDisconnectMode(this)) disconnectValue.text = optYes
        else disconnectValue.text = optNo

        visualizerValue.text =
            VisualizerType.entries.find { it.id == SettingsChange.getVisType(this) }?.label
                ?: malformedData

        color1 = SettingsChange.getColor1(this)
        color2 = SettingsChange.getColor2(this)
        color3 = SettingsChange.getColor3(this)
        colorValue1.text = "#" + color1.toHexString()
        colorValue2.text = "#" + color2.toHexString()
        colorValue3.text = "#" + color3.toHexString()

        ColorUtil.updateAllTextColors(findViewById<ScrollView>(R.id.Scrollcontainer), color1)

        findViewById<ScrollView>(R.id.Scrollcontainer).background = color2.toDrawable()

        ColorUtil.updateAllAccentColors(findViewById<ScrollView>(R.id.Scrollcontainer), color3)

        pgnteValue.text =
            PaginateType.entries.find { it.id == SettingsChange.getPaginateNum(this) }?.label
                ?: SettingsChange.getPaginateNum(this).toString()

        sortByValue.text =
            SortType.entries.find { it.id == SettingsChange.getSortType(this) }?.label
                ?: malformedData

        sortDirValue.text =
            SortDirection.entries.find { it.id == SettingsChange.getSortDir(this) }?.label
                ?: malformedData
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
        val auxDisYes: TextView = findViewById(R.id.settingDisYes)
        val auxDisNo: TextView = findViewById(R.id.settingDisNo)
        val pgnteEnter: TextView = findViewById(R.id.settingPgnteEnter)
        val pgnteInf: TextView = findViewById(R.id.settingPgnteInf)
        val sortByDef: TextView = findViewById(R.id.settingSortDefault)
        val sortByAlph: TextView = findViewById(R.id.settingSortAlph)
        val sortByAuth: TextView = findViewById(R.id.settingSortAuth)
        val sortByLen: TextView = findViewById(R.id.settingSortLength)
        val sortByRand: TextView = findViewById(R.id.settingSortRand)
        val sortDirAsc: TextView = findViewById(R.id.settingSortDirAsc)
        val sortDirDes: TextView = findViewById(R.id.settingSortDirDes)
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
        allButtons[disconnectValue] = arrayOf(auxDisYes, auxDisNo)
        allButtons[pgnteValue] = arrayOf(pgnteEnter, pgnteInf)
        allButtons[shortenValue] = arrayOf(shortenYes, shortenNo)
        allButtons[sortByValue] = arrayOf(sortByDef, sortByAlph, sortByAuth, sortByLen, sortByRand)
        allButtons[sortDirValue] = arrayOf(sortDirAsc, sortDirDes)
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
                    changeSetting(key, button.text.toString().trim())
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
                SettingsChange.saveAnimType(
                    this,
                    AnimationType.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id
                        ?: 0
                )
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

            disconnectValue -> {
                if (newValue == "Yes") SettingsChange.saveDisconnectMode(this, true)
                else SettingsChange.saveDisconnectMode(this, false)
            }

            visualizerValue -> {
                SettingsChange.saveVisType(
                    this,
                    VisualizerType.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id
                        ?: 0
                )
            }

            pgnteValue -> {
                SettingsChange.savePaginateNum(
                    this,
                    PaginateType.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id
                        ?: Integer.parseInt(newValue)
                )
            }

            sortByValue -> {
                SettingsChange.saveSortType(
                    this,
                    SortType.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id ?: 0
                )
            }

            sortDirValue -> {
                SettingsChange.saveSortDir(
                    this,
                    SortDirection.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id
                        ?: 0
                )
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
        builder.setMessage(
            "Enter a hex code (e.g., #00FF00 for green). Supports Alpha values in " + "the front (e.g., #7F00FF00 for half opacity green)"
        )

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
                        ColorUtil.updateAllTextColors(
                            findViewById<ScrollView>(R.id.Scrollcontainer), colorInt
                        )
                    }

                    2 -> {
                        SettingsChange.saveColor2(this, colorInt)
                        colorValue2.text = hexString
                        findViewById<ScrollView>(R.id.Scrollcontainer).background =
                            colorInt.toDrawable()
                    }

                    3 -> {
                        SettingsChange.saveColor3(this, colorInt)
                        colorValue3.text = hexString
                        ColorUtil.updateAllAccentColors(
                            findViewById<ScrollView>(R.id.Scrollcontainer), colorInt
                        )
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

    /**
     * Makes the default folder button bring up a file chooser. The returned folder is used for the
     * default folder setting.
     */
    private fun setupDefaultFolderButton() {
        val dFolderButton = findViewById<TextView>(R.id.settingDefaultFolder)
        dFolderButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            folderLauncher.launch(intent)
        }
    }


    /**
     * Sets up the pagination option button. Shows a number input, then saves it to the pagination
     * setting. If the number has a decimal or is not a number, will show a pop-up saying to try
     * again.
     */
    private fun setupPgnteEnterButton() {
        val pgnteButton = findViewById<TextView>(R.id.settingPgnteEnter)
        pgnteButton.setOnClickListener {
            // Make the pop-up builder.
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Set files per page")
            builder.setMessage(
                "Enter a number with no decimals."
            )

            // User input.
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER

            // Some style.
            input.setPadding(50, 40, 50, 40)
            builder.setView(input)

            // Save button.
            builder.setPositiveButton("Save") { dialog, _ ->
                val inputString = input.text.toString()
                try {
                    val num = Integer.parseInt(inputString)
                    SettingsChange.savePaginateNum(
                        this,
                        PaginateType.entries.find {
                            it.label.equals(
                                inputString,
                                ignoreCase = true
                            )
                        }?.id
                            ?: num
                    )
                    pgnteValue.text = inputString
                    Toast.makeText(
                        this,
                        "Files per page number saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: NumberFormatException) {
                    Toast.makeText(
                        this,
                        "Invalid number! Must be a number that has no decimals.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Cancel Button
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.show()
        }
    }

    /**
     * Sets up the info buttons for each setting. Makes them pop-up a small window that tells the
     * user what the setting effects and how much the setting effects battery life.
     */
    private fun setupInfo() {
        val infoText = HashMap<ImageButton, Array<String>>()
        infoText[color1Info] = arrayOf(
            InfoSettingsData.TITLE_TEXT_COLOR,
            InfoSettingsData.BODY_TEXT_COLOR,
            InfoSettingsData.BAT_NONE
        )
        infoText[color2Info] = arrayOf(
            InfoSettingsData.TITLE_BG_COLOR,
            InfoSettingsData.BODY_BG_COLOR,
            InfoSettingsData.BAT_NONE
        )
        infoText[color3Info] = arrayOf(
            InfoSettingsData.TITLE_ACC_COLOR,
            InfoSettingsData.BODY_ACC_COLOR,
            InfoSettingsData.BAT_NONE
        )
        infoText[animInfo] = arrayOf(
            InfoSettingsData.TITLE_ANIM, InfoSettingsData.BODY_ANIM, InfoSettingsData.BAT_ANIM
        )
        infoText[dFolderInfo] = arrayOf(
            InfoSettingsData.TITLE_DFOLDER, InfoSettingsData.BODY_DFOLDER, InfoSettingsData.BAT_NONE
        )
        infoText[pgnteInfo] = arrayOf(
            InfoSettingsData.TITLE_PGNTE,
            InfoSettingsData.BODY_PGNTE,
            InfoSettingsData.BAT_PGNTE,
        )
        infoText[shortInfo] = arrayOf(
            InfoSettingsData.TITLE_SHORT, InfoSettingsData.BODY_SHORT, InfoSettingsData.BAT_SLIGHT
        )
        infoText[repeatInfo] = arrayOf(
            InfoSettingsData.TITLE_REPEAT, InfoSettingsData.BODY_REPEAT, InfoSettingsData.BAT_NONE
        )
        infoText[shuffInfo] = arrayOf(
            InfoSettingsData.TITLE_SHUFF, InfoSettingsData.BODY_SHUFF, InfoSettingsData.BAT_NONE
        )
        infoText[disconnectInfo] = arrayOf(
            InfoSettingsData.TITLE_DIS, InfoSettingsData.BODY_DIS, InfoSettingsData.BAT_NONE
        )
        infoText[visInfo] = arrayOf(
            InfoSettingsData.TITLE_VIS, InfoSettingsData.BODY_VIS, InfoSettingsData.BAT_VIS
        )

        arrayOf(
            color1Info,
            color2Info,
            color3Info,
            animInfo,
            dFolderInfo,
            shortInfo,
            repeatInfo,
            shuffInfo,
            disconnectInfo,
            visInfo
        ).forEach { it ->
            it.setOnClickListener {
                showPopUp(
                    it,
                    infoText[it]?.get(0) ?: "Missing title info data.",
                    infoText[it]?.get(1) ?: "Missing body info data.",
                    infoText[it]?.get(2) ?: "Missing battery info data."
                )
            }
        }
    }

    /**
     * Shows a pop-up to the user based on the information given.
     *
     * @param anchorView Where the pop-up is anchored to.
     * @param title The title of the info.
     * @param body The body text of the info.
     * @param battery The effect of the battery on the user's device.
     */
    private fun showPopUp(anchorView: View, title: String, body: String, battery: String) {
        val popupView = layoutInflater.inflate(R.layout.settings_info_popup, null)
        popupView.findViewById<TextView>(R.id.popup_title).text = title
        popupView.findViewById<TextView>(R.id.popup_body).text = body
        popupView.findViewById<TextView>(R.id.popup_battery).text = battery

        val allComponents = popupView.findViewById<LinearLayout>(R.id.all)
        ColorUtil.updateAllTextColors(allComponents, color1)
        allComponents.background = color2.toDrawable()

        // The pop-up view.
        // Wraps both ways so it grows and shrinks.
        // True makes it so if the user taps anywhere else, it will close the pop-up.
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        val button = popupView.findViewById<Button>(R.id.close_button)
        button.setTextColor(color1)
        button.backgroundTintList = ColorStateList.valueOf(color3)
        button.setOnClickListener {
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchorView, 0, 0)
    }
}