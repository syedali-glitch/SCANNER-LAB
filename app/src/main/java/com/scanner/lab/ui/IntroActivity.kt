package com.scanner.lab.ui

import android.content.Intent
import android.os.Bundle
import com.scanner.lab.BaseActivity
import com.scanner.lab.databinding.ActivityIntroBinding

class IntroActivity : BaseActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_ICON = "extra_icon"
        const val EXTRA_TARGET_INTENT = "extra_target_intent"
    }

    private lateinit var binding: ActivityIntroBinding

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Feature"
        val desc = intent.getStringExtra(EXTRA_DESC) ?: "Description"
        val iconRes = intent.getIntExtra(EXTRA_ICON, com.scanner.lab.R.drawable.ic_scan_doc)
        
        val targetIntent = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_TARGET_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>(EXTRA_TARGET_INTENT)
        }

        if (targetIntent == null) {
            android.util.Log.e("IntroActivity", "Target Intent is NULL!")
        }

        binding.tvTitle.text = title
        binding.tvDescription.text = desc
        binding.ivIcon.setImageResource(iconRes)
        
        binding.btnStart.setOnClickListener {
            if (targetIntent != null) {
                startActivity(targetIntent)
            }
            finish()
        }
        
        binding.btnClose.setOnClickListener {
            finish()
        }
    }
}
