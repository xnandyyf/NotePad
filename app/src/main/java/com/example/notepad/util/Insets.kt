package com.example.notepad.util

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** 把状态栏 / 导航栏占用的空间补成根布局的内边距。 */
fun AppCompatActivity.applySystemBarsPadding(@IdRes rootId: Int) {
    val root = findViewById<View>(rootId)
    val base = intArrayOf(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom)
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            base[0] + bars.left,
            base[1] + bars.top,
            base[2] + bars.right,
            base[3] + bars.bottom
        )
        insets
    }
}
