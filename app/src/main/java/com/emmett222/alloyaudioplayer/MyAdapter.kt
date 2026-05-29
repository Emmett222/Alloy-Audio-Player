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
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.Background.MediaEngine
import com.emmett222.alloyaudioplayer.Player.PlayerActivity
import java.io.File
import java.util.TreeMap

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

        alphabetTree = generateAlphabetTable()

        // Ensure R.layout.recycler_item actually exists in your res/layout folder
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Runs on each element bound.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectedFile = files[position]
        holder.textView.text = selectedFile.name


        if (selectedFile.isDirectory) {
            holder.imageView.setImageResource(R.drawable.baseline_folder_24)
            holder.cdCase.visibility = View.INVISIBLE
        } else {
            holder.imageView.setImageResource(R.drawable.baseline_audio_file_24)
            holder.itemView.setBackgroundColor(getColorFromName(selectedFile.name))
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

    /**
     * Generates a TreeMap with:
     * - Keys: Each letter of the alphabet.
     * - Entries: 0-255 spread out amongst the letters evenly.
     *
     * @return TreeMap with aforementioned characteristics.
     */
    private fun generateAlphabetTable(): TreeMap<Char, Int> {
        val table = TreeMap<Char, Int>()
        val alphabet = ('A'..'Z').toList()

        for (i in alphabet.indices) {
            val value = (i * 255) / 25
            table[alphabet[i]] = value
        }
        return table
    }

    /**
     * Uses the first 3 letters of a string to get a unique color. If string has less than 3
     * letters, then only remaining letters are used.
     *
     * @param name: String to use.
     * @return Int Color based off of the first 3 letters of the string.
     */
    private fun getColorFromName(name: String) : Int {
        val char0 = if (name.isNotEmpty()) name[0].uppercaseChar() else 'A'
        val char1 = if (name.length > 1) name[1].uppercaseChar() else 'A'
        val char2 = if (name.length > 2) name[2].uppercaseChar() else 'A'

        val r = alphabetTree.getOrDefault(char0, 0)
        val g = alphabetTree.getOrDefault(char1, 0)
        val b = alphabetTree.getOrDefault(char2, 0)

        return Color.rgb(r, g, b)
    }



    override fun getItemCount(): Int = files.size

    // ViewHolder definition
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
        val cdCase: ImageView = itemView.findViewById(R.id.cd_case_overlay)
    }
}