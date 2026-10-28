package com.lifereset.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifereset.wear.ui.WearApp
import com.lifereset.wear.ui.WearViewModel

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: WearViewModel = viewModel()
            WearApp(vm)
        }
    }
}
