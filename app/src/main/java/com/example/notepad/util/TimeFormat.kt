package com.example.notepad.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 列表与编辑页统一的时间戳格式：yyyy-MM-dd HH:mm。 */
object TimeFormat {

    private val full = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun format(millis: Long): String = full.format(Date(millis))
}
