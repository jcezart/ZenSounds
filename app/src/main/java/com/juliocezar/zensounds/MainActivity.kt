package com.juliocezar.zensounds

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.juliocezar.zensounds.widgets.DailyWordWidgets
import com.juliocezar.zensounds.widgets.WidgetReadingRequest
import com.juliocezar.zensounds.services.PlaybackService
import com.juliocezar.zensounds.ui.MainScreen
import com.juliocezar.zensounds.ui.theme.ZenSoundsTheme

class MainActivity : ComponentActivity() {
    private var widgetReading by mutableStateOf<WidgetReadingRequest?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetReading = DailyWordWidgets.readIntent(intent)

        startService(Intent(this, PlaybackService::class.java))

        setContent {
            ZenSoundsTheme {
                MainScreen(widgetReading=widgetReading)
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetReading = DailyWordWidgets.readIntent(intent)
    }
}
