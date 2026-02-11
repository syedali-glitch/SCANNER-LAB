package com.scanner.lab.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.scanner.lab.R
import com.scanner.lab.databinding.ItemRecentScanBinding
import java.io.File
import java.util.Date
import java.util.Locale

class RecentAdapter(
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<RecentAdapter.RecentViewHolder>() {

    private var files = listOf<File>()

    fun submitList(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val binding = ItemRecentScanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount() = files.size

    inner class RecentViewHolder(private val binding: ItemRecentScanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File) {
            binding.tvRecentName.text = file.name
            
            // Calc time ago
            val now = System.currentTimeMillis()
            val diff = now - file.lastModified()
            val minutes = diff / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24
            
            val timeStr = when {
                minutes < 60 -> "$minutes m ago"
                hours < 24 -> "$hours h ago"
                else -> "$days d ago"
            }
            binding.tvRecentDate.text = timeStr

            // Icon logic
            val ext = file.extension.lowercase()
            val iconRes = when (ext) {
                "pdf" -> R.drawable.ic_tool_pdf_merge // Use PDF icon
                "jpg", "jpeg", "png" -> R.drawable.ic_scan_doc
                else -> R.drawable.ic_scan_doc
            }
            binding.ivThumbnail.setImageResource(iconRes)

            // Click
            binding.root.setOnClickListener { onClick(file) }
        }
    }
}
