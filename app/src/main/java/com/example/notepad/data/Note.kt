package com.example.notepad.data

/** 一条笔记。createdAt / updatedAt 都是毫秒时间戳。 */
data class Note(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val categoryColor: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val pinned: Boolean = false
)

/** 笔记分类。 */
data class Category(
    val id: Long,
    val name: String,
    val color: Int,
    val noteCount: Int = 0
)

/** 列表排序方式。 */
enum class SortOrder {
    UPDATED, CREATED, TITLE
}
