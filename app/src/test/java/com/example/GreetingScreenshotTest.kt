package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.ui.LedgerApp
import com.example.ui.LedgerViewModel
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun test_navigation() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repository = TransactionRepository(db.transactionDao(), db.subscriptionDao(), db.customCategoryDao(), db.budgetDao())
    val viewModel = LedgerViewModel(repository)
    viewModel.loadSampleData()

    composeTestRule.setContent { 
      MyApplicationTheme { 
        LedgerApp(viewModel = viewModel) 
      } 
    }

    // Try navigating to sub tab
    composeTestRule.onNodeWithTag("tab_subscriptions").performClick()
    composeTestRule.waitForIdle()

    // Try navigating to add tab
    composeTestRule.onNodeWithTag("fab_add").performClick()
    composeTestRule.waitForIdle()

    // Try navigating to analytics tab
    composeTestRule.onNodeWithTag("tab_analytics").performClick()
    composeTestRule.waitForIdle()

    // Go back to transactions
    composeTestRule.onNodeWithTag("tab_transactions").performClick()
    composeTestRule.waitForIdle()

    db.close()
  }

  @Test
  fun greeting_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repository = TransactionRepository(db.transactionDao(), db.subscriptionDao(), db.customCategoryDao(), db.budgetDao())
    val viewModel = LedgerViewModel(repository)
    viewModel.loadSampleData()

    composeTestRule.setContent { 
      MyApplicationTheme { 
        LedgerApp(viewModel = viewModel) 
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    
    db.close()
  }
}

