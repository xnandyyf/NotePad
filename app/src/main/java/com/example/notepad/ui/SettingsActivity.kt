package com.example.notepad.ui

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import com.example.notepad.R
import com.example.notepad.data.Prefs
import com.example.notepad.data.SortOrder
import com.example.notepad.util.applySystemBarsPadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch

/** 偏好设置：深色模式、正文字号、默认排序。 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        applySystemBarsPadding(R.id.root)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        setupDarkMode()
        setupFontSize()
        setupSortOrder()

        val versionText = findViewById<TextView>(R.id.tvVersion)
        versionText.text = getString(R.string.settings_version, versionName())
    }

    private fun setupDarkMode() {
        val switch = findViewById<MaterialSwitch>(R.id.swDark)
        switch.isChecked = Prefs.isDarkMode(this)
        switch.setOnCheckedChangeListener { _, checked ->
            Prefs.setDarkMode(this, checked)
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
        }
    }

    private fun setupFontSize() {
        val seekBar = findViewById<SeekBar>(R.id.sbFont)
        val valueView = findViewById<TextView>(R.id.tvFontValue)
        val preview = findViewById<TextView>(R.id.tvFontPreview)

        seekBar.max = Prefs.FONT_SIZE_MAX - Prefs.FONT_SIZE_MIN
        seekBar.progress = Prefs.fontSize(this) - Prefs.FONT_SIZE_MIN
        applyFontPreview(valueView, preview, Prefs.fontSize(this))

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = Prefs.FONT_SIZE_MIN + progress
                Prefs.setFontSize(this@SettingsActivity, size)
                applyFontPreview(valueView, preview, size)
            }

            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
    }

    private fun applyFontPreview(valueView: TextView, preview: TextView, size: Int) {
        valueView.text = getString(R.string.settings_font_value, size)
        preview.textSize = size.toFloat()
    }

    private fun setupSortOrder() {
        val group = findViewById<RadioGroup>(R.id.rgSort)
        when (Prefs.sortOrder(this)) {
            SortOrder.UPDATED -> group.check(R.id.rbUpdated)
            SortOrder.CREATED -> group.check(R.id.rbCreated)
            SortOrder.TITLE -> group.check(R.id.rbTitle)
        }
        group.setOnCheckedChangeListener { _, checkedId ->
            val order = when (checkedId) {
                R.id.rbCreated -> SortOrder.CREATED
                R.id.rbTitle -> SortOrder.TITLE
                else -> SortOrder.UPDATED
            }
            Prefs.setSortOrder(this, order)
        }
    }

    private fun versionName(): String {
        return runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        }.getOrDefault("1.0")
    }
}
