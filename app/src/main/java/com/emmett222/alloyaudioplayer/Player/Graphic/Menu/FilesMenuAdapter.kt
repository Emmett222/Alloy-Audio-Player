package com.emmett222.alloyaudioplayer.Player.Graphic.Menu

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Util.ColorUtil
import com.emmett222.alloyaudioplayer.Util.NameUtil
import java.io.File
import java.util.TreeMap

/**
 * Files menu to select the audio file wanted. Only shows audio files.
 *
 * @author Emmett Grebe
 * @version 7-1-2026
 */
class FilesMenuAdapter(val context: Context,
                       var backOption: File?,
                       var files: Array<File>,
                       private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<FilesMenuAdapter.ViewHolder>() {

    private var alphabetTree: TreeMap<Char, Int> = NameUtil.generateAlphabetTable()
    private val items: Array<File?> = buildList {
        if (backOption != null && backOption?.name != "emulated") {
            add(backOption) // Kotlin smart casts this perfectly to a non-null File
        }
        addAll(files) // Appends the rest
    }.toTypedArray()
    private var size = files.size

    /**
     * Runs on creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Initiates the XML layout into the view object.
        val view = LayoutInflater.from(context).inflate(R.layout.graphic_menu_files_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Runs on binding.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currItem = items[position] ?: return
        holder.textView.text = currItem.name

        if (currItem.isDirectory) { // Folders.
            if (currItem == backOption) { // Parent parent folder.
                holder.imageView.setImageResource(R.drawable.menu_files_back)
                holder.textView.setTypeface(holder.textView.typeface, Typeface.ITALIC)
            } else {
                holder.imageView.setImageResource(R.drawable.baseline_folder_24)
            }
            holder.queueBtn.visibility = View.GONE
            holder.imageView.setColorFilter(Color.GREEN)

        } else { // Audio files.
            val color = NameUtil.getColorFromName(currItem.name, alphabetTree, true)
            val oppositeColor = ColorUtil.textColorFromColor(color)

            holder.imageView.setImageResource(R.drawable.baseline_audio_file_24)
            holder.imageView.background = color
            if (oppositeColor == Color.WHITE) {
                holder.imageView.setColorFilter(ColorUtil.brightenColor(color, 60))
            } else {
                holder.imageView.setColorFilter(ColorUtil.darkenColor(color, 60))
            }

            holder.textView.text = NameUtil.removeDescriptors(currItem.name)

            holder.queueBtn.visibility = View.VISIBLE
        }

        if (currItem.isDirectory) {
            holder.all.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onItemClick(currItem) // Forward the click event back to the Activity
            }
        } else {
            holder.queueBtn.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onItemClick(currItem) // Forward the click event back to the Activity
            }
        }

        // Strangely, if user scrolls down then back up, the adapter sees the back option as an
        // audio file. It still acts as a directory, but the icon gets the background of an audio
        // file. This fixes that.
        if (currItem == backOption) {
            holder.imageView.background = null
        }
    }

    /**
     * Just gets the items size. Includes back button.
     *
     * @return Size of the items on the menu.
     */
    override fun getItemCount(): Int { return items.size}

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val all: LinearLayout = itemView.findViewById(R.id.all)
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
        val queueBtn: ImageButton = itemView.findViewById(R.id.queueBtn)
    }
}