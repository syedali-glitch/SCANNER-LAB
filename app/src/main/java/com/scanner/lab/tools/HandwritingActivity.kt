package com.scanner.lab.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
import com.scanner.lab.BaseActivity
import com.scanner.lab.databinding.ActivityHandwritingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HandwritingActivity : BaseActivity() {

    private lateinit var binding: ActivityHandwritingBinding
    private var model: DigitalInkRecognitionModel? = null
    private var recognizer: DigitalInkRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHandwritingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Edge to Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        initializeModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnClear.setOnClickListener {
            binding.drawingView.clear()
        }
        
        binding.btnRecognize.setOnClickListener {
            recognizeInk()
        }
    }

    private fun initializeModel() {
        // Use English for now
        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
        if (modelIdentifier == null) {
            binding.tvStatus.text = "Model Error: Language not supported"
            return
        }
        
        model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        
        val remoteModelManager = RemoteModelManager.getInstance()
        
        remoteModelManager.isModelDownloaded(model!!)
            .addOnSuccessListener { isDownloaded ->
                if (isDownloaded) {
                    binding.tvStatus.text = "Model Ready (Offline)"
                    binding.btnRecognize.isEnabled = true
                    initializeRecognizer()
                } else {
                    binding.tvStatus.text = "Downloading Model..."
                    downloadModel()
                }
            }
            .addOnFailureListener {
                binding.tvStatus.text = "Error checking model"
            }
    }
    
    private fun downloadModel() {
        model?.let { m ->
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
                
            RemoteModelManager.getInstance().download(m, conditions)
                .addOnSuccessListener {
                    binding.tvStatus.text = "Model Downloaded!"
                    binding.btnRecognize.isEnabled = true
                    initializeRecognizer()
                }
                .addOnFailureListener { e ->
                    binding.tvStatus.text = "Download Failed: ${e.message}"
                }
        }
    }
    
    private fun initializeRecognizer() {
        model?.let { m ->
            recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(m).build())
        }
    }

    private fun recognizeInk() {
        val ink = binding.drawingView.getInk()
        if (ink.strokes.isEmpty()) {
            Toast.makeText(this, "Please write something first", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressLayout.visibility = View.VISIBLE
        binding.btnRecognize.isEnabled = false
        
        recognizer?.recognize(ink)
            ?.addOnSuccessListener { result ->
                binding.progressLayout.visibility = View.GONE
                binding.btnRecognize.isEnabled = true
                if (result.candidates.isNotEmpty()) {
                    val text = result.candidates[0].text
                    showResultDialog(text)
                } else {
                    Toast.makeText(this, "No text recognized", Toast.LENGTH_SHORT).show()
                }
            }
            ?.addOnFailureListener { e ->
                binding.progressLayout.visibility = View.GONE
                binding.btnRecognize.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showResultDialog(text: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Result")
            .setMessage(text)
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Handwriting", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .create()
            
        dialog.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        recognizer?.close()
    }
}
