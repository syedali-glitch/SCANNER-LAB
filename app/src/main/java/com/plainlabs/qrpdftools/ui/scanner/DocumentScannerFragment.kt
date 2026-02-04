package com.plainlabs.qrpdftools.ui.scanner

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.plainlabs.qrpdftools.databinding.FragmentDocumentScannerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.camera.core.Camera
import com.plainlabs.qrpdftools.R

class DocumentScannerFragment : Fragment() {

    private var _binding: FragmentDocumentScannerBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var isFlashOn = false

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bundle = Bundle().apply {
                putString("imageUri", it.toString())
            }
            findNavController().navigate(R.id.action_doc_scanner_to_editor, bundle)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request camera permissions
        if (PermissionHelper.hasCameraPermission(requireContext())) {
            startCamera()
        } else {
            PermissionHelper.requestCameraPermission(this)
        }

        setupUI()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        binding.scanOverlay.startScanning()
    }

    override fun onPause() {
        super.onPause()
        binding.scanOverlay.stopScanning()
    }

    private fun setupUI() {
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }
        
        binding.scanModeGroup.setOnCheckedChangeListener { group, checkedId ->
            // Handle scanning modes
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                ErrorHandler.handleError(TAG, exc, "Camera initialization failed")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        binding.progressLoading.visibility = View.VISIBLE

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri ?: return
                    
                    // Premium Feedback
                    binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                    android.media.MediaActionSound().play(android.media.MediaActionSound.SHUTTER_CLICK)
                    
                    binding.progressLoading.visibility = View.GONE
                    
                    // Navigate to Premium Editor
                    val bundle = Bundle().apply {
                        putString("imageUri", uri.toString())
                    }
                    findNavController().navigate(com.plainlabs.qrpdftools.R.id.action_doc_scanner_to_editor, bundle)
                }
            }
        )
    }

    private fun toggleFlash() {
        camera?.let {
            isFlashOn = !isFlashOn
            it.cameraControl.enableTorch(isFlashOn)
            // Feedback
            val icon = if (isFlashOn) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_camera
            binding.btnFlash.setIconResource(icon)
            Toast.makeText(context, if (isFlashOn) "Flash ON" else "Flash OFF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    // Handle permissions result
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001) {
            if (PermissionHelper.hasCameraPermission(requireContext())) {
                startCamera()
            } else {
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                // finish() or navigate back
            }
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}