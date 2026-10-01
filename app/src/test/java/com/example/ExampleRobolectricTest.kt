package com.example

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ScanItemEntity
import com.example.ui.viewmodel.ScannerViewModel
import com.example.util.BarcodeUtils
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Scan Qr Pro", appName)
  }

  @Test
  fun `copyToClipboard updates system clipboard immediately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testContent = "https://example.com/qr-test-code"

    val result = BarcodeUtils.copyToClipboard(context, testContent, showToast = false)
    assertTrue(result)

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    assertNotNull(clipboard.primaryClip)
    assertEquals(1, clipboard.primaryClip?.itemCount)
    assertEquals(testContent, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
  }

  @Test
  fun `auto copy feature automatically copies scanned content to clipboard`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ScannerViewModel(application)

    // Ensure auto-copy is enabled
    viewModel.setAutoCopy(true)
    assertTrue(viewModel.preferences.autoCopy.value)

    val sampleScan = ScanItemEntity(
      rawValue = "WIFI:S:MyWifi;T:WPA;P:SecretPass123;;",
      displayValue = "WIFI:S:MyWifi",
      format = Barcode.FORMAT_QR_CODE,
      formatName = "QR Code",
      valueType = Barcode.TYPE_WIFI,
      valueTypeName = "Jaringan Wi-Fi"
    )

    viewModel.processSimulatedScan(sampleScan, application)

    val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    assertEquals(sampleScan.rawValue, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    assertTrue(viewModel.wasAutoCopied.value)
    assertEquals(sampleScan.rawValue, viewModel.activeScanResult.value?.rawValue)
  }
}
