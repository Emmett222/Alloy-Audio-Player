package com.emmett222.alloyaudioplayer.Player.Graphic.Menu.QueueMenu

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.emmett222.alloyaudioplayer.Player.Graphic.Menu.QueueMenu.Objects.QueueRowItem
import com.emmett222.alloyaudioplayer.R
import com.emmett222.alloyaudioplayer.Util.FileUtil
import com.emmett222.alloyaudioplayer.Util.NameUtil
import com.emmett222.alloyaudioplayer.Util.StringUtil
import java.io.File
import java.util.Collections

/**
 * Queue screen for the player.
 * Callback returns the file clicked, a boolean for if it is added to queue, a boolean for if
 * the file is removed from the playlist, and a boolean if it is in the queue or not.
 *
 * @author Emmett Grebe
 * @version 7-17-2026
 */
class QueueAdapter(val context: Context,
                   val listItems: MutableList<QueueRowItem>,
                   private val onItemClick: (File) -> Unit,
                   private val onQueueClick: (File) -> Unit,
                   private val onRemoveClick: (File, Boolean) -> Unit,
                   private val onItemMove:(List<QueueRowItem>) -> Unit
) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {
    private val handler = Handler(Looper.getMainLooper())

    private var attachedRecyclerView: RecyclerView? = null
    private lateinit var ithCallback: ItemTouchHelper.SimpleCallback
    private lateinit var ith: ItemTouchHelper

    /**
     * Runs on creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Initiates the XML layout into the view object.
        val view = LayoutInflater.from(context).inflate(R.layout.graphic_queue_item, parent, false)

        return ViewHolder(view)
    }

    /**
     * Runs on binding.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currItem = listItems[position]
        holder.textView.text = NameUtil.removeDescriptors(currItem.file.name)

        holder.itemView.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onItemClick(currItem.file) // Forward the click event back to the Activity
        }
        holder.queueBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onQueueClick(currItem.file) // Forward the click event back to the Activity
        }
        holder.removeBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onRemoveClick(currItem.file, currItem.isInQueue) // Forward the click event back to the Activity
        }

        if (position == 0) {
            holder.removeBtn.visibility = View.GONE
        }
        if (currItem.isCurrentPlaying) {
            holder.removeBtn.visibility = View.GONE
            holder.imageView.setImageResource(R.drawable.menu_queue_song)
        } else if (currItem.isInQueue) {
            holder.removeBtn.visibility = View.VISIBLE
            holder.imageView.setImageResource(R.drawable.menu_queue_songqueue)
        } else {
            holder.removeBtn.visibility = View.VISIBLE
            holder.imageView.setImageResource(R.drawable.menu_queue_song)
        }

        holder.imageView.setColorFilter(NameUtil.getColorFromName(currItem.file.name))

        // Put the heavy task in the background. This stops it from freezing up and not doing
        // the animations.
        handler.post {
            if (holder.adapterPosition == position) {
                val ms = FileUtil.getDurationFromFile(context, currItem.file.absolutePath)
                holder.timeText.text = StringUtil.formatMinutesAndSeconds(ms.toInt())
            }
        }
    }

    /**
     * Gets the RecyclerView when it is attached. Then adds it to the variable.
     *
     * @param recyclerView The RecyclerView that is attached.
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.attachedRecyclerView = recyclerView
        createITH()
    }

    /**
     * Prevents memory leaks by removing the RecyclerView when it is detached.
     *
     * @param recyclerView The RecyclerView that is attached.
     */
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.attachedRecyclerView = null
    }

    /**
     * Helper to create the ItemTouchHelper. Does these two things on move and swipe:
     * Move: Up or down. Moves an audio file around in the queue.
     * Swipe: Removes an audio file from the queue.
     */
    private fun createITH() {
        this.ithCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // Drag directions
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Swipe directions
        ) {
            /**
             * This fires when the item is moved. Moves the item in the array.
             */
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.absoluteAdapterPosition
                val to = target.absoluteAdapterPosition

                // No moving the current song or moving into the current song.
                if (from == 0 || to == 0) return false

                Collections.swap(listItems, from, to)

                notifyItemMoved(from, to)
                return true
            }

            /**
             * This fires when the item is dropped after being clicked and held. Moves the item in
             * the audio file list.
             */
            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                onItemMove(listItems)
            }

            /**
             * This fires when the item is swiped. Removes the item from the queue.
             */
            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                val position = viewHolder.absoluteAdapterPosition
                val removedItem = listItems[position]

                onRemoveClick(removedItem.file, removedItem.isInQueue)
            }

            /**
             * This fires when an item is swiped. This will show a little animation to show it is
             * deleted.
             */
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )

                // Do this later. It is just for looks so it is not needed now.
            }
        }

        ith = ItemTouchHelper(ithCallback)
        attachedRecyclerView?.post { ith.attachToRecyclerView(attachedRecyclerView) }
    }

    /**
     * This updates the layout system to cleanly change positions so scrolling doesn't break after
     * moving an item.
     *
     * @param newItems The new list of items for the layout to use.
     */
    fun updateData(newItems: List<QueueRowItem>) {
        this.listItems.clear()
        this.listItems.addAll(newItems)
    }

    /**
     * Just gets the items size.
     *
     * @return Size of the items on the menu.
     */
    override fun getItemCount(): Int { return listItems.size }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.file_name_text_view)
        val timeText: TextView = itemView.findViewById(R.id.time)
        val imageView: ImageView = itemView.findViewById(R.id.icon_view)
        val queueBtn: ImageButton = itemView.findViewById(R.id.queueBtn)
        val removeBtn: ImageButton = itemView.findViewById(R.id.removeBtn)
    }
}