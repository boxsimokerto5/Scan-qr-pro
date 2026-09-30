package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ScanItemEntity
import com.example.ui.components.ThemeToggleButton
import com.example.ui.viewmodel.ScannerViewModel
import com.example.util.BarcodeUtils
import com.google.mlkit.vision.barcode.common.Barcode

enum class GeneratorType {
    TEXT,
    URL,
    WHATSAPP,
    WIFI
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorScreen(
    viewModel: ScannerViewModel,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(GeneratorType.TEXT) }
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("https://") }
    var waPhone by remember { mutableStateOf("") }
    var waMessage by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var isDownloaded by remember { mutableStateOf(false) }

    // Center Logo state
    var customLogoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPresetLogo by remember { mutableStateOf<String?>(null) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val bitmap = BarcodeUtils.loadBitmapFromUri(context, it)
            if (bitmap != null) {
                customLogoBitmap = bitmap
                selectedPresetLogo = null
                Toast.makeText(context, "Logo berhasil dimuat!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memuat gambar logo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val effectiveLogoBitmap: Bitmap? = remember(customLogoBitmap, selectedPresetLogo) {
        customLogoBitmap ?: selectedPresetLogo?.let { BarcodeUtils.createPresetLogoBitmap(it) }
    }

    val qrContent = remember(selectedType, textInput, urlInput, waPhone, waMessage, wifiSsid, wifiPassword) {
        isDownloaded = false
        when (selectedType) {
            GeneratorType.TEXT -> textInput
            GeneratorType.URL -> urlInput
            GeneratorType.WHATSAPP -> {
                val cleanPhone = waPhone.replace("+", "").replace("-", "").replace(" ", "").trim()
                if (cleanPhone.isNotBlank()) {
                    if (waMessage.isNotBlank()) "https://wa.me/$cleanPhone?text=${android.net.Uri.encode(waMessage)}"
                    else "https://wa.me/$cleanPhone"
                } else waMessage
            }
            GeneratorType.WIFI -> {
                if (wifiSsid.isNotBlank()) "WIFI:S:$wifiSsid;T:WPA;P:$wifiPassword;;" else ""
            }
        }
    }

    val generatedBitmap: Bitmap? = remember(qrContent, effectiveLogoBitmap) {
        if (qrContent.isNotBlank()) {
            BarcodeUtils.generateQrCodeBitmap(qrContent, size = 650, logoBitmap = effectiveLogoBitmap)
        } else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Buat Barcode & QR",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generate QR code kustom, sematkan logo, unduh, dan bagikan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ThemeToggleButton(
                isDark = isDark,
                onToggle = { viewModel.toggleTheme(isDark) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Type selection chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == GeneratorType.TEXT,
                onClick = { selectedType = GeneratorType.TEXT },
                label = { Text("Teks") },
                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = selectedType == GeneratorType.URL,
                onClick = { selectedType = GeneratorType.URL },
                label = { Text("Tautan URL") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = selectedType == GeneratorType.WHATSAPP,
                onClick = { selectedType = GeneratorType.WHATSAPP },
                label = { Text("WhatsApp Link") },
                leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = selectedType == GeneratorType.WIFI,
                onClick = { selectedType = GeneratorType.WIFI },
                label = { Text("Wi-Fi") },
                leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Fields based on Type
        when (selectedType) {
            GeneratorType.TEXT -> {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Isi Teks") },
                    placeholder = { Text("Masukkan teks sembarang...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gen_text_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }
            GeneratorType.URL -> {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("URL Website") },
                    placeholder = { Text("https://contoh.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gen_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            GeneratorType.WHATSAPP -> {
                OutlinedTextField(
                    value = waPhone,
                    onValueChange = { waPhone = it },
                    label = { Text("Nomor WhatsApp (dengan kode negara)") },
                    placeholder = { Text("Contoh: 628123456789") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gen_wa_phone"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = waMessage,
                    onValueChange = { waMessage = it },
                    label = { Text("Pesan Default (Opsional)") },
                    placeholder = { Text("Halo, saya ingin bertanya...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gen_wa_msg"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
            GeneratorType.WIFI -> {
                OutlinedTextField(
                    value = wifiSsid,
                    onValueChange = { wifiSsid = it },
                    label = { Text("Nama Jaringan Wi-Fi (SSID)") },
                    placeholder = { Text("Nama Wi-Fi Anda") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = wifiPassword,
                    onValueChange = { wifiPassword = it },
                    label = { Text("Password Wi-Fi") },
                    placeholder = { Text("Kata sandi Wi-Fi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // LOGO DI TENGAH QR SECTION
        Text(
            text = "LOGO DI TENGAH QR (OPSIONAL)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Upload button & Active Logo preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            logoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_logo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (customLogoBitmap != null) "Ganti Gambar Logo" else "Unggah Logo dari Galeri",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (effectiveLogoBitmap != null) {
                        Spacer(modifier = Modifier.width(10.dp))

                        // Active Logo Badge Thumbnail
                        Box(contentAlignment = Alignment.TopEnd) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    bitmap = effectiveLogoBitmap.asImageBitmap(),
                                    contentDescription = "Logo Aktif",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }

                            // Delete / Remove logo button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        customLogoBitmap = null
                                        selectedPresetLogo = null
                                        Toast.makeText(context, "Logo dihapus", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus Logo",
                                    tint = Color.White,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Logo Presets
                Text(
                    text = "Atau pilih logo preset instan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPresetLogo == "WHATSAPP" && customLogoBitmap == null,
                        onClick = {
                            customLogoBitmap = null
                            selectedPresetLogo = if (selectedPresetLogo == "WHATSAPP") null else "WHATSAPP"
                        },
                        label = { Text("WA", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = selectedPresetLogo == "WEB" && customLogoBitmap == null,
                        onClick = {
                            customLogoBitmap = null
                            selectedPresetLogo = if (selectedPresetLogo == "WEB") null else "WEB"
                        },
                        label = { Text("Website", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = selectedPresetLogo == "WIFI" && customLogoBitmap == null,
                        onClick = {
                            customLogoBitmap = null
                            selectedPresetLogo = if (selectedPresetLogo == "WIFI") null else "WIFI"
                        },
                        label = { Text("Wi-Fi", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = selectedPresetLogo == "STAR" && customLogoBitmap == null,
                        onClick = {
                            customLogoBitmap = null
                            selectedPresetLogo = if (selectedPresetLogo == "STAR") null else "STAR"
                        },
                        label = { Text("Bintang", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)) }
                    )
                    if (effectiveLogoBitmap != null) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                customLogoBitmap = null
                                selectedPresetLogo = null
                            },
                            label = { Text("Tanpa Logo", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Info note about error correction
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kode QR menggunakan Error Correction Level H (30%), sehingga tetap terbaca 100% normal dan cepat oleh scanner meskipun memiliki logo di tengah.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Generated QR Code Preview Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gen_preview_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (generatedBitmap != null) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(240.dp)
                            .padding(6.dp)
                    ) {
                        Image(
                            bitmap = generatedBitmap.asImageBitmap(),
                            contentDescription = "Hasil QR Code dengan Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }

                    if (effectiveLogoBitmap != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ QR Code dilengkapi Logo di Tengah",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Prominent Download QR Button
                    Button(
                        onClick = {
                            val success = BarcodeUtils.saveQrCodeToGallery(context, generatedBitmap)
                            if (success) {
                                isDownloaded = true
                                Toast.makeText(context, context.getString(R.string.download_success), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.download_failed), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("download_qr_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDownloaded) "Tersimpan di Galeri (Pictures/ScanQrPro)" else "Unduh QR Code ke Galeri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary action buttons: Share Image & Copy Text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                BarcodeUtils.shareQrCodeBitmap(context, generatedBitmap, qrContent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_qr_image_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan Gambar", fontSize = 13.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                BarcodeUtils.copyToClipboard(context, qrContent, showToast = true)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("copy_gen_text_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Teks", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Save to history button
                    OutlinedButton(
                        onClick = {
                            val scanItem = ScanItemEntity(
                                rawValue = qrContent,
                                displayValue = qrContent,
                                format = Barcode.FORMAT_QR_CODE,
                                formatName = if (effectiveLogoBitmap != null) "QR Code + Logo" else "QR Code (Dibuat)",
                                valueType = when (selectedType) {
                                    GeneratorType.URL -> Barcode.TYPE_URL
                                    GeneratorType.WIFI -> Barcode.TYPE_WIFI
                                    else -> Barcode.TYPE_TEXT
                                },
                                valueTypeName = when (selectedType) {
                                    GeneratorType.URL -> "Tautan Web / URL"
                                    GeneratorType.WHATSAPP -> "WhatsApp Chat"
                                    GeneratorType.WIFI -> "Jaringan Wi-Fi"
                                    else -> "Teks Biasa"
                                }
                            )
                            viewModel.showScanResult(scanItem)
                            Toast.makeText(context, "QR Code dibuka di pratinjau", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka di Halaman Hasil & Riwayat", fontSize = 13.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ketik teks di atas untuk generate QR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
