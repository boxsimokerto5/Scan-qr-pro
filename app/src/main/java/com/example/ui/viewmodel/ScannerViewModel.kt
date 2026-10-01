package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ScanItemEntity
import com.example.data.preferences.ScanPreferences
import com.example.data.repository.ScanRepository
import com.example.util.BarcodeUtils
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScanFilterCategory {
    ALL,
    URL,
    TEXT,
    PRODUCT,
    FAVORITES
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanRepository
    val preferences: ScanPreferences = ScanPreferences(application)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ScanRepository(database.scanDao())
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ScanFilterCategory.ALL)
    val selectedFilter: StateFlow<ScanFilterCategory> = _selectedFilter.asStateFlow()

    val historyItems: StateFlow<List<ScanItemEntity>> = combine(
        repository.allScans,
        _searchQuery,
        _selectedFilter
    ) { scans, query, filter ->
        scans.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.rawValue.contains(query, ignoreCase = true) ||
                item.displayValue.contains(query, ignoreCase = true) ||
                item.formatName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                ScanFilterCategory.ALL -> true
                ScanFilterCategory.URL -> item.valueType == Barcode.TYPE_URL || BarcodeUtils.isLikelyUrl(item.rawValue)
                ScanFilterCategory.TEXT -> item.valueType == Barcode.TYPE_TEXT && !BarcodeUtils.isLikelyUrl(item.rawValue)
                ScanFilterCategory.PRODUCT -> item.valueType == Barcode.TYPE_PRODUCT || item.formatName.contains("EAN") || item.formatName.contains("UPC")
                ScanFilterCategory.FAVORITES -> item.isFavorite
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeScanResult = MutableStateFlow<ScanItemEntity?>(null)
    val activeScanResult: StateFlow<ScanItemEntity?> = _activeScanResult.asStateFlow()

    private val _wasAutoCopied = MutableStateFlow(false)
    val wasAutoCopied: StateFlow<Boolean> = _wasAutoCopied.asStateFlow()

    @Volatile
    private var isHandlingScan = false

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isProcessingImage = MutableStateFlow(false)
    val isProcessingImage: StateFlow<Boolean> = _isProcessingImage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Generator state
    private val _generatorInput = MutableStateFlow("")
    val generatorInput: StateFlow<String> = _generatorInput.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterCategory(category: ScanFilterCategory) {
        _selectedFilter.value = category
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun setAutoCopy(enabled: Boolean) {
        preferences.setAutoCopy(enabled)
    }

    fun setVibration(enabled: Boolean) {
        preferences.setVibration(enabled)
    }

    fun setBeepSound(enabled: Boolean) {
        preferences.setBeepSound(enabled)
    }

    val themeMode = preferences.themeMode

    fun setThemeMode(mode: com.example.data.preferences.AppThemeMode) {
        preferences.setThemeMode(mode)
    }

    fun toggleTheme(isCurrentlyDark: Boolean) {
        preferences.toggleLightDark(isCurrentlyDark)
    }

    fun clearActiveScanResult() {
        _activeScanResult.value = null
        _wasAutoCopied.value = false
        isHandlingScan = false
    }

    fun showScanResult(item: ScanItemEntity) {
        _wasAutoCopied.value = false
        _activeScanResult.value = item
    }

    fun onBarcodeDetected(barcode: Barcode, context: Context) {
        val rawValue = barcode.rawValue ?: return
        if (rawValue.isBlank()) return
        if (_activeScanResult.value != null || isHandlingScan) return
        isHandlingScan = true

        val displayValue = barcode.displayValue ?: rawValue
        val format = barcode.format
        val formatName = BarcodeUtils.getFormatName(format)
        val valueType = barcode.valueType
        val valueTypeName = BarcodeUtils.getValueTypeName(valueType, rawValue)

        val newScan = ScanItemEntity(
            rawValue = rawValue,
            displayValue = displayValue,
            format = format,
            formatName = formatName,
            valueType = valueType,
            valueTypeName = valueTypeName
        )

        // 1. Immediately copy to system clipboard if auto-copy is enabled
        val isAuto = preferences.autoCopy.value
        _wasAutoCopied.value = isAuto
        if (isAuto) {
            BarcodeUtils.copyToClipboard(
                context = context,
                text = rawValue,
                showToast = true,
                customMessage = "Konten QR berhasil disalin otomatis ke papan klip!"
            )
        }

        // 2. Immediately trigger haptic feedback
        if (preferences.vibration.value) {
            BarcodeUtils.triggerVibration(context)
        }

        // 3. Immediately display scan result sheet to user
        _activeScanResult.value = newScan

        // 4. Asynchronously persist to Room local database
        viewModelScope.launch {
            try {
                val id = repository.insertScan(newScan)
                if (_activeScanResult.value == newScan) {
                    _activeScanResult.value = newScan.copy(id = id)
                }
            } finally {
                isHandlingScan = false
            }
        }
    }

    fun processSimulatedScan(sampleScan: ScanItemEntity, context: Context) {
        if (_activeScanResult.value != null || isHandlingScan) return
        isHandlingScan = true

        val isAuto = preferences.autoCopy.value
        _wasAutoCopied.value = isAuto
        if (isAuto) {
            BarcodeUtils.copyToClipboard(
                context = context,
                text = sampleScan.rawValue,
                showToast = true,
                customMessage = "Konten QR berhasil disalin otomatis ke papan klip!"
            )
        }

        if (preferences.vibration.value) {
            BarcodeUtils.triggerVibration(context)
        }

        _activeScanResult.value = sampleScan

        viewModelScope.launch {
            try {
                val id = repository.insertScan(sampleScan)
                if (_activeScanResult.value == sampleScan) {
                    _activeScanResult.value = sampleScan.copy(id = id)
                }
            } finally {
                isHandlingScan = false
            }
        }
    }

    fun scanImageUri(context: Context, uri: Uri) {
        _isProcessingImage.value = true
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    _isProcessingImage.value = false
                    val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (first != null) {
                        onBarcodeDetected(first, context)
                    } else {
                        _toastMessage.value = "Tidak ditemukan barcode pada gambar tersebut"
                    }
                }
                .addOnFailureListener {
                    _isProcessingImage.value = false
                    _toastMessage.value = "Gagal memindai gambar"
                }
        } catch (e: Exception) {
            _isProcessingImage.value = false
            _toastMessage.value = "Terjadi kesalahan saat memproses gambar"
        }
    }

    fun toggleFavorite(item: ScanItemEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, item.isFavorite)
        }
    }

    fun deleteScan(item: ScanItemEntity) {
        viewModelScope.launch {
            repository.deleteScan(item)
            if (_activeScanResult.value?.id == item.id) {
                _activeScanResult.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun setGeneratorInput(text: String) {
        _generatorInput.value = text
        if (text.isNotBlank()) {
            _generatedBitmap.value = BarcodeUtils.generateQrCodeBitmap(text)
        } else {
            _generatedBitmap.value = null
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
