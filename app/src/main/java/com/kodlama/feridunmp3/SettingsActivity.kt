package com.kodlama.feridunmp3

import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.settings)

        val group = findViewById<RadioGroup>(R.id.lang_group)
        val current = AppCompatDelegate.getApplicationLocales()
        val lang = if (current.isEmpty) "" else current[0]?.language.orEmpty()
        group.check(
            when (lang) {
                "tr" -> R.id.lang_tr
                "fa" -> R.id.lang_fa
                "en" -> R.id.lang_en
                else -> R.id.lang_system
            }
        )
        group.setOnCheckedChangeListener { _, checkedId ->
            val locales = when (checkedId) {
                R.id.lang_tr -> LocaleListCompat.forLanguageTags("tr")
                R.id.lang_fa -> LocaleListCompat.forLanguageTags("fa")
                R.id.lang_en -> LocaleListCompat.forLanguageTags("en")
                else -> LocaleListCompat.getEmptyLocaleList()
            }
            // Aktiviteyi yeniden oluşturur; API 32 ve altında AppCompat kalıcı saklar.
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
