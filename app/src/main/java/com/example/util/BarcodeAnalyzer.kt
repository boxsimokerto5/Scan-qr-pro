package com.example.util

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val isScanningActive: () -> Boolean = { true },
    private val onBarcodeDetected: (Barcode) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var lastScanTime = 0L
    @Volatile
    private var isBusy = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        if (!isScanningActive() || isBusy || (currentTime - lastScanTime < 1500)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isBusy = true
        try {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (isScanningActive() && barcodes.isNotEmpty()) {
                        val firstValid = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                        if (firstValid != null) {
                            lastScanTime = System.currentTimeMillis()
                            onBarcodeDetected(firstValid)
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignore transient frame recognition failures
                }
                .addOnCompleteListener {
                    isBusy = false
                    imageProxy.close()
                }
        } catch (e: Exception) {
            isBusy = false
            imageProxy.close()
        }
    }
}
