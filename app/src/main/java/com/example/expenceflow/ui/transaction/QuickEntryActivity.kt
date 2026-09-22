package com.example.expenceflow.ui.transaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.expenceflow.data.auto.VoiceEntrySheet
import com.example.expenceflow.ui.theme.ExpenceFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenceFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VoiceEntrySheet(
                            onDismiss = { finish() }
                        )
                    }
                }
            }
        }
    }
}
