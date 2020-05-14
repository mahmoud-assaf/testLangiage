package com.mahmoud.kozbara

import android.app.Application
import android.preference.PreferenceManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import java.util.*

class MyApplication:Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        updateResources()
    }

    private fun updateResources():Boolean {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val language = sharedPreferences.getString("language", "ar")


        BaseActivity.dLocale = Locale(language) //set any locale you want here
        return true
    }
}