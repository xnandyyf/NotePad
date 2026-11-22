package com.example.notepad.data

import android.content.Context

/** 轻量偏好设置：深色模式、正文字号、默认排序。 */
object Prefs {

    private const val NAME = "notepad_prefs"
    private const val KEY_DARK = "dark_mode"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_SORT = "sort_order"

    const val FONT_SIZE_MIN = 12
    const val FONT_SIZE_MAX = 24
    const val FONT_SIZE_DEFAULT = 16

    private fun sp(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isDarkMode(context: Context): Boolean = sp(context).getBoolean(KEY_DARK, false)

    fun setDarkMode(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_DARK, enabled).apply()
    }

    fun fontSize(context: Context): Int = sp(context).getInt(KEY_FONT_SIZE, FONT_SIZE_DEFAULT)

    fun setFontSize(context: Context, size: Int) {
        sp(context).edit().putInt(KEY_FONT_SIZE, size.coerceIn(FONT_SIZE_MIN, FONT_SIZE_MAX)).apply()
    }

    fun sortOrder(context: Context): SortOrder {
        val raw = sp(context).getString(KEY_SORT, SortOrder.UPDATED.name)
        return runCatching { SortOrder.valueOf(raw!!) }.getOrDefault(SortOrder.UPDATED)
    }

    fun setSortOrder(context: Context, order: SortOrder) {
        sp(context).edit().putString(KEY_SORT, order.name).apply()
    }
}
