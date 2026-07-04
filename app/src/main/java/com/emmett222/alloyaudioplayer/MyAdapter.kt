package com.emmett222.alloyaudioplayer

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.Util.ColorUtil
import com.emmett222.alloyaudioplayer.Util.FileUtil
import com.emmett222.alloyaudioplayer.Util.NameUtil
import com.emmett222.alloyaudioplayer.Util.StringUtil
import java.io.File
import java.util.TreeMap

/**
 * Shows all the audio files and directories in a neat list. Shows audio files with a unique color
 * on them.
 *
 * @author Emmett Grebe
 * @version 7-1-2026
 */
class MyAdapter(
    val context: Context,
    var files: Array<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Runs on creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Ensure R.layout.recycler_item actually exists in your res/layout folder
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Runs on each element bound.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectedFile = files[position]


        if (selectedFile.isDirectory) {
            holder.imageView.setImageResource(R.drawable.baseline_folder_24)
            holder.cdCase.visibility = View.INVISIBLE
            holder.textView.text = selectedFile.name
            holder.textView.setTextColor(Color.parseColor("#000000"))

            holder.timeText.visibility = View.GONE
        } else {
            val color = NameUtil.getColorFromName(selectedFile.name, true)
            val oppositeColor = ColorUtil.textColorFromColor(color)

            holder.itemView.background = color

            holder.textView.text = NameUtil.removeDescriptors(selectedFile.name)
            holder.textView.setTextColor(oppositeColor)

            holder.imageView.setImageResource(R.drawable.baseline_audio_file_24)
            if (oppositeColor == Color.WHITE) {
                holder.imageView.setColorFilter(ColorUtil.brightenColor(color, 60))
            } else {
                holder.imageView.setColorFilter(ColorUtil.darkenColor(color, 60))
            }

            holder.timeText.setTextColor(oppositeColor)
            holder.timeText.text = "--:--" // Default placeholder

            // Put the heavy task in the background. This stops it from freezing up and not doing
            // the animations.
            handler.post {
                if (holder.adapterPosition == position) {
                    val ms = FileUtil.getDurationFromFile(context, selectedFile.absolutePath)
                    holder.timeText.text = StringUtil.formatMinutesAndSeconds(ms.toInt())
                }
            }
        }

        holder.itemView.setOnClickListener() {
            onItemClick(selectedFile)
        }
    }

    override fun getItemCount(): Int = files.size

    // ViewHolder definition
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val timeText: TextView = itemView.findViewById(R.id.time)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
        val cdCase: ImageView = itemView.findViewById(R.id.cd_case_overlay)
    }


}