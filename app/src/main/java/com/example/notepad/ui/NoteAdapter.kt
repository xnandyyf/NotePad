package com.example.notepad.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notepad.R
import com.example.notepad.data.Note
import com.example.notepad.util.TimeFormat

/**
 * 笔记列表适配器。
 *
 * 列表项显示：标题、正文摘要、分类、时间戳（yyyy-MM-dd HH:mm），置顶的笔记带“置顶”标记。
 */
class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteHolder>() {

    private val items = mutableListOf<Note>()

    init {
        setHasStableIds(true)
    }

    fun submit(list: List<Note>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class NoteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val snippet: TextView = itemView.findViewById(R.id.tvSnippet)
        val time: TextView = itemView.findViewById(R.id.tvTime)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val dot: View = itemView.findViewById(R.id.dotCategory)
        val pinned: TextView = itemView.findViewById(R.id.tvPinned)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteHolder(view)
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val note = items[position]
        val context = holder.itemView.context

        holder.title.text = note.title.ifBlank { context.getString(R.string.untitled) }
        holder.snippet.text = note.content.replace('\n', ' ').trim()
        holder.snippet.visibility = if (note.content.isBlank()) View.GONE else View.VISIBLE
        holder.time.text = TimeFormat.format(note.updatedAt)
        holder.pinned.visibility = if (note.pinned) View.VISIBLE else View.GONE

        if (note.categoryName != null) {
            holder.category.text = note.categoryName
            holder.category.visibility = View.VISIBLE
            holder.dot.visibility = View.VISIBLE
            holder.dot.backgroundTintList = ColorStateList.valueOf(note.categoryColor)
        } else {
            holder.category.visibility = View.GONE
            holder.dot.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val current = holder.bindingAdapterPosition
            if (current != RecyclerView.NO_POSITION) {
                onNoteClick(items[current])
            }
        }
        holder.itemView.setOnLongClickListener {
            val current = holder.bindingAdapterPosition
            if (current != RecyclerView.NO_POSITION) {
                onNoteLongClick(items[current])
            }
            true
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id
}
