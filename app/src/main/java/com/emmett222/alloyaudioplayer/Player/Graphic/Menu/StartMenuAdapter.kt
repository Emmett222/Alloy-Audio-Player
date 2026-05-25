package com.emmett222.alloyaudioplayer.Player.Graphic.Menu

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.MyAdapter
import com.emmett222.alloyaudioplayer.MyAdapter.ViewHolder
import com.emmett222.alloyaudioplayer.R

/**
 * Menu that shows when menu button is pressed on the Player Activity.
 * Shows buttons for visualizers, queue, settings
 */
class StartMenuAdapter(val context: Context) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    companion object {
        const val VISUALIZERS = "Visualizers"
        const val QUEUE = "Queue"
        const val SETTINGS = "Settings"
    }

    private val items: Array<String> = arrayOf(VISUALIZERS, QUEUE, SETTINGS)

    /**
     * Runs on creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.ViewHolder {
        // Initiates the XML layout into the view object.
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Runs on binding.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currItem = items[position]
        holder.textView.text = items[position]
    }

    /**
     * Just gets the items size.
     *
     * @return Size of the items on the menu.
     */
    override fun getItemCount(): Int { return items.size }
}