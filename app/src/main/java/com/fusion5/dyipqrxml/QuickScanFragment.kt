package com.fusion5.dyipqrxml

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.fusion5.dyipqrxml.databinding.FragmentQuickScanBinding
import com.fusion5.dyipqrxml.ui.quickscan.QuickScanViewModel
import com.fusion5.dyipqrxml.ui.quickscan.QuickScanViewModelFactory
import com.fusion5.dyipqrxml.util.QRCodeAnalyzer
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QuickScanFragment : Fragment() {

    private var _binding: FragmentQuickScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private val viewModel: QuickScanViewModel by viewModels {
        QuickScanViewModelFactory(
            ServiceLocator.provideTerminalRepository(requireContext()),
            ServiceLocator.provideScanHistoryRepository(requireContext()),
            ServiceLocator.provideSessionRepository(requireContext())
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            updateScanResult("Camera permission required to scan QR codes.")
            binding.buttonRescan.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuickScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.buttonRescan.setOnClickListener {
            resetScanning()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update UI based on state
                if (state.scanResult != null) {
                    updateScanResult("Scanned: ${state.scanResult}")
                }
                if (state.errorMessage != null) {
                    updateScanResult("Error: ${state.errorMessage}")
                }
                if (state.successMessage != null) {
                    updateScanResult(state.successMessage)
                }
            }
        }
    }

    private fun resetScanning() {
        binding.buttonRescan.visibility = View.GONE
        updateScanResult("Scanning for QR Code...")
        viewModel.startScanning()

        // Restart camera if it was stopped
        if (cameraProvider == null) {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(cameraProvider!!)
            } catch (exc: Exception) {
                Log.e("QuickScanFragment", "Use case binding failed", exc)
                updateScanResult("Error starting camera: ${exc.localizedMessage}")
                binding.buttonRescan.visibility = View.VISIBLE
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        // Unbind all use cases first
        cameraProvider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        // Configure image analyzer with optimal settings for QR scanning
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(1280, 720)) // Set optimal resolution
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
                    handleScannedQRCode(qrCode)
                })
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        try {
            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("QuickScanFragment", "Use case binding failed", exc)
            updateScanResult("Error starting camera: ${exc.localizedMessage}")
            binding.buttonRescan.visibility = View.VISIBLE
        }
    }

    private fun handleScannedQRCode(qrCode: String) {
        lifecycleScope.launch {
            viewModel.onScanResult(qrCode)
        }
    }

    private fun updateScanResult(message: String) {
        requireActivity().runOnUiThread {
            binding.textScanResult.text = message
            if (message != "Scanning for QR Code...") {
                binding.buttonRescan.visibility = View.VISIBLE
            }
        }
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED && cameraProvider == null
        ) {
            resetScanning()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}