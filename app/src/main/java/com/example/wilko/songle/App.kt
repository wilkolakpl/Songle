package com.example.wilko.songle

import android.app.Application
import android.util.Log
import com.example.wilko.songle.utils.DelegatesExt
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*

/**
 * Created by wilko on 12/7/2017.
 *
 *
 * This class, which keeps the instance of the application, is Antonio Leiva's implementation.
 * https://github.com/antoniolg/Kotlin-for-Android-Developers/blob/master/app/src/main/java/com/antonioleiva/weatherapp/ui/App.kt
 */

class App : Application() {

    companion object {
        var instance: App by DelegatesExt.notNullSingleValue()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // set up localization
        val lang = defaultSharedPreferences.getString("language", "en")
        val sysLang = defaultSharedPreferences.getBoolean("system_language", true)
        val systemLocale = Locale.getDefault().language
        if (lang != "" && systemLocale != lang && !sysLang) {
            val locale = Locale(lang)
            resources.configuration.locale = locale
            resources.updateConfiguration(resources.configuration, resources.displayMetrics)
        }
    }
}