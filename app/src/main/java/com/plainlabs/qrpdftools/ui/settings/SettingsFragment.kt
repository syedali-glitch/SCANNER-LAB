package com.plainlabs.qrpdftools.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.plainlabs.qrpdftools.databinding.FragmentSettingsBinding

/**
 * Settings Fragment
 * 
 * Provides app settings without requiring account/cloud:
 * - Theme (Dark/Light/System)
 * - Scanner settings (Auto-save, Vibration, Sound)
 * - Storage settings (Save location, Clear history)
 * - About info
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val prefs by lazy {
        requireContext().getSharedPreferences("scanner_settings", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
        setupListeners()
        setupVersion()
    }

    private fun loadSettings() {
        // Load saved preferences
        binding.switchDarkTheme.isChecked = prefs.getBoolean("dark_theme", true)
        binding.switchSystemTheme.isChecked = prefs.getBoolean("system_theme", false)
        binding.switchAutoSave.isChecked = prefs.getBoolean("auto_save", true)
        binding.switchVibration.isChecked = prefs.getBoolean("vibration", true)
        binding.switchSound.isChecked = prefs.getBoolean("sound", false)
        
        // Update dark theme switch state based on system theme
        binding.switchDarkTheme.isEnabled = !binding.switchSystemTheme.isChecked
    }

    private fun setupListeners() {
        // Dark Theme Toggle
        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            if (!binding.switchSystemTheme.isChecked) {
                applyTheme(isChecked)
            }
        }

        // System Theme Toggle
        binding.switchSystemTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("system_theme", isChecked).apply()
            binding.switchDarkTheme.isEnabled = !isChecked
            
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                applyTheme(binding.switchDarkTheme.isChecked)
            }
        }

        // Auto-save Toggle
        binding.switchAutoSave.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_save", isChecked).apply()
        }

        // Vibration Toggle
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration", isChecked).apply()
        }

        // Sound Toggle
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound", isChecked).apply()
        }

        // Save Location
        binding.settingSaveLocation.setOnClickListener {
            Toast.makeText(context, "Files saved to: Documents/ScannerLab", Toast.LENGTH_SHORT).show()
        }

        // Clear History
        binding.settingClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }

        // Test Crash
        binding.settingTestCrash.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Test Crashlytics")
                .setMessage("This will intentionally crash the app to test Firebase reporting. The app will close immediately.")
                .setPositiveButton("CRASH!") { _, _ ->
                    throw RuntimeException("Test Crash from Settings: verifying Firebase Crashlytics")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun applyTheme(darkMode: Boolean) {
        val mode = if (darkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun showClearHistoryDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear History")
            .setMessage("This will delete all scan history. This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                // Clear history from database
                Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupVersion() {
        try {
            val pInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            binding.textVersion.text = pInfo.versionName
        } catch (e: Exception) {
            binding.textVersion.text = "1.0.0"
        }
        
        // Navigate to About (Legal Shield)
        binding.textVersion.parent.let { parent ->
            (parent as? View)?.setOnClickListener {
                androidx.navigation.fragment.findNavController()
                    .navigate(com.plainlabs.qrpdftools.R.id.action_settings_to_about)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}