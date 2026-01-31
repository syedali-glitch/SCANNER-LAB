package com.plainlabs.qrpdftools.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.plainlabs.qrpdftools.databinding.BottomSheetHistoryBinding
import com.plainlabs.qrpdftools.domain.model.ScanResult
import kotlinx.coroutines.launch

class HistoryBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.Theme_QRPDFTools_BottomSheetDialog
    
    private var _binding: BottomSheetHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModel.Factory(requireActivity().application)
    }
    
    private lateinit var adapter: ScanAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = ScanAdapter(
            onCopyClick = { scan ->
                viewModel.copyScanToClipboard(requireContext(), scan)
            },
            onShareClick = { scan ->
                viewModel.shareScan(requireContext(), scan)
            },
            onFavoriteClick = { scan ->
                viewModel.toggleFavorite(scan)
            }
        )
        
        binding.recyclerView.adapter = adapter
        
        // Setup swipe to delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val scan = adapter.currentList[position]
                
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Delete
                        viewModel.deleteScan(scan)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Toggle favorite
                        viewModel.toggleFavorite(scan)
                    }
                }
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }
    
    private fun setupListeners() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }
        
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchScans(s.toString())
            }
        })
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.scans.collect { scans ->
                adapter.submitList(scans)
                
                if (scans.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
