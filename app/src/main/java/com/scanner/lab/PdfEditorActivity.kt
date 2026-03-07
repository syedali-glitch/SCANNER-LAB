package com.scanner.lab

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scanner.lab.databinding.ActivityPdfEditorBinding
import com.scanner.lab.utils.PdfUtilityTools
import com.scanner.lab.utils.ScopedStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfEditorActivity : BaseActivity() {

    private lateinit var binding: ActivityPdfEditorBinding
    private var pdfUri: Uri? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var cachedPdfFile: File? = null
    
    private val overlays = mutableListOf<PdfUtilityTools.TextOverlay>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pdfUri = intent.data
        if (pdfUri == null) {
            Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        loadPdf()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnAddText.setOnClickListener { showAddTextDialog() }
        binding.btnSave.setOnClickListener { saveEditedPdf() }
    }

    private fun loadPdf() {
        binding.progressLayout.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                cachedPdfFile = ScopedStorageHelper.copyUriToCache(this@PdfEditorActivity, pdfUri!!, "pdf")
                val pfd = ParcelFileDescriptor.open(cachedPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(pfd)
                
                withContext(Dispatchers.Main) {
                    renderPage(0)
                    binding.progressLayout.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PdfEditorActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun renderPage(pageIndex: Int) {
        pdfRenderer?.let { renderer ->
            currentPage?.close()
            currentPage = renderer.openPage(pageIndex)
            
            val bitmap = Bitmap.createBitmap(currentPage!!.width, currentPage!!.height, Bitmap.Config.ARGB_8888)
            currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            binding.pdfPageView.setImageBitmap(bitmap)
        }
    }

    private fun showAddTextDialog() {
        val input = EditText(this).apply { hint = "Type text here..." }
        AlertDialog.Builder(this)
            .setTitle("Add Text overlay")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    addNewTextOverlay(text)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewTextOverlay(text: String) {
        // For v1.8, we implement a simple center-page addition. 
        // Real dragging logic requires custom touch handling in overlayContainer.
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(android.graphics.Color.BLACK)
            textSize = 18f
            setPadding(8, 8, 8, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#44FFFFFF"))
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = binding.overlayContainer.width / 2
            topMargin = binding.overlayContainer.height / 2
        }

        binding.overlayContainer.addView(tv, params)
        
        // Make it simple draggable for now
        tv.setOnTouchListener(object : View.OnTouchListener {
            var dX = 0f
            var dY = 0f
            override fun onTouch(view: View, event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        view.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                }
                return true
            }
        })
    }

    private fun saveEditedPdf() {
        if (cachedPdfFile == null) return
        
        binding.progressLayout.visibility = View.VISIBLE
        binding.tvProgressMsg.text = "Saving PDF..."
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val finalOverlays = mutableListOf<PdfUtilityTools.TextOverlay>()
                
                // Map screen coordinates to PDF coordinates
                // Note: This is an estimation. Real PDF coord systems are 72 DPI bottom-up.
                // We simplify for the user demo.
                withContext(Dispatchers.Main) {
                    val containerWidth = binding.overlayContainer.width.toFloat()
                    val containerHeight = binding.overlayContainer.height.toFloat()
                    val pdfWidth = currentPage!!.width.toFloat()
                    val pdfHeight = currentPage!!.height.toFloat()
                    
                    for (i in 0 until binding.overlayContainer.childCount) {
                        val view = binding.overlayContainer.getChildAt(i) as TextView
                        
                        // Normalized coords (0 to 1)
                        val normX = view.x / containerWidth
                        val normY = (containerHeight - view.y - view.height) / containerHeight
                        
                        finalOverlays.add(PdfUtilityTools.TextOverlay(
                            text = view.text.toString(),
                            x = normX * pdfWidth,
                            y = normY * pdfHeight,
                            fontSize = 18f, // Fixed for now
                            pageNumber = 1 // Fixed to first page for v1.8
                        ))
                    }
                }
                
                val outputName = "Edited_${System.currentTimeMillis()}.pdf"
                val outputUri = ScopedStorageHelper.createDocumentUri(this@PdfEditorActivity, outputName)!!
                val tempOut = ScopedStorageHelper.createCacheFile(this@PdfEditorActivity, "pdf")
                
                val result = PdfUtilityTools.applyTextOverlays(
                    cachedPdfFile!!.absolutePath,
                    tempOut.absolutePath,
                    finalOverlays
                )
                
                if (result.isSuccess) {
                    contentResolver.openOutputStream(outputUri)?.use { out ->
                        tempOut.inputStream().copyTo(out)
                    }
                    ScopedStorageHelper.finalizeFile(this@PdfEditorActivity, outputUri)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PdfEditorActivity, "Success! Saved to Documents", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PdfEditorActivity, "Save Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressLayout.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentPage?.close()
        pdfRenderer?.close()
    }
}
