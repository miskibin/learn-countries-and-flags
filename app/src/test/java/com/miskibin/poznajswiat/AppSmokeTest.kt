package com.miskibin.poznajswiat

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        awaitText("Powtórka dnia")
        awaitText("Ścieżka nauki")
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

        // Map quiz: use the "I don't know" reveal
        rule.onNodeWithText("Wskaż kraj na mapie świata").performClick()
        awaitText("Wskaż na mapie")
        screenshot("04_map_quiz")
        rule.onNodeWithText("Nie wiem — pokaż").performClick()
        awaitText("Dalej")
        screenshot("04b_map_reveal")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Stolice")

        // Knowledge map
        rule.onNodeWithText("Zobacz na mapie, ile już umiesz").performScrollTo().performClick()
        awaitText("W trakcie")
        screenshot("12_knowledge")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Stolice")

        // History quiz: the question form is random (year / order / country),
        // so assert on the shared structure only.
        rule.onNodeWithText("Dopasuj daty do wydarzeń z dziejów świata")
            .performScrollTo().performClick()
        awaitText("Pytanie 1 z")
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("quiz_option").fetchSemanticsNodes().size >= 2
        }
        screenshot("08_history_quiz")
        rule.onAllNodesWithTag("quiz_option")[0].performClick()
        awaitText("Dalej")
        awaitText("Nowożytność") // timeline bar era labels
        screenshot("09_history_quiz_answered")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Stolice")

        // Review session (mixed; seeded by the answers given above)
        rule.onNodeWithText("Start").performScrollTo().performClick()
        awaitText("Pytanie 1 z")
        screenshot("10_review")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Stolice")

        // Timeline
        rule.onNodeWithText("Najważniejsze wydarzenia w historii świata")
            .performScrollTo().performClick()
        awaitText("Starożytność")
        screenshot("11_timeline")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Stolice")

        // Learn + search + detail (with neighbors and history sections)
        rule.onNodeWithText("Poznaj wszystkie kraje, flagi i ciekawostki")
            .performScrollTo().performClick()
        awaitText("Szukaj kraju lub stolicy")
        screenshot("05_learn")
        rule.onNodeWithText("Szukaj kraju lub stolicy…").performTextInput("Polska")
        awaitText("Warszawa")
        screenshot("06_learn_search")
        rule.onNodeWithText("Warszawa").performClick()
        awaitText("Twoje postępy")
        awaitText("Sąsiedzi")
        awaitText("Wydarzenia historyczne")
        screenshot("07_detail")
    }
}
