package com.miskibin.poznajswiat

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * End-to-end smoke test: boots the real activity with the real assets and
 * drives the main flows, saving screenshots to build/screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class AppSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitText(text: String, timeout: Long = 15_000) {
        rule.waitUntil(timeout) {
            rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        val dir = File("build/screenshots").apply { mkdirs() }
        rule.activityRule.scenario.onActivity { activity ->
            val view = activity.window.decorView
            if (view.width <= 0 || view.height <= 0) return@onActivity
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(dir, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun mainFlows() {
        // Home
        awaitText("Poznaj Świat")
        awaitText("Stolice")
        screenshot("01_home")

        // Flag quiz: answer the first question and advance to the second.
        rule.onNodeWithText("Zgadnij, do którego kraju należy flaga").performClick()
        awaitText("Pytanie 1 z")
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("quiz_option").fetchSemanticsNodes().size == 4
        }
        screenshot("02_flag_quiz")
        rule.onAllNodesWithTag("quiz_option")[0].performClick()
        awaitText("Dalej")
        screenshot("03_flag_quiz_answered")
        rule.onNodeWithText("Dalej").performClick()
        awaitText("Pytanie 2 z")

        // Exit quiz back to home
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Stolice")

        // Map quiz
        rule.onNodeWithText("Wskaż kraj na mapie świata").performClick()
        awaitText("Wskaż na mapie")
        screenshot("04_map_quiz")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Stolice")

        // Learn + search + detail
        rule.onNodeWithText("Poznaj wszystkie kraje, flagi i ciekawostki").performClick()
        awaitText("Szukaj kraju lub stolicy")
        screenshot("05_learn")
        rule.onNodeWithText("Szukaj kraju lub stolicy…").performTextInput("Polska")
        awaitText("Warszawa")
        screenshot("06_learn_search")
        rule.onNodeWithText("Warszawa").performClick()
        awaitText("Twoje postępy")
        awaitText("Stolica")
        screenshot("07_detail")
    }
}
