package com.plainlabs.qrpdftools.ui.pdftools

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.plainlabs.qrpdftools.databinding.FragmentPdfToolsBinding
import com.plainlabs.qrpdftools.util.PdfProcessor
import com.plainlabs.qrpdftools.util.PdfUtilityTools
import com.plainlabs.qrpdftools.util.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfToolsFragment : Fragment() {

    private var _binding: FragmentPdfToolsBinding? = null
    private val binding get() = _binding!!

    // Action types to track current operation
    private enum class ActionType { MERGE, SPLIT, COMPRESS, WATERMARK, PROTECT, ROTATE }
    private var currentAction: ActionType? = null

    // Launchers
    private val pickFilesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleFileSelection(result.data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfToolsBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        binding.cardMerge.setOnClickListener { 
            currentAction = ActionType.MERGE
            pickFiles(allowMultiple = true) 
        }
        
        binding.cardSplit.setOnClickListener { 
            currentAction = ActionType.SPLIT
            pickFiles(allowMultiple = false) 
        }
        
        binding.cardCompress.setOnClickListener { 
            currentAction = ActionType.COMPRESS
            pickFiles(allowMultiple = false) 
        }
        
        binding.cardWatermark.setOnClickListener { 
            currentAction = ActionType.WATERMARK
            pickFiles(allowMultiple = false) 
        }
        
        binding.cardProtect.setOnClickListener { 
            currentAction = ActionType.PROTECT
            pickFiles(allowMultiple = false) 
        }
        
        binding.cardRotate.setOnClickListener { 
            currentAction = ActionType.ROTATE
            pickFiles(allowMultiple = false) 
        }
    }

    private fun pickFiles(allowMultiple: Boolean) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        }
        pickFilesLauncher.launch(intent)
    }

    private fun handleFileSelection(data: Intent?) {
        val uris = mutableListOf<Uri>()
        data?.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                uris.add(clipData.getItemAt(i).uri)
            }
        } ?: data?.data?.let { uri ->
            uris.add(uri)
        }

        if (uris.isEmpty()) return

        when (currentAction) {
            ActionType.MERGE -> performMerge(uris)
            ActionType.SPLIT -> showSplitDialog(uris.first())
            ActionType.COMPRESS -> performCompress(uris.first())
            ActionType.WATERMARK -> showWatermarkDialog(uris.first())
            ActionType.PROTECT -> showProtectDialog(uris.first())
            ActionType.ROTATE -> performRotate(uris.first())
            else -> {}
        }
    }
    
    // ------------------------------------------------------------------------
    // OPERATIONS
    // ------------------------------------------------------------------------

    private fun performMerge(uris: List<Uri>) {
        if (uris.size < 2) {
            Toast.makeText(context, "Select at least 2 PDF files", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                Toast.makeText(context, "Merging...", Toast.LENGTH_SHORT).show()
                val files = uris.mapNotNull { uri -> StorageUtils.copyToCache(requireContext(), uri) }
                val outputFile = File(requireContext().filesDir, "merged_${System.currentTimeMillis()}.pdf")
                
                val success = PdfProcessor(requireContext()).mergePdfs(files, outputFile)

                if (success) {
                    onSuccess(outputFile)
                } else {
                    Toast.makeText(context, "Merge failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSplitDialog(uri: Uri) {
        // Simple fixed split for MVP (Split at Page 1) or TODO
        // For now, implementing simple 1-page split
        val input = EditText(context).apply { hint = "End Page (e.g. 1)" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Split PDF")
            .setMessage("Extract pages from 1 to:")
            .setView(input)
            .setPositiveButton("Split") { _, _ ->
                val endPage = input.text.toString().toIntOrNull() ?: 1
                performSplit(uri, 1, endPage)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSplit(uri: Uri, start: Int, end: Int) {
        lifecycleScope.launch {
            try {
                val file = StorageUtils.copyToCache(requireContext(), uri) ?: return@launch
                val outputFile = File(requireContext().filesDir, "split_${System.currentTimeMillis()}.pdf")
                
                val success = PdfProcessor(requireContext()).splitPdf(file, start, end, outputFile)
                
                if (success) onSuccess(outputFile)
                else Toast.makeText(context, "Split failed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performCompress(uri: Uri) {
         lifecycleScope.launch {
            try {
                Toast.makeText(context, "Compressing...", Toast.LENGTH_SHORT).show()
                val file = StorageUtils.copyToCache(requireContext(), uri) ?: return@launch
                val outputFile = File(requireContext().filesDir, "compressed_${System.currentTimeMillis()}.pdf")
                
                val ratio = PdfUtilityTools.compressPdf(file, outputFile, 0.5f)
                
                Toast.makeText(context, "Compressed by ${(ratio * 100).toInt()}%", Toast.LENGTH_LONG).show()
                onSuccess(outputFile)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showWatermarkDialog(uri: Uri) {
        val input = EditText(context).apply { hint = "Watermark Text" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Watermark")
            .setView(input)
            .setPositiveButton("Apply") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) performWatermark(uri, text)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performWatermark(uri: Uri, text: String) {
        lifecycleScope.launch {
            try {
                val file = StorageUtils.copyToCache(requireContext(), uri) ?: return@launch
                val outputFile = File(requireContext().filesDir, "watermarked_${System.currentTimeMillis()}.pdf")
                
                PdfUtilityTools.watermarkPdf(file, outputFile, text)
                onSuccess(outputFile)
            } catch (e: Exception) {
                 Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showProtectDialog(uri: Uri) {
        val input = EditText(context).apply { hint = "Password" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Protect PDF")
            .setView(input)
            .setPositiveButton("Protect") { _, _ ->
                val pass = input.text.toString()
                if (pass.isNotEmpty()) performProtect(uri, pass)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performProtect(uri: Uri, pass: String) {
        lifecycleScope.launch {
             try {
                val file = StorageUtils.copyToCache(requireContext(), uri) ?: return@launch
                val outputFile = File(requireContext().filesDir, "protected_${System.currentTimeMillis()}.pdf")
                
                PdfUtilityTools.protectPdf(file, outputFile, pass, pass)
                onSuccess(outputFile)
            } catch (e: Exception) {
                 Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performRotate(uri: Uri) {
        val options = arrayOf("90° Clockwise", "180°", "270° Counter-clockwise")
        val degrees = intArrayOf(90, 180, 270)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rotate PDF")
            .setItems(options) { _, which ->
                val rotation = degrees[which]
                executeRotation(uri, rotation)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeRotation(uri: Uri, degrees: Int) {
        lifecycleScope.launch {
            try {
                Toast.makeText(context, "Rotating...", Toast.LENGTH_SHORT).show()
                val file = StorageUtils.copyToCache(requireContext(), uri) ?: return@launch
                val outputFile = File(requireContext().filesDir, "rotated_${System.currentTimeMillis()}.pdf")
                
                val success = PdfProcessor(requireContext()).rotatePdf(file, outputFile, degrees)
                
                if (success) onSuccess(outputFile)
                else Toast.makeText(context, "Rotation failed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ------------------------------------------------------------------------
    // UTILS
    // ------------------------------------------------------------------------

    private fun onSuccess(file: File) {
        // Open file share/view intent
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Open Result"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}