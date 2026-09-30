package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppThemeMode
import com.example.ui.screens.GeneratorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScanResultSheet
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ScanQrProTheme
import com.example.ui.viewmodel.ScannerViewModel

enum class OmniScanTab {
    SCAN,
    HISTORY,
    GENERATE,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.ads.IronSourceAdManager.init(this)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> systemDark
            }

            ScanQrProTheme(darkTheme = isDark) {
                MainAppScreen(
                    viewModel = viewModel,
                    isDark = isDark,
                    onUserAction = {
                        com.example.ads.IronSourceAdManager.recordActionAndCheckInterstitial(this, threshold = 5)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.ads.IronSourceAdManager.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        com.example.ads.IronSourceAdManager.onPause(this)
    }
}

@Composable
fun MainAppScreen(
    viewModel: ScannerViewModel,
    isDark: Boolean,
    onUserAction: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(OmniScanTab.SCAN) }
    val activeScanResult by viewModel.activeScanResult.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val autoCopyEnabled by viewModel.preferences.autoCopy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast & snackbar feedback
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Turn off torch when leaving scanner tab
    LaunchedEffect(currentTab) {
        if (currentTab != OmniScanTab.SCAN && viewModel.isTorchOn.value) {
            viewModel.toggleTorch()
        }
    }

    // Handle back button: return to SCAN tab if on another tab
    BackHandler(enabled = currentTab != OmniScanTab.SCAN) {
        currentTab = OmniScanTab.SCAN
    }

    // Trigger interstitial action check every time a scan result is viewed
    LaunchedEffect(activeScanResult) {
        if (activeScanResult != null) {
            onUserAction()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Docked ironSource & Pangle Banner Ad directly above Bottom Navigation Bar
                if (currentTab != OmniScanTab.SCAN) {
                    com.example.ads.IronSourceBanner()
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == OmniScanTab.SCAN,
                        onClick = {
                            if (currentTab != OmniScanTab.SCAN) {
                                currentTab = OmniScanTab.SCAN
                                onUserAction()
                            }
                        },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.tab_scan)) },
                        label = { Text(stringResource(R.string.tab_scan)) },
                        modifier = Modifier.testTag("nav_tab_scan")
                    )
                    NavigationBarItem(
                        selected = currentTab == OmniScanTab.HISTORY,
                        onClick = {
                            if (currentTab != OmniScanTab.HISTORY) {
                                currentTab = OmniScanTab.HISTORY
                                onUserAction()
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.tab_history)) },
                        label = { Text(stringResource(R.string.tab_history)) },
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                    NavigationBarItem(
                        selected = currentTab == OmniScanTab.GENERATE,
                        onClick = {
                            if (currentTab != OmniScanTab.GENERATE) {
                                currentTab = OmniScanTab.GENERATE
                                onUserAction()
                            }
                        },
                        icon = { Icon(Icons.Default.QrCode, contentDescription = stringResource(R.string.tab_generate)) },
                        label = { Text(stringResource(R.string.tab_generate)) },
                        modifier = Modifier.testTag("nav_tab_generate")
                    )
                    NavigationBarItem(
                        selected = currentTab == OmniScanTab.SETTINGS,
                        onClick = {
                            if (currentTab != OmniScanTab.SETTINGS) {
                                currentTab = OmniScanTab.SETTINGS
                                onUserAction()
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.tab_settings)) },
                        label = { Text(stringResource(R.string.tab_settings)) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            OmniScanTab.SCAN -> {
                ScanScreen(
                    viewModel = viewModel,
                    isDark = isDark,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            OmniScanTab.HISTORY -> {
                HistoryScreen(
                    viewModel = viewModel,
                    isDark = isDark,
                    onSelectItem = { item ->
                        viewModel.showScanResult(item)
                        onUserAction()
                    },
                    onUserTouch = onUserAction,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            OmniScanTab.GENERATE -> {
                GeneratorScreen(
                    viewModel = viewModel,
                    isDark = isDark,
                    onUserTouch = onUserAction,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            OmniScanTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    isDark = isDark,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Clean landing page sheet for scan results
        activeScanResult?.let { item ->
            ScanResultSheet(
                item = item,
                isAutoCopied = autoCopyEnabled,
                onDismiss = { viewModel.clearActiveScanResult() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onDeleteItem = { viewModel.deleteScan(it) }
            )
        }
    }
}
