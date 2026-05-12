package com.emmett222.alloyaudioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import java.io.File

class MyAdapter(val context: Context, var files: Array<File>) :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Ensure R.layout.recycler_item actually exists in your res/layout folder
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectedFile = files[position]
        holder.textView.text = selectedFile.name

        if (selectedFile.isDirectory) {
            holder.imageView.setImageResource(R.drawable.baseline_folder_24)
        } else {
            holder.imageView.setImageResource(R.drawable.baseline_audio_file_24)
        }

        holder.itemView.setOnClickListener() {
            if (selectedFile.isDirectory) {
                var intent: Intent = Intent(context, FileListActivity::class.java)
                var path: String = selectedFile.absolutePath
                intent.putExtra("path", path)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                var intent: Intent = Intent(context, PlayerActivity::class.java)
                var path: String = selectedFile.path
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
    }
}