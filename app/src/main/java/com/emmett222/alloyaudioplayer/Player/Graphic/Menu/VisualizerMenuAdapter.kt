package com.emmett222.alloyaudioplayer.Player.Graphic.Menu

import android.content.Context
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.R

/**
 * Visualizer menu to select the visualizer wanted.
 *
 * @author Emmett Grebe
 * @version 5-25-2026
 */
class VisualizerMenuAdapter(val context: Context,
                            private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<VisualizerMenuAdapter.ViewHolder>() {

    companion object {
        const val NOVIS = "No Visualizer"
        const val LINEWAVE = "Waves"
        const val MIRLINEWAVE = "Mirrored Waves"
        const val LINEBARS = "Middle Bars"
        const val BOTLINEBARS = "Bottom Bars"
        const val CIRCLEWAVE = "Circle Waves"
        const val CIRCLEBAR = "Circle Bars"
        const val CIRCLEGROW = "Growing Circle"
        const val TALKINGSMILEY = "Smiley face"

        var items: Array<String> = arrayOf(NOVIS, LINEWAVE, MIRLINEWAVE, LINEBARS, BOTLINEBARS, CIRCLEWAVE, CIRCLEBAR, CIRCLEGROW, TALKINGSMILEY)
    }

    /**
     * Runs on creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisualizerMenuAdapter.ViewHolder {
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

        when (currItem) {
            NOVIS -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_novis)
            }
            LINEWAVE -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_linewave)
            }
            MIRLINEWAVE -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_mirlinewave)
            }
            LINEBARS -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_linebars)
            }
            BOTLINEBARS -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_botlinebars)
            }
            CIRCLEWAVE -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_circlewave)
            }
            CIRCLEBAR -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_circlebar)
            }
            CIRCLEGROW -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_circlegrow)
            }
            TALKINGSMILEY -> {
                holder.imageView.setImageResource(R.drawable.menu_vis_talkingsmiley)
            }
        }

        holder.itemView.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onItemClick(currItem) // Forward the click event back to the Activity
        }

        holder.imageView.setColorFilter(Color.GREEN)
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