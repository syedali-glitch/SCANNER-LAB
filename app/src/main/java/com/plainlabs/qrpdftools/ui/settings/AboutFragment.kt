package com.plainlabs.qrpdftools.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.plainlabs.qrpdftools.databinding.FragmentAboutBinding

/**
 * AboutFragment - PlainLabs Legal Shield
 * 
 * Displays proprietary branding, "Patent Pending" status, and license placeholders
 * as mandated by the PlainLabs Architect.
 */
class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVersionInfo()
    }

    private fun setupVersionInfo() {
        try {
            val pInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            binding.textVersion.text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            binding.textVersion.text = "Version 1.0.0"
        }
        
        // Architect Rule: Hardcoded signature status
        binding.textPatentStatus.text = "PlainLabs Scanner - Patent Pending"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
