package com.emmett222.alloyaudioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.Navigator
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.Background.MediaEngine
import com.emmett222.alloyaudioplayer.Player.PlayerActivity
import com.emmett222.alloyaudioplayer.Util.ColorUtil
import com.emmett222.alloyaudioplayer.Util.NameUtil
import java.io.File
import java.util.TreeMap
import java.util.jar.Attributes

/**
 * Shows all the audio files and directories in a neat list. Shows audio files with a unique color
 * on them.
 *
 * @author Emmett Grebe
 * @version 5-29-2026
 */
class MyAdapter(val context: Context, var files: Array<File>) :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    // Keys: Each letter of the alphabet.
    // Entries: 0-255 spread out amongst the letters evenly.
    // Using TreeMap because it stays in alphabetical order.
    private lateinit var alphabetTree: TreeMap<Char, Int>

    /**
     * Runs on creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        alphabetTree = NameUtil.generateAlphabetTable()

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
        } else {
            holder.imageView.setImageResource(R.drawable.baseline_audio_file_24)

            val color = NameUtil.getColorFromName(selectedFile.name, alphabetTree, true)
            val oppositeColor = ColorUtil.textColorFromColor(color)
            holder.itemView.background = color
            holder.textView.text = NameUtil.removeDescriptors(selectedFile.name)
            holder.textView.setTextColor(oppositeColor)
            if (oppositeColor == Color.WHITE) {
                holder.imageView.setColorFilter(ColorUtil.brightenColor(color, 60))
            } else {
                holder.imageView.setColorFilter(ColorUtil.darkenColor(color, 60))
            }

        }

        holder.itemView.setOnClickListener() {
            if (selectedFile.isDirectory) {
                val intent: Intent = Intent(context, FileListActivity::class.java)
                val path: String = selectedFile.absolutePath
                intent.putExtra("path", path)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val intent: Intent = Intent(context, PlayerActivity::class.java)
                val path: String = selectedFile.path
                intent.putExtra("path", path)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = files.size

    // ViewHolder definition
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
        val cdCase: ImageView = itemView.findViewById(R.id.cd_case_overlay)
    }
}