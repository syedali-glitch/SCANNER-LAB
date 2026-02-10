package com.scanner.lab

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.scanner.lab.util.LocaleHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}
