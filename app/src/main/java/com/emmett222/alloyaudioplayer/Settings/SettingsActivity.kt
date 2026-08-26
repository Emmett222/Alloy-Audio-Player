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
import com.emmett222.alloyaudioplayer.databinding.ActivityFileListBinding
import com.emmett222.alloyaudioplayer.databinding.SettingsBinding

/**
 * Screen for changing settings.
 *
 * @author Emmett Grebe
 * @version 8-25-2026
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsBinding
    var generalOpen: Boolean = false
    var filesOpen: Boolean = false
    var playerOpen: Boolean = false
    private var color1: Int = 0
    private var color2: Int = 0
    private var color3: Int = 0

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
                    binding.settingDefaultFolderValue.text = cleanFolder
                } else {
                    SettingsChange.saveDefaultFolder(this, result.data?.data?.path ?: "")
                    binding.settingDefaultFolderValue.text = result.data?.data?.path ?: "root"
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // This replaces the default back button functionality.
//        val onBackPressedCallback = object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                // Add an 'are you sure?' pop up here.
//            }
//        }
//        // This adds the custom call back to the dispatcher. The dispatcher is responsible for
//        // handling the back click.
//        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

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
        binding.settingAnimValue.text =
            AnimationType.entries.find { it.id == SettingsChange.getAnimType(this) }?.label
                ?: malformedData

        binding.settingDefaultFolderValue.text = SettingsChange.getDefaultFolder(this)

        if (SettingsChange.getShortMode(this)) binding.settingShortenValue.text = optYes
        else binding.settingShortenValue.text = optNo

        if (SettingsChange.getRepeatMode(this)) binding.settingRValue.text = optYes
        else binding.settingRValue.text = optNo

        if (SettingsChange.getShufMode(this)) binding.settingASValue.text = optYes
        else binding.settingASValue.text = optNo

        if (SettingsChange.getDisconnectMode(this)) binding.settingDisValue.text = optYes
        else binding.settingDisValue.text = optNo

        binding.settingVisualizerValue.text =
            VisualizerType.entries.find { it.id == SettingsChange.getVisType(this) }?.label
                ?: malformedData

        color1 = SettingsChange.getColor1(this)
        color2 = SettingsChange.getColor2(this)
        color3 = SettingsChange.getColor3(this)
        binding.settingColorValue1.text = "#" + color1.toHexString()
        binding.settingColorValue2.text = "#" + color2.toHexString()
        binding.settingColorValue3.text = "#" + color3.toHexString()

        ColorUtil.updateAllTextColors(findViewById<ScrollView>(R.id.Scrollcontainer), color1)

        findViewById<ScrollView>(R.id.Scrollcontainer).background = color2.toDrawable()

        ColorUtil.updateAllAccentColors(findViewById<ScrollView>(R.id.Scrollcontainer), color3)

        binding.settingPgnteValue.text =
            PaginateType.entries.find { it.id == SettingsChange.getPaginateNum(this) }?.label
                ?: SettingsChange.getPaginateNum(this).toString()

        binding.settingSortValue.text =
            SortType.entries.find { it.id == SettingsChange.getSortType(this) }?.label
                ?: malformedData

        binding.settingSortDirValue.text =
            SortDirection.entries.find { it.id == SettingsChange.getSortDir(this) }?.label
                ?: malformedData
    }

    /**
     * Sets up the dropdowns. Makes them drop down to show settings inside.
     */
    private fun setupDropdowns() {
        binding.generalTitleContainer.setOnClickListener {
            if (generalOpen) {
                binding.generalDropdown.rotation = 0F
                binding.generalSettingsContainer.visibility = View.GONE
                generalOpen = false
            } else {
                binding.generalDropdown.rotation = 180F
                binding.generalSettingsContainer.visibility = View.VISIBLE
                generalOpen = true
            }
        }

        binding.fileListTitleContainer.setOnClickListener {
            if (filesOpen) {
                binding.fileDropdown.rotation = 0F
                binding.filesSettingsContainer.visibility = View.GONE
                filesOpen = false
            } else {
                binding.fileDropdown.rotation = 180F
                binding.filesSettingsContainer.visibility = View.VISIBLE
                filesOpen = true
            }
        }

        binding.playerTitleContainer.setOnClickListener {
            if (playerOpen) {
                binding.playerDropdown.rotation = 0F
                binding.playerSettingsContainer.visibility = View.GONE
                playerOpen = false
            } else {
                binding.playerDropdown.rotation = 180F
                binding.playerSettingsContainer.visibility = View.VISIBLE
                playerOpen = true
            }
        }
    }

    /**
     * Sets up the buttons. When pressing a button, it will change the value text to itself, then
     * change the setting.
     */
    private fun setupButtons() {
        // Put it all in a hashmap:
        val allButtons: HashMap<TextView, Array<TextView>> = hashMapOf(
            binding.settingAnimValue to arrayOf(
                binding.settingAnimLeft,
                binding.settingAnimRight,
                binding.settingAnimNone
            ),
            binding.settingDisValue to arrayOf(
                binding.settingDisYes,
                binding.settingDisNo
            ),
            binding.settingPgnteValue to arrayOf(
                binding.settingPgnteEnter,
                binding.settingPgnteInf
            ),
            binding.settingShortenValue to arrayOf(
                binding.settingShortYes,
                binding.settingShortNo
            ),
            binding.settingSortValue to arrayOf(
                binding.settingSortDefault,
                binding.settingSortAlph,
                binding.settingSortAuth,
                binding.settingSortLength,
                binding.settingSortRand
            ),
            binding.settingSortDirValue to arrayOf(
                binding.settingSortDirAsc,
                binding.settingSortDirDes
            ),
            binding.settingRValue to arrayOf(
                binding.settingRYes,
                binding.settingRNo
            ),
            binding.settingASValue to arrayOf(
                binding.settingShufYes,
                binding.settingShufNo
            ),
            binding.settingVisualizerValue to arrayOf(
                binding.settingVNone,
                binding.settingVLW,
                binding.settingVMLW,
                binding.settingVBM,
                binding.settingVBB,
                binding.settingVCW,
                binding.settingVCB,
                binding.settingVGC,
                binding.settingVSF
            )
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
            binding.settingAnimValue -> {
                SettingsChange.saveAnimType(
                    this,
                    AnimationType.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id
                        ?: 0
                )
            }

            binding.settingShortenValue -> {
                if (newValue == "Yes") SettingsChange.saveShortMode(this, true)
                else SettingsChange.saveShortMode(this, false)
            }

            binding.settingRValue -> {
                if (newValue == "Yes") SettingsChange.saveRepeatMode(this, true)
                else SettingsChange.saveRepeatMode(this, false)
            }

            binding.settingASValue -> {
                if (newValue == "Yes") SettingsChange.saveShufMode(this, true)
                else SettingsChange.saveShufMode(this, false)
            }

            binding.settingDisValue -> {
                if (newValue == "Yes") SettingsChange.saveDisconnectMode(this, true)
                else SettingsChange.saveDisconnectMode(this, false)
            }

            binding.settingVisualizerValue -> {
                SettingsChange.saveVisType(
                    this,
                    VisualizerType.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id
                        ?: 0
                )
            }

            binding.settingPgnteValue -> {
                SettingsChange.savePaginateNum(
                    this,
                    PaginateType.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id
                        ?: Integer.parseInt(newValue)
                )
            }

            binding.settingSortValue -> {
                SettingsChange.saveSortType(
                    this,
                    SortType.entries.find { it.label.equals(newValue, ignoreCase = true) }?.id ?: 0
                )
            }

            binding.settingSortDirValue -> {
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
        binding.settingColor1.setOnClickListener {
            showColorPickerDialog(1)
            binding.settingColor1.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        binding.settingColor2.setOnClickListener {
            showColorPickerDialog(2)
            binding.settingColor2.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        binding.settingColor3.setOnClickListener {
            showColorPickerDialog(3)
            binding.settingColor3.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
                        binding.settingColorValue1.text = hexString
                        ColorUtil.updateAllTextColors(
                            findViewById<ScrollView>(R.id.Scrollcontainer), colorInt
                        )
                    }

                    2 -> {
                        SettingsChange.saveColor2(this, colorInt)
                        binding.settingColorValue2.text = hexString
                        findViewById<ScrollView>(R.id.Scrollcontainer).background =
                            colorInt.toDrawable()
                    }

                    3 -> {
                        SettingsChange.saveColor3(this, colorInt)
                        binding.settingColorValue3.text = hexString
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
                    binding.settingPgnteValue.text = inputString
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
        infoText[binding.color1Info] = arrayOf(
            InfoSettingsData.TITLE_TEXT_COLOR,
            InfoSettingsData.BODY_TEXT_COLOR,
            InfoSettingsData.BAT_NONE
        )
        infoText[binding.color2Info] = arrayOf(
            InfoSettingsData.TITLE_BG_COLOR,
            InfoSettingsData.BODY_BG_COLOR,
            InfoSettingsData.BAT_NONE
        )
        infoText[binding.color3Info] = arrayOf(
            InfoSettingsData.TITLE_ACC_COLOR,
            InfoSettingsData.BODY_ACC_COLOR,
            InfoSettingsData.BAT_NONE
        )
        infoText[binding.animInfo] = arrayOf(
            InfoSettingsData.TITLE_ANIM, InfoSettingsData.BODY_ANIM, InfoSettingsData.BAT_ANIM
        )
        infoText[binding.defaultFolderInfo] = arrayOf(
            InfoSettingsData.TITLE_DFOLDER, InfoSettingsData.BODY_DFOLDER, InfoSettingsData.BAT_NONE
        )
        infoText[binding.pgnteInfo] = arrayOf(
            InfoSettingsData.TITLE_PGNTE,
            InfoSettingsData.BODY_PGNTE,
            InfoSettingsData.BAT_PGNTE,
        )
        infoText[binding.shortInfo] = arrayOf(
            InfoSettingsData.TITLE_SHORT, InfoSettingsData.BODY_SHORT, InfoSettingsData.BAT_SLIGHT
        )
        infoText[binding.repeatInfo] = arrayOf(
            InfoSettingsData.TITLE_REPEAT, InfoSettingsData.BODY_REPEAT, InfoSettingsData.BAT_NONE
        )
        infoText[binding.shuffInfo] = arrayOf(
            InfoSettingsData.TITLE_SHUFF, InfoSettingsData.BODY_SHUFF, InfoSettingsData.BAT_NONE
        )
        infoText[binding.disInfo] = arrayOf(
            InfoSettingsData.TITLE_DIS, InfoSettingsData.BODY_DIS, InfoSettingsData.BAT_NONE
        )
        infoText[binding.visInfo] = arrayOf(
            InfoSettingsData.TITLE_VIS, InfoSettingsData.BODY_VIS, InfoSettingsData.BAT_VIS
        )

        arrayOf(
            binding.color1Info,
            binding.color2Info,
            binding.color3Info,
            binding.animInfo,
            binding.settingDefaultFolderValue,
            binding.shortInfo,
            binding.repeatInfo,
            binding.shuffInfo,
            binding.disInfo,
            binding.visInfo
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