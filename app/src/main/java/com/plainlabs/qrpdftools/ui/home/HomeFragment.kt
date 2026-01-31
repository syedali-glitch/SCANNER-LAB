package com.plainlabs.qrpdftools.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.databinding.FragmentHomeBinding

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
        setupUI()
    }

    private fun setupUI() {
        binding.cardQrScanner.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_qr_scanner)
        }

        binding.cardDocScanner.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_doc_scanner)
        }

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
