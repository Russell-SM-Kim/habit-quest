package com.lifereset.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lifereset.app.ui.LifeResetApp
import com.lifereset.core.designsystem.theme.LifeResetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LifeResetApplication
        setContent {
            LifeResetTheme {
                LifeResetApp(container = app.dataContainer, sounds = app.sounds)
            }
        }
    }
}
