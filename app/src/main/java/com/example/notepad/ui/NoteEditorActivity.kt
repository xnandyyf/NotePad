package com.example.notepad.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.notepad.R
import com.example.notepad.data.NoteDbHelper
import com.example.notepad.data.Prefs
import com.example.notepad.util.TimeFormat
import com.example.notepad.util.applySystemBarsPadding
import com.google.android.material.appbar.MaterialToolbar

/**
 * 笔记编辑页：标题、正文、分类、字数统计，支持保存 / 分享 / 删除。
 */
class NoteEditorActivity : AppCompatActivity() {

    private lateinit var db: NoteDbHelper
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var tvCategory: TextView
    private lateinit var tvWordCount: TextView

    private var noteId = 0L
    private var updatedAt = System.currentTimeMillis()
    private var categoryId: Long? = null
    private var originalTitle = ""
    private var originalContent = ""
    private var originalCategoryId: Long? = null
    private var savedOrDeleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)
        applySystemBarsPadding(R.id.root)

        db = NoteDbHelper(this)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { handleBack() }

        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        tvCategory = findViewById(R.id.tvCategory)
        tvWordCount = findViewById(R.id.tvWordCount)

        val contentSize = Prefs.fontSize(this).toFloat()
        etContent.textSize = contentSize

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        if (noteId > 0L) {
            val note = db.getNote(noteId)
            if (note == null) {
                finish()
                return
            }
            updatedAt = note.updatedAt
            categoryId = note.categoryId
            originalCategoryId = note.categoryId
            etTitle.setText(note.title)
            etContent.setText(note.content)
            originalTitle = note.title
            originalContent = note.content
            toolbar.title = getString(R.string.editor_title)
            toolbar.subtitle = TimeFormat.format(note.updatedAt)
        } else {
            toolbar.title = getString(R.string.editor_new)
        }

        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateWordCount()
        })

        tvCategory.setOnClickListener { pickCategory() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })

        updateCategoryLabel()
        updateWordCount()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        val existing = noteId > 0L
        menu.findItem(R.id.action_share)?.isVisible = existing
        menu.findItem(R.id.action_delete)?.isVisible = existing
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                save()
                true
            }

            R.id.action_share -> {
                share()
                true
            }

            R.id.action_delete -> {
                confirmDelete()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun save() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString()
        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, R.string.content_empty, Toast.LENGTH_SHORT).show()
            return
        }
        if (noteId > 0L) {
            db.update(noteId, title, content, categoryId)
        } else {
            noteId = db.insert(title, content, categoryId)
        }
        savedOrDeleted = true
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun share() {
        val title = etTitle.text.toString().ifBlank { getString(R.string.untitled) }
        val text = "$title\n\n${etContent.text}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)))
    }

    private fun confirmDelete() {
        if (noteId <= 0L) return
        AlertDialog.Builder(this)
            .setMessage(R.string.delete_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                db.delete(noteId)
                savedOrDeleted = true
                Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }

    private fun pickCategory() {
        val categories = db.categories()
        val labels = mutableListOf(getString(R.string.category_none))
        categories.forEach { labels.add(it.name) }
        val checked = if (categoryId == null) 0 else categories.indexOfFirst { it.id == categoryId } + 1

        AlertDialog.Builder(this)
            .setTitle(R.string.category_pick)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
                categoryId = if (which == 0) null else categories[which - 1].id
                updateCategoryLabel()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateCategoryLabel() {
        val name = categoryId?.let { id -> db.categories().firstOrNull { it.id == id }?.name }
            ?: getString(R.string.category_none)
        tvCategory.text = getString(R.string.category_label_value, name)
    }

    private fun updateWordCount() {
        tvWordCount.text = getString(
            R.string.word_count,
            etContent.text.length,
            TimeFormat.format(updatedAt)
        )
    }

    private fun hasChanges(): Boolean {
        return etTitle.text.toString() != originalTitle ||
            etContent.text.toString() != originalContent ||
            categoryId != originalCategoryId
    }

    private fun handleBack() {
        if (savedOrDeleted || !hasChanges()) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setMessage(R.string.unsaved_discard)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.discard) { _, _ -> finish() }
            .show()
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}
