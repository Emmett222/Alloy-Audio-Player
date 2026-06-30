package com.emmett222.alloyaudioplayer.Player.Graphic.Menu

import android.content.Context
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
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
 * @version 6-30-2026
 */
class FilesMenuAdapter(val context: Context,
                       var files: Array<File>,
                       private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<FilesMenuAdapter.ViewHolder>() {

    private var alphabetTree: TreeMap<Char, Int> = NameUtil.generateAlphabetTable()

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

        val currItem = files[position]
        holder.textView.text = currItem.name

        if (currItem.isDirectory) {
            holder.imageView.setImageResource(R.drawable.baseline_folder_24)
            holder.queueBtn.visibility = View.GONE
        } else {
            holder.imageView.setImageResource(R.drawable.baseline_audio_file_24)

            val color = NameUtil.getColorFromName(currItem.name, alphabetTree, true)
            val oppositeColor = ColorUtil.textColorFromColor(color)
            holder.imageView.background = color
            holder.imageView.setColorFilter(Color.WHITE)
            holder.textView.text = NameUtil.removeDescriptors(currItem.name)
            if (oppositeColor == Color.WHITE) {
                holder.imageView.setColorFilter(ColorUtil.brightenColor(color, 60))
            } else {
                holder.imageView.setColorFilter(ColorUtil.darkenColor(color, 60))
            }
            holder.queueBtn.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onItemClick(currItem) // Forward the click event back to the Activity
        }

    }

    /**
     * Just gets the items size.
     *
     * @return Size of the items on the menu.
     */
    override fun getItemCount(): Int { return files.size }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
        val queueBtn: ImageButton = itemView.findViewById(R.id.queueBtn)
    }
}