package com.example.notepad.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notepad.R
import com.example.notepad.data.Category
import com.example.notepad.data.NoteDbHelper
import com.example.notepad.util.applySystemBarsPadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/** 分类管理：新增、删除分类，显示每个分类下的笔记数量。 */
class CategoryActivity : AppCompatActivity() {

    private lateinit var db: NoteDbHelper
    private lateinit var adapter: CategoryAdapter
    private lateinit var emptyView: TextView
    private lateinit var nameInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        applySystemBarsPadding(R.id.root)

        db = NoteDbHelper(this)
        emptyView = findViewById(R.id.tvEmpty)
        nameInput = findViewById(R.id.etCategoryName)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = CategoryAdapter(::confirmDelete)
        val recycler = findViewById<RecyclerView>(R.id.rvCategories)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.btnAddCategory).setOnClickListener { addCategory() }

        refresh()
    }

    private fun addCategory() {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.category_name_empty, Toast.LENGTH_SHORT).show()
            return
        }
        if (db.addCategory(name)) {
            nameInput.setText("")
            refresh()
        } else {
            Toast.makeText(this, R.string.category_exists, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(category: Category) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.category_delete_confirm, category.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                db.deleteCategory(category.id)
                refresh()
            }
            .show()
    }

    private fun refresh() {
        val categories = db.categories()
        adapter.submit(categories)
        emptyView.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
    }
}
