package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.BrowserScreen
import com.example.ui.viewmodel.BrowserViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrowserScreenSearchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testClickSearchBarInBrowserScreen() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = BrowserViewModel(app)

        composeTestRule.setContent {
            BrowserScreen(
                viewModel = viewModel,
                onOpenTabs = {},
                onOpenBookmarks = {},
                onOpenHistory = {},
                onOpenDownloads = {},
                onOpenSpeedTest = {},
                onOpenSettings = {}
            )
        }

        // Click search bar
        composeTestRule.onNodeWithTag("address_text_field").performClick()
        composeTestRule.waitForIdle()

        // Type something
        composeTestRule.onNodeWithTag("address_text_field").performTextInput("android")
        composeTestRule.waitForIdle()
    }
}
