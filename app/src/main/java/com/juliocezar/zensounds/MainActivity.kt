package com.juliocezar.zensounds

import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import android.view.WindowInsets
import com.juliocezar.zensounds.ui.theme.ZenSoundsTheme
import com.juliocezar.zensounds.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permite conteúdo sob as system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ZenSoundsTheme {
                MainScreen()
            }
        }

        window.decorView.post {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

