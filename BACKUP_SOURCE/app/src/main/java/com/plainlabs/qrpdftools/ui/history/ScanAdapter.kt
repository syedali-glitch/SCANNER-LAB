package com.plainlabs.qrpdftools.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.databinding.ItemScanBinding
import com.plainlabs.qrpdftools.domain.model.ScanResult

class ScanAdapter(
    private val onCopyClick: (ScanResult) -> Unit,
    private val onShareClick: (ScanResult) -> Unit,
    private val onFavoriteClick: (ScanResult) -> Unit
) : ListAdapter<ScanResult, ScanAdapter.ScanViewHolder>(ScanDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val binding = ItemScanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ScanViewHolder(
        private val binding: ItemScanBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(scan: ScanResult) {
            binding.tvContent.text = scan.shortContent
            binding.tvType.text = scan.typeDisplay
            binding.tvTimestamp.text = scan.formattedTimestamp
            
            // Set favorite icon
            if (scan.isFavorite) {
                binding.ivFavorite.setImageResource(R.drawable.ic_star)
                binding.ivFavorite.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.favorite_yellow)
                )
            } else {
                binding.ivFavorite.setImageResource(R.drawable.ic_star_outline)
                binding.ivFavorite.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.text_gray)
                )
            }
            
            // Click listeners
            binding.ivFavorite.setOnClickListener {
                onFavoriteClick(scan)
            }
            
            binding.btnCopy.setOnClickListener {
                onCopyClick(scan)
            }
            
            binding.btnShare.setOnClickListener {
                onShareClick(scan)
            }
        }
    }
    
    private class ScanDiffCallback : DiffUtil.ItemCallback<ScanResult>() {
        override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem == newItem
        }
    }
}
