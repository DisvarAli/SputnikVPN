package com.my.vpn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainScreen_showsConnectButtonOrOnboarding() {
        composeRule.waitForIdle()
        val context = composeRule.activity
        val connectLabel = context.getString(R.string.connect_button)
        val onboardingNext = context.getString(R.string.onboarding_next)
        val connectVisible = runCatching {
            composeRule.onNodeWithText(connectLabel).assertIsDisplayed()
        }.isSuccess
        val onboardingVisible = runCatching {
            composeRule.onNodeWithText(onboardingNext).assertIsDisplayed()
        }.isSuccess
        assert(connectVisible || onboardingVisible)
    }
}
