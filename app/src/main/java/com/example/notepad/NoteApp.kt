package com.example.notepad

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.notepad.data.Prefs

/** 启动时按偏好设置应用深色 / 浅色配色，避免每个界面重复处理。 */
class NoteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val mode = if (Prefs.isDarkMode(this)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
