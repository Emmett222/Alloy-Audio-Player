package com.emmett222.alloyaudioplayer.Player.Graphic.Menu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.MyAdapter
import com.emmett222.alloyaudioplayer.MyAdapter.ViewHolder
import com.emmett222.alloyaudioplayer.R

/**
 * Menu that shows when menu button is pressed on the Player Activity.
 * Shows buttons for visualizers, queue, settings
 */
class StartMenuAdapter(val context: Context) : RecyclerView.Adapter<StartMenuAdapter.ViewHolder>() {

    companion object {
        const val VISUALIZERS = "Visualizers"
        const val QUEUE = "Queue"
        const val FILES = "Files"
        const val SETTINGS = "Settings"
    }

    private val items: Array<String> = arrayOf(VISUALIZERS, QUEUE, FILES, SETTINGS)

    /**
     * Runs on creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartMenuAdapter.ViewHolder {
        // Initiates the XML layout into the view object.
        val view = LayoutInflater.from(context).inflate(R.layout.graphic_menu_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Runs on binding.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currItem = items[position]
        holder.textView.text = currItem
    }

    /**
     * Just gets the items size.
     *
     * @return Size of the items on the menu.
     */
    override fun getItemCount(): Int { return items.size }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
    }
}