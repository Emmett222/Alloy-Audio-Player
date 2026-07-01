package com.emmett222.alloyaudioplayer.Player.Graphic.Menu

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Util.NameUtil
import java.io.File
import java.util.TreeMap

/**
 * Queue screen for the player.
 * Callback returns the file clicked, a boolean for if it is added to queue, a boolean for if
 * the file is removed from the playlist, and a boolean if it is in the queue or not.
 *
 * @author Emmett Grebe
 * @version 5-30-2026
 */
class QueueAdapter(val context: Context,
                   val currentItem: File,
                   val queueItems: ArrayDeque<File>,
                   val items: Array<File>,
                   private val onItemClick: (File, Boolean, Boolean, Boolean) -> Unit
) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

    private var alphabetTree: TreeMap<Char, Int> = NameUtil.generateAlphabetTable()
    val allItems: List<File> = listOf(currentItem) + queueItems + items

    /**
     * Runs on creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueAdapter.ViewHolder {
        // Initiates the XML layout into the view object.
        val view = LayoutInflater.from(context).inflate(R.layout.graphic_queue_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Runs on binding.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val isInQueue = ((position != 0) && (position <= queueItems.size))
        val currItem = allItems[position]
        holder.textView.text = NameUtil.removeDescriptors(currItem.name)

        holder.itemView.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onItemClick(currItem, false, false, isInQueue) // Forward the click event back to the Activity
        }
        holder.queueBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onItemClick(currItem, true, false, isInQueue) // Forward the click event back to the Activity
        }
        holder.removeBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onItemClick(currItem, false, true, isInQueue) // Forward the click event back to the Activity
        }

        if (position == 0) {
            holder.removeBtn.visibility = View.GONE
        }
        if (isInQueue) {
            holder.imageView.setImageResource(R.drawable.menu_queue_songqueue)
        } else {
            holder.imageView.setImageResource(R.drawable.menu_queue_song)
        }

        holder.imageView.setColorFilter(NameUtil.getColorFromName(currItem.name, alphabetTree))
    }

    /**
     * Just gets the items size.
     *
     * @return Size of the items on the menu.
     */
    override fun getItemCount(): Int { return allItems.size }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
        val queueBtn: ImageButton = itemView.findViewById(R.id.queueBtn)
        val removeBtn: ImageButton = itemView.findViewById(R.id.removeBtn)
    }
}