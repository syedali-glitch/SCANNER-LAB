package com.plainlabs.qrpdftools.ui.converter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.plainlabs.qrpdftools.databinding.FragmentFileConverterBinding
import com.plainlabs.qrpdftools.util.*
import com.plainlabs.qrpdftools.conversion.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileConverterFragment : Fragment() {

    private var _binding: FragmentFileConverterBinding? = null
    private val binding get() = _binding!!

    private var selectedFileUri: Uri? = null
    private var selectedFormat = OutputFormat.PDF

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                binding.tvSelectedFile.text = getFileName(uri)
                binding.btnConvert.isEnabled = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileConverterBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        binding.btnSelectFile.setOnClickListener {
            pickFile()
        }

        binding.radioGroupFormat.setOnCheckedChangeListener { _, checkedId ->
            selectedFormat = when (checkedId) {
                binding.rbPdf.id -> OutputFormat.PDF
                binding.rbDocx.id -> OutputFormat.DOCX
                binding.rbExcel.id -> OutputFormat.EXCEL
                else -> OutputFormat.PDF
            }
            updateDisclaimerVisibility()
        }

        binding.btnConvert.setOnClickListener {
            convertFile()
        }
    }

    private fun updateDisclaimerVisibility() {
        val isPdfToWord = selectedFormat == OutputFormat.DOCX && selectedFileUri?.let { getFileName(it).endsWith(".pdf", true) } == true
        binding.tvDisclaimer.visibility = if (isPdfToWord) View.VISIBLE else View.GONE
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "image/*"
            ))
        }
        filePicker.launch(intent)
    }

    private fun getFileName(uri: Uri): String {
        var name = "Unknown"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            name = cursor.getString(nameIndex)
        }
        return name
    }

    private fun convertFile() {
        val uri = selectedFileUri ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnConvert.isEnabled = false

        lifecycleScope.launch {
            try {
                // Copy file to temp location
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val fileName = getFileName(uri)
                val tempFile = File(requireContext().cacheDir, fileName)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Detect input format and perform conversion
                val inputFormat = detectFileFormat(fileName)
                val outputExtension = when (selectedFormat) {
                    OutputFormat.PDF -> "pdf"
                    OutputFormat.DOCX -> "docx"
                    OutputFormat.EXCEL -> "xlsx"
                }

                val outputPath = "${requireContext().filesDir}/converted_${System.currentTimeMillis()}.$outputExtension"
                val outputFile = File(outputPath)

                // Perform conversion based on input and output formats
                val success = performConversion(tempFile, outputFile, inputFormat, selectedFormat)

                binding.progressBar.visibility = View.GONE

                if (success) {
                    Toast.makeText(context, "Conversion successful!\nSaved to: ${outputFile.name}", Toast.LENGTH_LONG).show()
                    binding.btnConvert.isEnabled = true
                    selectedFileUri = null
                    binding.tvSelectedFile.text = "No file selected"

                    // Open converted file
                    openConvertedFile(outputFile)
                } else {
                    Toast.makeText(context, "Conversion failed", Toast.LENGTH_SHORT).show()
                    binding.btnConvert.isEnabled = true
                }

                // Cleanup temp file
                tempFile.delete()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnConvert.isEnabled = true
                ErrorHandler.handleError("FileConverter", e, "Conversion failed")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun detectFileFormat(fileName: String): InputFormat {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> InputFormat.PDF
            fileName.endsWith(".docx", ignoreCase = true) -> InputFormat.DOCX
            fileName.endsWith(".xlsx", ignoreCase = true) -> InputFormat.EXCEL
            fileName.endsWith(".jpg", ignoreCase = true) || 
            fileName.endsWith(".jpeg", ignoreCase = true) ||
            fileName.endsWith(".png", ignoreCase = true) -> InputFormat.IMAGE
            else -> InputFormat.IMAGE
        }
    }

    private suspend fun performConversion(
        inputFile: File,
        outputFile: File,
        inputFormat: InputFormat,
        outputFormat: OutputFormat
    ): Boolean = withContext(CoroutineOptimizer.OptimizedIO) {
        return@withContext try {
            when {
                // PDF conversions with optimized I/O
                inputFormat == InputFormat.PDF && outputFormat == OutputFormat.DOCX -> {
                    PdfConverter.extractPdfTextToWord(inputFile.absolutePath, outputFile.absolutePath, requireContext())
                }
                inputFormat == InputFormat.PDF && outputFormat == OutputFormat.PDF -> {
                    // Use zero-copy for same format
                    FastFileProcessor.fastCopy(inputFile, outputFile)
                    true
                }

                // DOCX conversions
                inputFormat == InputFormat.DOCX && outputFormat == OutputFormat.PDF -> {
                    DocxConverter.docxToPdf(inputFile.absolutePath, outputFile.absolutePath)
                }
                inputFormat == InputFormat.DOCX && outputFormat == OutputFormat.DOCX -> {
                    FastFileProcessor.fastCopy(inputFile, outputFile)
                    true
                }

                // Image conversions with memory optimization
                inputFormat == InputFormat.IMAGE && outputFormat == OutputFormat.PDF -> {
                    PdfConverter.imageToPdf(listOf(inputFile.absolutePath), outputFile.absolutePath)
                }
                inputFormat == InputFormat.IMAGE && outputFormat == OutputFormat.DOCX -> {
                    val ocrResult = OcrEngine.performOcr(inputFile.absolutePath, requireContext())
                    ProfessionalDocxConverter.createProfessionalDocx(
                        ocrResult.text,
                        outputFile.absolutePath,
                        "Scanned Document"
                    )
                }
                inputFormat == InputFormat.IMAGE && outputFormat == OutputFormat.EXCEL -> {
                    val ocrResult = OcrEngine.performOcr(inputFile.absolutePath, requireContext())
                    ExcelConverter.ocrToExcel(ocrResult, outputFile.absolutePath)
                }

                // Excel conversions
                inputFormat == InputFormat.EXCEL && outputFormat == OutputFormat.PDF -> {
                    ExcelConverter.excelToPdf(inputFile.absolutePath, outputFile.absolutePath)
                }
                inputFormat == InputFormat.EXCEL && outputFormat == OutputFormat.EXCEL -> {
                    FastFileProcessor.fastCopy(inputFile, outputFile)
                    true
                }

                else -> false
            }
        } catch (e: Exception) {
            ErrorHandler.handleError("Conversion", e, "Conversion error: ${e.message}")
            false
        }
    }

    private fun openConvertedFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            ErrorHandler.handleError("OpenFile", e, "Could not open file")
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "*/*"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

enum class InputFormat {
    PDF, DOCX, EXCEL, IMAGE
}

enum class OutputFormat {
    PDF, DOCX, EXCEL
}