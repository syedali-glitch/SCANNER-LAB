package com.plainlabs.qrpdftools.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.databinding.BottomSheetHistoryBinding
import com.plainlabs.qrpdftools.ui.history.ScanAdapter
import kotlinx.coroutines.launch

class FavoritesBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FavoritesViewModel by viewModels {
        FavoritesViewModel.Factory(requireActivity().application)
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
        
        binding.tvTitle.text = getString(R.string.favorites)
        binding.etSearch.visibility = View.GONE
        
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
        
        // Setup swipe to unfavorite
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
                viewModel.toggleFavorite(scan)
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }
    
    private fun setupListeners() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.favorites.collect { favorites ->
                adapter.submitList(favorites)
                
                if (favorites.isEmpty()) {
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
