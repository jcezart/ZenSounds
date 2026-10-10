package com.juliocezar.zensounds

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.juliocezar.zensounds.services.PlaybackService
import com.juliocezar.zensounds.ui.MainScreen
import com.juliocezar.zensounds.ui.theme.ZenSoundsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(Intent(this, PlaybackService::class.java))

        setContent {
            ZenSoundsTheme {
                MainScreen()
            }
        }
    }
}