package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.LedgerApp
import com.example.ui.LedgerViewModel
import com.example.ui.theme.RemixLedgerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Safe Area edge-to-edge draw-behind backgrounds
        enableEdgeToEdge()

        setContent {
            RemixLedgerTheme {
                LedgerApp(viewModel = viewModel)
            }
        }
    }
}
