package com.scanner.lab

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.scanner.lab.util.LocaleHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        com.scanner.lab.utils.AntiReverseEngineering.check(this)
    }
}
