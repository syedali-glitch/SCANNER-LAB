package com.plainlabs.qrpdftools.ui.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.plainlabs.qrpdftools.databinding.FragmentImageEditorBinding
import com.plainlabs.qrpdftools.util.FilterRepository
import com.plainlabs.qrpdftools.util.PerspectiveTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * ImageEditorFragment - Central hub for high-end scanning processing.
 * 
 * Mandate: Perspective correction and filters.
 */
class ImageEditorFragment : Fragment() {

    private var _binding: FragmentImageEditorBinding? = null
    private val binding get() = _binding!!

    private var originalBitmap: Bitmap? = null
    private var processedBitmap: Bitmap? = null
    private var imageUriStr: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentImageEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        imageUriStr = arguments?.getString("imageUri")
        if (imageUriStr == null) {
            findNavController().popBackStack()
            return
        }

        loadOriginalImage()
        setupListeners()
    }

    private fun loadOriginalImage() {
        lifecycleScope.launch {
            binding.progressEditor.visibility = View.VISIBLE
            try {
                val uri = Uri.parse(imageUriStr)
                withContext(Dispatchers.IO) {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    originalBitmap = BitmapFactory.decodeStream(inputStream)
                }
                processedBitmap = originalBitmap
                binding.ivPreview.setImageBitmap(originalBitmap)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressEditor.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.btnFilterNone.setOnClickListener { 
            processedBitmap = originalBitmap
            binding.ivPreview.setImageBitmap(processedBitmap)
        }

        binding.btnFilterMagic.setOnClickListener { applyFilter("magic") }
        binding.btnFilterBW.setOnClickListener { applyFilter("bw") }
        binding.btnFilterGray.setOnClickListener { applyFilter("gray") }

        binding.btnRectify.setOnClickListener { rectify() }

        binding.btnSave.setOnClickListener { saveAndExit() }
    }

    private fun applyFilter(filterType: String) {
        val bitmapToProcess = originalBitmap ?: return // Always process from the rectified/original base
        lifecycleScope.launch {
            binding.progressEditor.visibility = View.VISIBLE
            val result = withContext(Dispatchers.Default) {
                when (filterType) {
                    "magic" -> FilterRepository.applyMagicColor(bitmapToProcess)
                    "bw" -> FilterRepository.applyBlackAndWhite(bitmapToProcess)
                    "gray" -> FilterRepository.applyGrayscale(bitmapToProcess)
                    else -> bitmapToProcess
                }
            }
            processedBitmap = result
            binding.ivPreview.setImageBitmap(processedBitmap)
            binding.progressEditor.visibility = View.GONE
        }
    }

    private fun rectify() {
        val bitmapToRectify = originalBitmap ?: return
        val points = binding.cropView.getCropPoints()
        
        lifecycleScope.launch {
            binding.progressEditor.visibility = View.VISIBLE
            try {
                // Map CropView points to actual bitmap coordinates
                val viewWidth = binding.cropView.width
                val viewHeight = binding.cropView.height
                val bitmapWidth = bitmapToRectify.width
                val bitmapHeight = bitmapToRectify.height
                
                val scaleX = bitmapWidth.toFloat() / viewWidth
                val scaleY = bitmapHeight.toFloat() / viewHeight
                
                val mappedPoints = points.map { android.graphics.PointF(it.x * scaleX, it.y * scaleY) }
                
                val result = withContext(Dispatchers.Default) {
                    PerspectiveTransformation.rectifyDocument(bitmapToRectify, mappedPoints)
                }
                
                originalBitmap = result
                processedBitmap = result
                binding.ivPreview.setImageBitmap(result)
                binding.cropView.visibility = View.GONE // Successfully rectified, hide crop handles
                
            } catch (e: Exception) {
                Toast.makeText(context, "Rectification failed", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressEditor.visibility = View.GONE
            }
        }
    }

    private fun saveAndExit() {
        val finalBitmap = processedBitmap ?: return
        lifecycleScope.launch {
            binding.progressEditor.visibility = View.VISIBLE
            try {
                val file = File(requireContext().cacheDir, "scan_result_${System.currentTimeMillis()}.jpg")
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { out ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                }
                // Transition logic: Typically this would go to a batch collector or result screen
                Toast.makeText(context, "Scan processed successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() 
            } catch (e: Exception) {
                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressEditor.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Manual cleanup to help GC with large bitmaps as per Arch rule
        originalBitmap?.recycle()
        processedBitmap?.recycle()
        originalBitmap = null
        processedBitmap = null
    }
}
