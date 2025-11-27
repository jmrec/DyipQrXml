package com.fusion5.dyipqrxml.util

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QRCodeAnalyzer(private val onQRCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        try {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue?.let { qrCode ->
                        onQRCodeScanned(qrCode)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QRCodeAnalyzer", "Barcode scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e("QRCodeAnalyzer", "Error processing image", e)
            imageProxy.close()
        }
    }
}