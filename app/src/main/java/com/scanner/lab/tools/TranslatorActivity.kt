package com.scanner.lab.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
// import com.google.mlkit.common.model.DownloadConditions
// import com.google.mlkit.nlp.translate.TranslateLanguage
// import com.google.mlkit.nlp.translate.Translation
// import com.google.mlkit.nlp.translate.Translator
// import com.google.mlkit.nlp.translate.TranslatorOptions
import com.scanner.lab.BaseActivity
import com.scanner.lab.databinding.ActivityTranslatorBinding

class TranslatorActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = com.scanner.lab.databinding.ActivityTranslatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        Toast.makeText(this, "Translation feature momentarily unavailable due to build dependency issues.", Toast.LENGTH_LONG).show()
    }
}
