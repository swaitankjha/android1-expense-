package com.example.expenceflow.ui.transaction

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.expenceflow.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_screen", "transaction")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }
}
