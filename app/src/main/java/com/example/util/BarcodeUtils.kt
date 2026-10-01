package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Patterns
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object BarcodeUtils {

    fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_PDF417 -> "PDF-417"
            Barcode.FORMAT_ITF -> "ITF"
            else -> "Barcode"
        }
    }

    fun getValueTypeName(valueType: Int, rawValue: String): String {
        if (isLikelyUrl(rawValue)) return "Tautan Web / URL"
        return when (valueType) {
            Barcode.TYPE_URL -> "Tautan Web / URL"
            Barcode.TYPE_WIFI -> "Jaringan Wi-Fi"
            Barcode.TYPE_PHONE -> "Nomor Telepon"
            Barcode.TYPE_SMS -> "Pesan SMS"
            Barcode.TYPE_EMAIL -> "Alamat Email"
            Barcode.TYPE_CONTACT_INFO -> "Kontak (vCard)"
            Barcode.TYPE_PRODUCT -> "Produk Komersial"
            Barcode.TYPE_GEO -> "Koordinat Lokasi"
            Barcode.TYPE_CALENDAR_EVENT -> "Acara Kalender"
            else -> "Teks Biasa"
        }
    }

    fun isLikelyUrl(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return true
        }
        return Patterns.WEB_URL.matcher(trimmed).matches()
    }

    fun ensureUrlScheme(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun openInBrowser(context: Context, url: String) {
        val finalUrl = ensureUrlScheme(url)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka browser", Toast.LENGTH_SHORT).show()
        }
    }

    fun searchWeb(context: Context, query: String) {
        try {
            val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka pencarian", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, message: String, targetPhone: String? = null) {
        try {
            val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            val url = if (!targetPhone.isNullOrBlank()) {
                val cleanPhone = targetPhone.replace("+", "").replace("-", "").replace(" ", "").trim()
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (fallbackEx: Exception) {
                Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyToClipboard(
        context: Context,
        text: String,
        showToast: Boolean = true,
        customMessage: String? = null
    ): Boolean {
        return try {
            val appContext = context.applicationContext ?: context
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            val clip = ClipData.newPlainText("Scan Qr Pro", text)

            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                clipboard.setPrimaryClip(clip)
                if (showToast) {
                    val msg = customMessage ?: "Teks disalin ke clipboard"
                    Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        clipboard.setPrimaryClip(clip)
                        if (showToast) {
                            val msg = customMessage ?: "Teks disalin ke clipboard"
                            Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {}
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareText(context: Context, text: String, title: String = "Bagikan Hasil Scan") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_TITLE, title)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan teks", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(70)
                }
            }
        } catch (_: Exception) {
            // Ignore vibration errors if not permitted on device
        }
    }

    /**
     * Generates a QR Code Bitmap with optional central logo.
     * Uses ErrorCorrectionLevel.H (recovers up to 30% missing or covered data) so the QR code
     * remains 100% easily readable by all scanners despite the center logo.
     */
    fun generateQrCodeBitmap(
        text: String,
        size: Int = 600,
        logoBitmap: Bitmap? = null
    ): Bitmap? {
        if (text.isBlank()) return null
        return try {
            val hints = mutableMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                // Use ErrorCorrectionLevel.H for resilient error correction up to 30%
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }

            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
                }
            }
            val qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            qrBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            if (logoBitmap == null) {
                return qrBitmap
            }

            // Overlay the center logo with a crisp white protective backing
            val combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combinedBitmap)
            canvas.drawBitmap(qrBitmap, 0f, 0f, null)

            // Logo size: 21% of QR code dimension (well within the 30% ErrorCorrectionLevel.H capacity)
            val logoSize = (width * 0.21f).toInt()
            val logoX = (width - logoSize) / 2f
            val logoY = (height - logoSize) / 2f
            val badgePadding = (logoSize * 0.16f)

            // Draw white rounded plate behind logo so QR black modules don't touch the logo
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val badgeRect = RectF(
                logoX - badgePadding,
                logoY - badgePadding,
                logoX + logoSize + badgePadding,
                logoY + logoSize + badgePadding
            )
            val cornerRadius = badgePadding * 1.6f
            canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, bgPaint)
            canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, borderPaint)

            // Make the logo rounded for modern aesthetics and draw inside the badge
            val roundedLogo = getRoundedCornerBitmap(logoBitmap, logoSize, logoSize, cornerRadius * 0.8f)
            canvas.drawBitmap(roundedLogo, logoX, logoY, null)

            combinedBitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun getRoundedCornerBitmap(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        cornerRadius: Float
    ): Bitmap {
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, width, height)
        val rectF = RectF(rect)

        paint.color = AndroidColor.BLACK
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, rect, rect, paint)
        return output
    }

    /**
     * Creates crisp vector-style preset badge bitmaps (WhatsApp, Web, Wi-Fi, Star)
     * so users can test center logos instantly without needing to pick a gallery file.
     */
    fun createPresetLogoBitmap(presetName: String): Bitmap? {
        val size = 140
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        when (presetName) {
            "WHATSAPP" -> {
                paint.color = android.graphics.Color.parseColor("#25D366")
                canvas.drawRoundRect(rect, 32f, 32f, paint)
                paint.color = AndroidColor.WHITE
                paint.textSize = 68f
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                val yPos = (size / 2f - (paint.descent() + paint.ascent()) / 2f)
                canvas.drawText("WA", size / 2f, yPos, paint)
            }
            "WEB" -> {
                paint.color = android.graphics.Color.parseColor("#0284C7")
                canvas.drawRoundRect(rect, 32f, 32f, paint)
                paint.color = AndroidColor.WHITE
                paint.textSize = 52f
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                val yPos = (size / 2f - (paint.descent() + paint.ascent()) / 2f)
                canvas.drawText("WWW", size / 2f, yPos, paint)
            }
            "WIFI" -> {
                paint.color = android.graphics.Color.parseColor("#8B5CF6")
                canvas.drawRoundRect(rect, 32f, 32f, paint)
                paint.color = AndroidColor.WHITE
                paint.textSize = 46f
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                val yPos = (size / 2f - (paint.descent() + paint.ascent()) / 2f)
                canvas.drawText("Wi-Fi", size / 2f, yPos, paint)
            }
            "STAR" -> {
                paint.color = android.graphics.Color.parseColor("#F59E0B")
                canvas.drawRoundRect(rect, 32f, 32f, paint)
                paint.color = AndroidColor.WHITE
                paint.textSize = 76f
                paint.textAlign = Paint.Align.CENTER
                val yPos = (size / 2f - (paint.descent() + paint.ascent()) / 2f)
                canvas.drawText("★", size / 2f, yPos, paint)
            }
            else -> return null
        }
        return bitmap
    }

    /**
     * Loads, center-crops to square, and scales down user-selected image for optimal logo embedding.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxSize: Int = 300): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val dimension = minOf(original.width, original.height)
            val cropped = Bitmap.createBitmap(
                original,
                (original.width - dimension) / 2,
                (original.height - dimension) / 2,
                dimension,
                dimension
            )
            Bitmap.createScaledBitmap(cropped, maxSize, maxSize, true)
        } catch (e: Exception) {
            null
        }
    }

    fun saveQrCodeToGallery(context: Context, bitmap: Bitmap): Boolean {
        val filename = "ScanQrPro_${System.currentTimeMillis()}.png"
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScanQrPro")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareQrCodeBitmap(context: Context, bitmap: Bitmap, text: String) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "qr_${System.currentTimeMillis()}.png")
            FileOutputStream(imageFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            if (contentUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Bagikan Gambar QR Code").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            shareText(context, text, "Bagikan QR Code")
        }
    }
}
