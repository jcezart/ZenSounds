package com.juliocezar.zensounds

import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import android.view.WindowInsets
import com.google.android.gms.ads.MobileAds
import com.juliocezar.zensounds.ui.theme.ZenSoundsTheme
import com.juliocezar.zensounds.ui.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ZenSoundsTheme {
                MainScreen()
            }
        }

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            ConsentRequestParameters.Builder().build(),
            {
                if (consentInformation.isConsentFormAvailable) {
                    loadForm(consentInformation)
                } else {
                    initializeAds()
                }
            },
            { error: FormError? ->
                initializeAds()
            }
        )

        window.decorView.post {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun loadForm(consentInformation: ConsentInformation) {
        UserMessagingPlatform.loadConsentForm(
            this,
            { consentForm ->
                consentForm.show(this) { error: FormError? ->
                    initializeAds()
                }
            },
            { error: FormError? ->
                initializeAds()
            }
        )
    }

    private fun initializeAds() {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MainActivity) {}
        }
    }
}