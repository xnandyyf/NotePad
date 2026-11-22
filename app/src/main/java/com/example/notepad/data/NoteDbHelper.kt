package com.example.notepad.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.notepad.R
import androidx.core.content.ContextCompat

/**
 * 笔记数据库：notes 保存笔记，categories 保存分类。
 * 列表的时间戳、搜索、分类筛选、排序全部在这里用 SQL 完成。
 */
class NoteDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL DEFAULT '',
                $COL_CONTENT TEXT NOT NULL DEFAULT '',
                $COL_CATEGORY INTEGER,
                $COL_CREATED INTEGER NOT NULL,
                $COL_UPDATED INTEGER NOT NULL,
                $COL_PINNED INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY ($COL_CATEGORY) REFERENCES $TABLE_CATEGORIES($COL_ID) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL UNIQUE,
                $COL_COLOR INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_updated ON $TABLE_NOTES($COL_UPDATED)")
        seedCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 课程实验阶段只做向前演进：新增列时在这里 ALTER TABLE
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COL_PINNED INTEGER NOT NULL DEFAULT 0")
        }
    }

    private fun seedCategories(db: SQLiteDatabase) {
        val defaults = listOf(
            "工作" to R.color.cat_work,
            "生活" to R.color.cat_life,
            "学习" to R.color.cat_study,
            "灵感" to R.color.cat_idea
        )
        defaults.forEach { (name, colorRes) ->
            val values = ContentValues().apply {
                put(COL_NAME, name)
                put(COL_COLOR, ContextCompat.getColor(appContext, colorRes))
            }
            db.insert(TABLE_CATEGORIES, null, values)
        }
    }

    // ------------------------------------------------------------ 查询
    fun listNotes(keyword: String, categoryId: Long?, sort: SortOrder): List<Note> {
        val selection = StringBuilder()
        val args = mutableListOf<String>()

        if (keyword.isNotBlank()) {
            selection.append("(${noteAlias}.$COL_TITLE LIKE ? OR ${noteAlias}.$COL_CONTENT LIKE ?)")
            val like = "%${keyword.trim()}%"
            args.add(like)
            args.add(like)
        }
        if (categoryId != null) {
            if (selection.isNotEmpty()) selection.append(" AND ")
            selection.append("${noteAlias}.$COL_CATEGORY = ?")
            args.add(categoryId.toString())
        }

        val order = when (sort) {
            SortOrder.UPDATED -> "${noteAlias}.$COL_PINNED DESC, ${noteAlias}.$COL_UPDATED DESC"
            SortOrder.CREATED -> "${noteAlias}.$COL_PINNED DESC, ${noteAlias}.$COL_CREATED DESC"
            SortOrder.TITLE -> "${noteAlias}.$COL_PINNED DESC, ${noteAlias}.$COL_TITLE COLLATE NOCASE ASC"
        }

        val sql = """
            SELECT n.$COL_ID, n.$COL_TITLE, n.$COL_CONTENT, n.$COL_CATEGORY, n.$COL_CREATED,
                   n.$COL_UPDATED, n.$COL_PINNED, c.$COL_NAME AS cat_name, c.$COL_COLOR AS cat_color
            FROM $TABLE_NOTES n
            LEFT JOIN $TABLE_CATEGORIES c ON c.$COL_ID = n.$COL_CATEGORY
            ${if (selection.isEmpty()) "" else "WHERE $selection"}
            ORDER BY $order
        """.trimIndent()

        val result = mutableListOf<Note>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    Note(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        content = cursor.getString(2),
                        categoryId = if (cursor.isNull(3)) null else cursor.getLong(3),
                        createdAt = cursor.getLong(4),
                        updatedAt = cursor.getLong(5),
                        pinned = cursor.getInt(6) == 1,
                        categoryName = cursor.getString(7),
                        categoryColor = if (cursor.isNull(8)) 0 else cursor.getInt(8)
                    )
                )
            }
        }
        return result
    }

    fun getNote(id: Long): Note? {
        val sql = """
            SELECT n.$COL_ID, n.$COL_TITLE, n.$COL_CONTENT, n.$COL_CATEGORY, n.$COL_CREATED,
                   n.$COL_UPDATED, n.$COL_PINNED, c.$COL_NAME, c.$COL_COLOR
            FROM $TABLE_NOTES n
            LEFT JOIN $TABLE_CATEGORIES c ON c.$COL_ID = n.$COL_CATEGORY
            WHERE n.$COL_ID = ?
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(id.toString())).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return Note(
                id = cursor.getLong(0),
                title = cursor.getString(1),
                content = cursor.getString(2),
                categoryId = if (cursor.isNull(3)) null else cursor.getLong(3),
                createdAt = cursor.getLong(4),
                updatedAt = cursor.getLong(5),
                pinned = cursor.getInt(6) == 1,
                categoryName = cursor.getString(7),
                categoryColor = if (cursor.isNull(8)) 0 else cursor.getInt(8)
            )
        }
    }

    fun categories(): List<Category> {
        val sql = """
            SELECT c.$COL_ID, c.$COL_NAME, c.$COL_COLOR,
                   (SELECT COUNT(*) FROM $TABLE_NOTES n WHERE n.$COL_CATEGORY = c.$COL_ID) AS cnt
            FROM $TABLE_CATEGORIES c
            ORDER BY c.$COL_ID ASC
        """.trimIndent()
        val result = mutableListOf<Category>()
        readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(Category(cursor.getLong(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3)))
            }
        }
        return result
    }

    // ------------------------------------------------------------ 写入
    fun insert(title: String, content: String, categoryId: Long?): Long {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
            if (categoryId == null) putNull(COL_CATEGORY) else put(COL_CATEGORY, categoryId)
            put(COL_CREATED, now)
            put(COL_UPDATED, now)
            put(COL_PINNED, 0)
        }
        return writableDatabase.insert(TABLE_NOTES, null, values)
    }

    fun update(id: Long, title: String, content: String, categoryId: Long?) {
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
            if (categoryId == null) putNull(COL_CATEGORY) else put(COL_CATEGORY, categoryId)
            put(COL_UPDATED, System.currentTimeMillis())
        }
        writableDatabase.update(TABLE_NOTES, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE_NOTES, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun setPinned(id: Long, pinned: Boolean) {
        val values = ContentValues().apply { put(COL_PINNED, if (pinned) 1 else 0) }
        writableDatabase.update(TABLE_NOTES, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun addCategory(name: String): Boolean {
        val values = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_COLOR, categoryColorFor(name))
        }
        return writableDatabase.insert(TABLE_CATEGORIES, null, values) != -1L
    }

    fun deleteCategory(id: Long) {
        writableDatabase.delete(TABLE_CATEGORIES, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun categoryNames(): List<String> = categories().map { it.name }

    private fun categoryColorFor(name: String): Int {
        val palette = intArrayOf(
            ContextCompat.getColor(appContext, R.color.cat_work),
            ContextCompat.getColor(appContext, R.color.cat_life),
            ContextCompat.getColor(appContext, R.color.cat_study),
            ContextCompat.getColor(appContext, R.color.cat_idea)
        )
        return palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    }

    companion object {
        const val DB_NAME = "notepad.db"
        const val DB_VERSION = 2

        const val TABLE_NOTES = "notes"
        const val TABLE_CATEGORIES = "categories"

        const val COL_ID = "_id"
        const val COL_TITLE = "title"
        const val COL_CONTENT = "content"
        const val COL_CATEGORY = "category_id"
        const val COL_CREATED = "created_at"
        const val COL_UPDATED = "updated_at"
        const val COL_PINNED = "pinned"
        const val COL_NAME = "name"
        const val COL_COLOR = "color"

        private const val noteAlias = "n"
    }
}
