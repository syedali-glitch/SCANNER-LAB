package com.plainlabs.qrpdftools.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.data.local.entity.ScanType
import com.plainlabs.qrpdftools.databinding.FragmentQrScannerBinding
import com.plainlabs.qrpdftools.ui.main.MainViewModel
import com.plainlabs.qrpdftools.util.ErrorHandler
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerFragment : Fragment() {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var scanningEnabled = true

    private val viewModel: MainViewModel by viewModels(
        ownerProducer = { requireActivity() }
    ) {
        MainViewModel.Factory(requireActivity().application)
    }

    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.camera_permission_required,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupUI()
        checkCameraPermission()
    }

    private fun setupUI() {
        binding.fab.setOnClickListener {
            scanningEnabled = !scanningEnabled
            if (scanningEnabled) {
                binding.scanStatus.text = getString(R.string.point_camera)
            } else {
                binding.scanStatus.text = getString(R.string.scanning)
            }
        }

        // Start FAB pulse animation
        val pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.fab_pulse)
        binding.fab.startAnimation(pulseAnim)
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        ErrorHandler.safe("QRScannerFragment", Unit, ErrorHandler.Messages.CAMERA_ERROR) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(binding.previewView.surfaceProvider)
                        }

                    // Image analysis for barcode scanning
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                cameraExecutor,
                                BarcodeScannerAnalyzer { barcodes ->
                                    processBarcodes(barcodes)
                                }
                            )
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    ErrorHandler.handleError("QRScannerFragment", e, ErrorHandler.Messages.CAMERA_ERROR)
                    Toast.makeText(requireContext(), R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        }
    }

    private fun processBarcodes(barcodes: List<Barcode>) {
        ErrorHandler.safe("QRScannerFragment", Unit, ErrorHandler.Messages.SCAN_ERROR) {
            if (!scanningEnabled || barcodes.isEmpty()) return@safe

            val barcode = barcodes.first()
            val rawValue = barcode.rawValue ?: return@safe

            scanningEnabled = false

            lifecycleScope.launch {
                try {
                    // Show success animation
                    val successAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.scan_success)
                    binding.fab.startAnimation(successAnim)

                    // Premium feedback
                    binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)

                    // Update status
                    binding.scanStatus.text = getString(R.string.scan_success)

                    // Determine scan type
                    val scanType = when (barcode.valueType) {
                        Barcode.TYPE_URL,
                        Barcode.TYPE_TEXT,
                        Barcode.TYPE_WIFI,
                        Barcode.TYPE_CONTACT_INFO -> ScanType.QR_CODE
                        else -> ScanType.BARCODE
                    }

                    // Save to database
                    viewModel.saveScan(
                        content = rawValue,
                        type = scanType,
                        format = barcode.format.toString()
                    )

                    Toast.makeText(requireContext(), "Scan saved!", Toast.LENGTH_SHORT).show()

                    // Re-enable scanning after delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        scanningEnabled = true
                        binding.scanStatus.text = getString(R.string.point_camera)
                    }, 2000)
                } catch (e: Exception) {
                    ErrorHandler.handleError("QRScannerFragment", e, "Scan processing failed")
                    scanningEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
