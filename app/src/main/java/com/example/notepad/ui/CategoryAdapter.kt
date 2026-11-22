package com.example.notepad.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notepad.R
import com.example.notepad.data.Category

/** 分类列表适配器：色点、名称、笔记数量、删除按钮。 */
class CategoryAdapter(private val onDelete: (Category) -> Unit) :
    RecyclerView.Adapter<CategoryAdapter.CategoryHolder>() {

    private val items = mutableListOf<Category>()

    init {
        setHasStableIds(true)
    }

    fun submit(list: List<Category>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class CategoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dot: View = itemView.findViewById(R.id.dotColor)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val count: TextView = itemView.findViewById(R.id.tvCount)
        val delete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
        val category = items[position]
        holder.name.text = category.name
        holder.count.text = holder.itemView.context.getString(R.string.category_note_count, category.noteCount)
        holder.dot.backgroundTintList = ColorStateList.valueOf(category.color)
        holder.delete.setOnClickListener { onDelete(category) }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id
}
