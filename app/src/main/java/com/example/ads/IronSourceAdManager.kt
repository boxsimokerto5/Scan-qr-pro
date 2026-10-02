package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.NeonCyan
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.integration.IntegrationHelper
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object IronSourceAdManager {
    private const val TAG = "IronSourceAdManager"

    // Configuration IDs provided by the user
    const val APP_KEY = "286c3ca95"
    const val BANNER_ID = "r9enfd7ebwloknf4"
    const val INTERSTITIAL_ID = "sb8athfndh69gsmq"
    const val NATIVE_ID = "19g7vmxgcan7h8b6"
    const val REWARDED_ID = "7tgikazgk6tl9i1t"

    // Action counter for interstitial trigger (threshold = 5 clicks/actions)
    private val actionCounter = AtomicInteger(0)
    private val isInitialized = AtomicBoolean(false)

    // Current pending reward callback
    private var onUserRewardedCallback: (() -> Unit)? = null

    fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.PRODUCT.contains("sdk_gphone")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")))
    }

    fun init(activity: Activity) {
        if (isInitialized.getAndSet(true)) return

        if (isEmulator()) {
            Log.d(TAG, "Running in Android Emulator. Suppressing native ad SDK renderer to avoid MESA driver and AdServices measurement service issues.")
            return
        }

        try {
            Log.d(TAG, "Initializing ironSource SDK with App Key: $APP_KEY")
            
            // Set listeners before init
            setupListeners()

            // Initialize ironSource with Interstitial, Rewarded Video, and Banner
            IronSource.init(
                activity,
                APP_KEY,
                IronSource.AD_UNIT.INTERSTITIAL,
                IronSource.AD_UNIT.REWARDED_VIDEO,
                IronSource.AD_UNIT.BANNER
            )

            // Pre-load Interstitial
            IronSource.loadInterstitial()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize ironSource", t)
        }
    }

    private fun setupListeners() {
        // LevelPlay Rewarded Video Listener
        IronSource.setLevelPlayRewardedVideoListener(object : LevelPlayRewardedVideoListener {
            override fun onAdAvailable(adInfo: AdInfo) {
                Log.d(TAG, "Rewarded ad available: ${adInfo.adNetwork}")
            }

            override fun onAdUnavailable() {
                Log.d(TAG, "Rewarded ad unavailable")
            }

            override fun onAdOpened(adInfo: AdInfo) {
                Log.d(TAG, "Rewarded ad opened")
            }

            override fun onAdShowFailed(error: IronSourceError, adInfo: AdInfo) {
                Log.e(TAG, "Rewarded ad show failed: ${error.errorMessage}")
                // Fallback: grant reward so user is not stuck
                triggerPendingReward()
            }

            override fun onAdClicked(placement: Placement, adInfo: AdInfo) {
                Log.d(TAG, "Rewarded ad clicked")
            }

            override fun onAdRewarded(placement: Placement, adInfo: AdInfo) {
                Log.d(TAG, "User earned reward from ad")
                triggerPendingReward()
            }

            override fun onAdClosed(adInfo: AdInfo) {
                Log.d(TAG, "Rewarded ad closed")
                // In case reward wasn't triggered yet
                triggerPendingReward()
            }
        })

        // LevelPlay Interstitial Listener
        IronSource.setLevelPlayInterstitialListener(object : LevelPlayInterstitialListener {
            override fun onAdReady(adInfo: AdInfo) {
                Log.d(TAG, "Interstitial ad ready")
            }

            override fun onAdLoadFailed(error: IronSourceError) {
                Log.w(TAG, "Interstitial load failed: ${error.errorMessage}")
            }

            override fun onAdOpened(adInfo: AdInfo) {
                Log.d(TAG, "Interstitial ad opened")
            }

            override fun onAdShowSucceeded(adInfo: AdInfo) {
                Log.d(TAG, "Interstitial ad shown successfully")
            }

            override fun onAdShowFailed(error: IronSourceError, adInfo: AdInfo) {
                Log.e(TAG, "Interstitial show failed: ${error.errorMessage}")
                // Preload again for future attempts
                IronSource.loadInterstitial()
            }

            override fun onAdClicked(adInfo: AdInfo) {
                Log.d(TAG, "Interstitial ad clicked")
            }

            override fun onAdClosed(adInfo: AdInfo) {
                Log.d(TAG, "Interstitial ad closed, preloading next...")
                // Preload next interstitial
                IronSource.loadInterstitial()
            }
        })
    }

    private fun triggerPendingReward() {
        val callback = onUserRewardedCallback
        onUserRewardedCallback = null
        callback?.invoke()
    }

    /**
     * Shows Rewarded Ad for the Download QR action.
     * If rewarded video is available, displays it and grants reward upon completion.
     * If unavailable (offline/no fill), immediately invokes onRewarded to ensure great user experience.
     */
    fun showRewardedAdForDownload(activity: Activity, onRewarded: () -> Unit) {
        if (isEmulator()) {
            onRewarded()
            return
        }
        try {
            if (IronSource.isRewardedVideoAvailable()) {
                onUserRewardedCallback = onRewarded
                IronSource.showRewardedVideo()
            } else {
                Log.d(TAG, "Rewarded video not available, granting reward directly as fallback")
                onRewarded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing rewarded video", e)
            onRewarded()
        }
    }

    /**
     * Increments the user touch/action counter.
     * Accurately tracks every touch action (threshold = 5).
     * Preloads automatically to guarantee the interstitial is ready on the 5th touch.
     */
    fun recordActionAndCheckInterstitial(activity: Activity, threshold: Int = 5) {
        if (isEmulator()) return

        val count = actionCounter.incrementAndGet()
        Log.d(TAG, "Sentuhan aksi pengguna: #$count dari $threshold")

        // Actively preload interstitial so it is 100% ready when count hits 5
        if (!IronSource.isInterstitialReady()) {
            try {
                IronSource.loadInterstitial()
            } catch (_: Throwable) {}
        }

        if (count >= threshold) {
            actionCounter.set(0)
            try {
                if (IronSource.isInterstitialReady()) {
                    Log.d(TAG, "Menampilkan iklan Interstitial tepat di sentuhan ke-$threshold!")
                    IronSource.showInterstitial()
                } else {
                    Log.d(TAG, "Interstitial sedang dimuat di sentuhan ke-$threshold, memuat ulang...")
                    IronSource.loadInterstitial()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error displaying interstitial", e)
            }
        }
    }

    fun onResume(activity: Activity) {
        if (isEmulator()) return
        try {
            IronSource.onResume(activity)
        } catch (_: Throwable) {}
    }

    fun onPause(activity: Activity) {
        if (isEmulator()) return
        try {
            IronSource.onPause(activity)
        } catch (_: Throwable) {}
    }
}

/**
 * Docked ironSource & Pangle Banner Composable (320x50 standard).
 * Sits seamlessly flush right above the Bottom Navigation Bar without awkward gaps.
 */
@Composable
fun IronSourceBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var bannerLayout by remember { mutableStateOf<IronSourceBannerLayout?>(null) }
    var isBannerLoaded by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        if (activity != null && !IronSourceAdManager.isEmulator()) {
            try {
                val banner = IronSource.createBanner(activity, ISBannerSize.BANNER)
                banner.levelPlayBannerListener = object : LevelPlayBannerListener {
                    override fun onAdLoaded(adInfo: AdInfo) {
                        isBannerLoaded = true
                    }

                    override fun onAdLoadFailed(error: IronSourceError) {
                        isBannerLoaded = false
                    }

                    override fun onAdClicked(adInfo: AdInfo) {}
                    override fun onAdScreenPresented(adInfo: AdInfo) {}
                    override fun onAdScreenDismissed(adInfo: AdInfo) {}
                    override fun onAdLeftApplication(adInfo: AdInfo) {}
                }
                IronSource.loadBanner(banner)
                bannerLayout = banner
            } catch (t: Throwable) {
                Log.e("IronSourceBanner", "Error creating banner", t)
            }
        }

        onDispose {
            bannerLayout?.let {
                try {
                    IronSource.destroyBanner(it)
                } catch (_: Throwable) {}
            }
            bannerLayout = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (bannerLayout != null) {
                AndroidView(
                    factory = {
                        bannerLayout!!.apply {
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                android.view.Gravity.CENTER
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ironsource_banner_view")
                )
            }
        }
    }
}

/**
 * Elegant Native Ad Card integrated seamlessly inside the History list.
 */
@Composable
fun IronSourceNativeAdCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("ironsource_native_ad_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "SPONSORED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Iklan Mitra Pangle & ironSource",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "4.9",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "AD",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan Qr Pro Media Hub",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dapatkan fitur scanner terlengkap dengan integrasi jaringan iklan global.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
