package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.ui.LedgerApp
import com.example.ui.LedgerViewModel
import com.example.ui.LedgerViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = TransactionRepository(
        database.transactionDao(),
        database.subscriptionDao(),
        database.customCategoryDao(),
        database.budgetDao()
    )

    setContent {
      MyApplicationTheme {
        val viewModel: LedgerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
          factory = LedgerViewModelFactory(repository)
        )
        LedgerApp(viewModel = viewModel)
      }
    }
  }
}

