package com.plainlabs.qrpdftools.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.databinding.FragmentHomeBinding

/**
 * Home Hub Fragment - Main Menu
 * 
 * Primary navigation hub for the app with two main sections:
 * 1. Scanner Hub - QR codes, barcodes, documents
 * 2. File Converter - Upload, convert, download
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // ═══════════════════════════════════════════════════════════════════
        // SCANNER SECTION
        // ═══════════════════════════════════════════════════════════════════
        
        // Main Scanner Hub card (opens QR scanner by default)
        binding.cardScannerHub.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_qr_scanner)
        }
        
        // Quick access: QR Code scanner
        binding.cardQrScanner.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_qr_scanner)
        }

        // Quick access: Document scanner
        binding.cardDocScanner.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_doc_scanner)
        }
        
        // Quick access: Barcode scanner (uses same QR scanner - ML Kit handles both)
        binding.cardBarcodeScanner.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_qr_scanner)
        }

        // ═══════════════════════════════════════════════════════════════════
        // FILE CONVERTER SECTION
        // ═══════════════════════════════════════════════════════════════════
        
        binding.cardFileConverter.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_file_converter)
        }

        // ═══════════════════════════════════════════════════════════════════
        // FILE VIEWER/EDITOR SECTION
        // ═══════════════════════════════════════════════════════════════════
        
        binding.cardFileViewer.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_file_viewer)
        }

        // ═══════════════════════════════════════════════════════════════════
        // UTILITY SECTION
        // ═══════════════════════════════════════════════════════════════════
        
        binding.cardHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }

        binding.cardSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
