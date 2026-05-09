import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.R
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

        // Optional: Handle icon logic here
        if (selectedFile.isDirectory) {
            holder.imageView.setImageResource(R.drawable.ic_folder) // Use your actual drawable name
        } else {
            holder.imageView.setImageResource(R.drawable.ic_file)
        }
    }

    override fun getItemCount(): Int = files.size

    // ViewHolder definition
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
    }
}