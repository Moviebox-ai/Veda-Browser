package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.domain.model.Tab
import com.example.ui.components.AddressBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddressBarClickTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testClickAddressBarWhenHomeTab() {
        val homeTab = Tab(
            id = "tab_1",
            url = "velocity:home",
            title = "Home"
        )

        composeTestRule.setContent {
            AddressBar(
                currentTab = homeTab,
                tabCount = 1,
                isLoading = false,
                progress = 0,
                onNavigate = {},
                onReload = {},
                onStop = {},
                onOpenTabs = {},
                onOpenMenu = {},
                onOpenSiteInfo = {}
            )
        }

        composeTestRule.onNodeWithTag("address_text_field").performClick()
    }

    @Test
    fun testClickAddressBarWhenWebTab() {
        val webTab = Tab(
            id = "tab_2",
            url = "https://example.com/test",
            title = "Example"
        )

        composeTestRule.setContent {
            AddressBar(
                currentTab = webTab,
                tabCount = 1,
                isLoading = false,
                progress = 0,
                onNavigate = {},
                onReload = {},
                onStop = {},
                onOpenTabs = {},
                onOpenMenu = {},
                onOpenSiteInfo = {}
            )
        }

        composeTestRule.onNodeWithTag("address_text_field").performClick()
    }
}
