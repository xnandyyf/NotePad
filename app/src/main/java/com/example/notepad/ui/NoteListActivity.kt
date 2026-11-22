package com.example.notepad.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notepad.R
import com.example.notepad.data.Category
import com.example.notepad.data.Note
import com.example.notepad.data.NoteDbHelper
import com.example.notepad.data.Prefs
import com.example.notepad.data.SortOrder
import com.example.notepad.util.TimeFormat
import com.example.notepad.util.applySystemBarsPadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

/**
 * 笔记列表：时间戳展示、标题 / 内容搜索、分类筛选、排序、置顶、导出全部。
 */
class NoteListActivity : AppCompatActivity() {

    private lateinit var db: NoteDbHelper
    private lateinit var adapter: NoteAdapter
    private lateinit var toolbar: MaterialToolbar
    private lateinit var chipGroup: ChipGroup
    private lateinit var emptyView: TextView

    private var keyword: String = ""
    private var selectedCategoryId: Long? = null
    private var sortOrder: SortOrder = SortOrder.UPDATED

    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
            if (uri != null) exportAll(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)
        applySystemBarsPadding(R.id.root)

        db = NoteDbHelper(this)
        sortOrder = Prefs.sortOrder(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        chipGroup = findViewById(R.id.chipCategories)
        emptyView = findViewById(R.id.tvEmpty)

        adapter = NoteAdapter(onNoteClick = ::openNote, onNoteLongClick = ::showNoteActions)

        val recycler = findViewById<RecyclerView>(R.id.rvNotes)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val search = findViewById<TextInputEditText>(R.id.etSearch)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                keyword = s?.toString().orEmpty()
                loadNotes()
            }
        })

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, NoteEditorActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        sortOrder = Prefs.sortOrder(this)
        rebuildCategoryChips()
        loadNotes()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_list, menu)
        val checked = when (sortOrder) {
            SortOrder.UPDATED -> R.id.sort_updated
            SortOrder.CREATED -> R.id.sort_created
            SortOrder.TITLE -> R.id.sort_title
        }
        menu.findItem(checked)?.isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort_updated -> {
                changeSort(SortOrder.UPDATED)
                true
            }

            R.id.sort_created -> {
                changeSort(SortOrder.CREATED)
                true
            }

            R.id.sort_title -> {
                changeSort(SortOrder.TITLE)
                true
            }
            R.id.action_category -> {
                startActivity(Intent(this, CategoryActivity::class.java))
                true
            }

            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.action_export_all -> {
                if (db.listNotes("", null, sortOrder).isEmpty()) {
                    Toast.makeText(this, R.string.export_empty, Toast.LENGTH_SHORT).show()
                } else {
                    createDocument.launch(getString(R.string.export_file_name))
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun changeSort(order: SortOrder) {
        sortOrder = order
        Prefs.setSortOrder(this, order)
        invalidateOptionsMenu()
        loadNotes()
    }

    private fun rebuildCategoryChips() {
        val categories = db.categories()
        val keepId = selectedCategoryId
        chipGroup.removeAllViews()

        val allChip = buildChip(getString(R.string.category_all), keepId == null)
        allChip.setOnClickListener {
            selectedCategoryId = null
            loadNotes()
        }
        chipGroup.addView(allChip)

        categories.forEach { category ->
            val chip = buildChip(category.name, keepId == category.id)
            chip.setOnClickListener {
                selectedCategoryId = category.id
                loadNotes()
            }
            chipGroup.addView(chip)
        }

        if (keepId != null && categories.none { it.id == keepId }) {
            selectedCategoryId = null
            chipGroup.check(allChip.id)
        }
    }

    private fun buildChip(label: String, checked: Boolean): Chip {
        return Chip(this).apply {
            id = label.hashCode().let { if (it == 0) 1 else kotlin.math.abs(it) }
            text = label
            isCheckable = true
            isChecked = checked
            setEnsureMinTouchTargetSize(false)
        }
    }

    private fun loadNotes() {
        val notes = db.listNotes(keyword, selectedCategoryId, sortOrder)
        adapter.submit(notes)
        emptyView.visibility = if (notes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        toolbar.subtitle = getString(R.string.note_count, notes.size)
    }

    private fun openNote(note: Note) {
        startActivity(Intent(this, NoteEditorActivity::class.java).putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id))
    }

    private fun showNoteActions(note: Note) {
        val pinLabel = getString(if (note.pinned) R.string.menu_unpin else R.string.menu_pin)
        val actions = arrayOf(pinLabel, getString(R.string.menu_share), getString(R.string.menu_delete))

        AlertDialog.Builder(this)
            .setTitle(R.string.note_actions)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        db.setPinned(note.id, !note.pinned)
                        loadNotes()
                    }

                    1 -> shareNote(note)
                    2 -> confirmDelete(note)
                }
            }
            .show()
    }

    private fun shareNote(note: Note) {
        val title = note.title.ifBlank { getString(R.string.untitled) }
        val text = buildString {
            append(title).append("\n\n").append(note.content)
            append("\n\n").append(TimeFormat.format(note.updatedAt))
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)))
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setMessage(R.string.delete_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                db.delete(note.id)
                loadNotes()
            }
            .show()
    }

    private fun exportAll(uri: Uri) {
        val notes = db.listNotes("", null, sortOrder)
        runCatching {
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                notes.forEachIndexed { index, note ->
                    writer.write("${index + 1}. ${note.title.ifBlank { getString(R.string.untitled) }}")
                    writer.newLine()
                    writer.write("分类：${note.categoryName ?: getString(R.string.category_none)}")
                    writer.newLine()
                    writer.write("时间：${TimeFormat.format(note.updatedAt)}")
                    writer.newLine()
                    writer.newLine()
                    writer.write(note.content)
                    writer.newLine()
                    writer.write("----------------------------------------")
                    writer.newLine()
                    writer.newLine()
                }
            }
        }.onSuccess {
            Toast.makeText(this, getString(R.string.export_done, uri.toString()), Toast.LENGTH_LONG).show()
        }.onFailure { error ->
            Toast.makeText(this, getString(R.string.export_failed, error.message.orEmpty()), Toast.LENGTH_LONG).show()
        }
    }
}
